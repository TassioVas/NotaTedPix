package br.com.crowe.notasTedPix;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sankhya.util.JdbcUtils;
import com.sankhya.util.SessionFile;
import com.sankhya.util.StringUtils;
import com.sankhya.util.UIDGenerator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.modelcore.comercial.ImpressaoNotaHelpper;
import br.com.sankhya.modelcore.comercial.util.JasperPrintWrapper;
import br.com.sankhya.modelcore.util.ArquivoModeloUtils;
import br.com.sankhya.modelcore.util.DatasetUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import br.com.sankhya.ws.ServiceContext;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

public class GerarPdfDespesas implements AcaoRotinaJava {

	String msg;
	// doAction(contexto);
	private static final String LINK_BAIXAR = "<a title=\"Baixar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo={0}\" target=\"_blank\"><u><b>{1}</b></u></a>";

	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		// TODO Auto-generated method stub

		System.out.println("Entrou para gerar a nota");

		if ((contexto.getLinhas()).length == 0) {
			contexto.mostraErro("Selecione um registro antes.");
		} else if ((contexto.getLinhas()).length > 1) {
			contexto.mostraErro("Selecione apenas um registro.");
		}
		Registro linha = contexto.getLinhas()[0];
		BigDecimal nuNota = (BigDecimal) linha.getCampo("NUNOTA");
		byte[] bytes = gerarPDF(nuNota);
		SessionFile fileReport = SessionFile.createSessionFile("Nota_" + nuNota, "application/pdf", bytes);
		String chaveSessaoArquivo = UIDGenerator.getNextID();
		ServiceContext.getCurrent().putHttpSessionAttribute(chaveSessaoArquivo, (Serializable) fileReport);
		contexto.setMensagemRetorno(String.format("Impressgeradas.\nClique %s paravisualizar.",new Object[] { getLinkBaixar("aqui", chaveSessaoArquivo) }));
		System.out.println("Gerou a nota e o pdf : " + bytes);

	}

	public byte[] gerarPDF(BigDecimal nuNota) throws Exception {
		JdbcWrapper jdbc = null;
		ByteArrayOutputStream bytesPdf = null;

		System.out.println("Passou aqui para gerar o pdf");
		System.out.println("nunota : " + nuNota);
		try {
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbc = dwfEntityFacade.getJdbcWrapper();
			jdbc.openSession();
			DynamicVO notaVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNota);
			ConcatenatePDF concatenate = new ConcatenatePDF();
			if ("L".equals(notaVO.asString("STATUSNOTA"))) {
				// Collection<BigDecimal> financeiros =
				// buildColecaoFinanceirosPeloNunota(nuNota, jdbc);
				/*
				 * if (financeiros.size() > 0) { BoletoHelper.ConfiguracaoBoleto
				 * configuracaoBoleto = new BoletoHelper.ConfiguracaoBoleto();
				 * configuracaoBoleto.setFinanceirosSelecionados(financeiros);
				 * configuracaoBoleto.setAgrupamentoBoleto(4);
				 * configuracaoBoleto.setTipoReimpressao("T");
				 * configuracaoBoleto.setReimprimirBoleta(true);
				 * configuracaoBoleto.setNroBoletoIni("0");
				 * configuracaoBoleto.setNroBoletoFim("0");
				 * configuracaoBoleto.setVisualizaPDFBoleto(true); BoletoHelper boletoHelper =
				 * new BoletoHelper(); configuracaoBoleto.setTipoSaidaBoleto(1);
				 * boletoHelper.gerarBoleto(configuracaoBoleto); byte[] boletosPDF =
				 * boletoHelper.getBoletosPDF(); concatenate.addPdfFile(boletosPDF); }
				 */
				ImpressaoNotaHelpper impressaoNotaHelpper = new ImpressaoNotaHelpper();
				System.out.println("GerarPDFDESPESAS linha 98 nunota" + nuNota );
				impressaoNotaHelpper.inicializaNota(nuNota);
				Collection<DynamicVO> impressoesList = impressaoNotaHelpper.gerarJasperPrintAnexo();
				if (impressoesList != null && !impressoesList.isEmpty()) {
					DynamicVO impressaoVO = impressoesList.iterator().next();
					byte[] bytes = (byte[]) impressaoVO.getProperty("CONTEUDO");
					if (bytes != null) {
						ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
						ObjectInputStream ois = new ObjectInputStream(bais);
						Object content = ois.readObject();
						if (content instanceof JasperPrintWrapper) {
							JasperPrint jasperPrint = (JasperPrint) content;
							byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
							concatenate.addPdfFile(pdf);
						}
						ois.close();
						bais.close();
					}
				}
				NativeSql sqlVar = new NativeSql(jdbc);
				sqlVar.appendSql("SELECT DISTINCT NUNOTA FROM TGFCAB WHERE NUNOTA = ?");
				sqlVar.addParameter(nuNota);
				ResultSet rsVar = sqlVar.executeQuery();
				while (rsVar.next()) {
					int nroRelatorio = MGECoreParameter.getParameterAsInt("NURELDESPEMAIL");
					if (nroRelatorio > 0) {
					DynamicVO pedidoVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota",
							rsVar.getBigDecimal("NUNOTA"));
					Map<String, Object> paramsRelatorio = new HashMap<String, Object>();
					paramsRelatorio.put("PK_NUNOTA", pedidoVO.asBigDecimal("NUMNOTA"));
					paramsRelatorio.put("PDIR_MODELO", ArquivoModeloUtils.getDiretorioModelos());
					Report r = ReportManager.getInstance().getReport(BigDecimal.valueOf(4), dwfEntityFacade);
					JasperPrint jasperPrint = r.buildJasperPrint(paramsRelatorio, jdbc.getConnection());
					if (jasperPrint != null && jasperPrint.getPages().size() > 0) {
						byte[] pdf = JasperExportManager.exportReportToPdf(jasperPrint);
						concatenate.addPdfFile(pdf);
					}
					}
				/*	Collection<DynamicVO> itens = dwfEntityFacade.findByDynamicFinderAsVO(new FinderWrapper("TGFITE",
							"this.NUNOTA = ?", new Object[] { pedidoVO.asBigDecimal("NUNOTA") }));
					for (DynamicVO itemVO : itens) {
						if (itemVO.asBlob("ANEXO") != null)
							getBytesFile(jdbc, concatenate, itemVO, "ANEXO");
						if (itemVO.asBlob("ANEXO1") != null)
							getBytesFile(jdbc, concatenate, itemVO, "ANEXO1");
						if (itemVO.asBlob("ANEXO2") != null)
							getBytesFile(jdbc, concatenate, itemVO, "ANEXO2");
						if (itemVO.asBlob("ANEXO3") != null)
							getBytesFile(jdbc, concatenate, itemVO, "ANEXO3");
					}*/
				}
			}

			bytesPdf = concatenate.run();
			return bytesPdf.toByteArray();

			// throw new Exception("Apenas notas confirmadas podem gerar o relat");
		} finally {
			// if (bytesPdf != null)
			// bytesPdf.close();
			JdbcWrapper.closeSession(jdbc);
		}
	}
	
	

	private void getBytesFile(JdbcWrapper jdbc, ConcatenatePDF concatenate, DynamicVO itemVO, String fieldName)
			throws Exception {
		NativeSql query = new NativeSql(jdbc);
		query.appendSql("SELECT ").appendSql(fieldName);
		query.appendSql(" FROM TGFITE WHERE NUNOTA = ?");
		query.addParameter(itemVO.asBigDecimal("NUNOTA"));
		ResultSet rs = query.executeQuery();
		if (rs.next()) {
			DatasetUtils.InformacoesAnexo infoAnexo = DatasetUtils.buildInformacoesAnexo(rs, fieldName);
			String name = infoAnexo.name;
			if (name != null && name.toUpperCase().endsWith(".PDF")) {
				InputStream in = rs.getBinaryStream(fieldName);
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				StringBuffer buf = new StringBuffer();
				byte[] b = new byte[2048];
				boolean hasFileInfo = false;
				boolean writeDirectly = false;
				int offset = 0;
				int length;
				while ((length = in.read(b)) > 0) {
					if (writeDirectly) {
						baos.write(b, 0, length);
						continue;
					}
					offset = buf.length();
					buf.append(new String(b));
					if (!hasFileInfo && "__start_fileinformation__".equals(buf.substring(0, 25)))
						hasFileInfo = true;
					if (hasFileInfo) {
						int i = buf.indexOf("__end_fileinformation__");
						if (i > -1) {
							i += 23;
							i -= offset;
							baos.write(b, i, length - i);
							writeDirectly = true;
						}
						continue;
					}
					baos.write(b, 0, length);
					writeDirectly = true;
				}
				baos.flush();
				in.close();
				byte[] file = baos.toByteArray();
				concatenate.addPdfFile(file);
			}
		}
	}
	
	private String getLinkBaixar(String descricao, String chave) {
		String url = "<a title=\"Baixar Arquivo\" href=\"/mge/visualizadorArquivos.mge?chaveArquivo={0}\" target=\"_blank\"><u><b>{1}</b></u></a>"
				.replace("{0}", chave);
		url = url.replace("{1}", descricao);
		return url;
	}

}
