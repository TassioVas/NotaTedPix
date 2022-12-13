package br.com.crowe.notasTedPix;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sankhya.util.SessionFile;
import com.sankhya.util.StringUtils;
import com.sankhya.util.UIDGenerator;

import br.com.sankhya.commons.xml.Element;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.EntityPrimaryKey;
import br.com.sankhya.jape.dao.EntityPropertyDescriptor;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.comercial.BarramentoRegra;
import br.com.sankhya.modelcore.comercial.BoletoHelper;
import br.com.sankhya.modelcore.comercial.ImpressaoNotaHelpper;
import br.com.sankhya.modelcore.comercial.centrais.CACHelper;
import br.com.sankhya.modelcore.comercial.util.JasperPrintWrapper;
import br.com.sankhya.modelcore.util.ArquivoModeloUtils;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.modelcore.util.MGECoreParameter;
import br.com.sankhya.modelcore.util.Report;
import br.com.sankhya.modelcore.util.ReportManager;
import br.com.sankhya.ws.ServiceContext;
import br.com.sankhyagwt.user.util.XMLUtils;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperPrint;

public class NotasTedPix implements AcaoRotinaJava {

	private static final SimpleDateFormat ddMMyyy = new SimpleDateFormat("dd/MM/yyyy");
	private CACHelper cacHelper = null;

	String msg;

	Collection dynamicVOs;
	EntityFacade dwfFacade;

	Gson GSON = (new GsonBuilder()).serializeNulls().create();
	private EntityFacade dwfEntityFacade;
	private String id;
	private ServiceContext sctx;

	BigDecimal nuNotaOld;
	BigDecimal nuAnexo;

	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		// TODO Auto-generated method stub

		System.out.println("Codigo inicia aqui");

		incluirNotaCabIte(contexto);

	}

	private void incluirNotaCabIte(ContextoAcao contexto) throws Exception {

		Timestamp dataHoraAtual = new Timestamp(System.currentTimeMillis());
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbc);
		SessionHandle hnd = JapeSession.open();
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		CACHelper cacHelper = new CACHelper();
		try {

			System.out.println("entrou no metodo");

			if ((contexto.getLinhas()).length == 0) {
				contexto.mostraErro("Selecione um registro antes.");
			} else if ((contexto.getLinhas()).length > 1) {
				contexto.mostraErro("Selecione apenas um registro.");
			}

			contexto.confirmar("Enviar Email",
					"Esta opvai enviar a nota de de suas despesas para contatos configurados do parceiro.\n\nDeseja continuar?",
					1);
			Registro linha = contexto.getLinhas()[0];
			BigDecimal nuNota = (BigDecimal) linha.getCampo("NUNOTA");
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			DynamicVO notaVO = (DynamicVO) dwfEntityFacade.findEntityByPrimaryKeyAsVO("CabecalhoNota", nuNota);
			// Collection<DynamicVO> contatos = dwfEntityFacade.findByDynamicFinderAsVO(
			// new FinderWrapper("Contato", "this.CODPARC = ? AND this.AD_RECEBENOTADEB =
			// 'S'",
			// new Object[] { notaVO.asBigDecimal("CODPARC") }));
			// ArrayList<String> listaEmails = new ArrayList<String>();
			// for (DynamicVO contatoVO : contatos) {
			// if (StringUtils.isNotEmpty(contatoVO.asString("EMAIL")))
			// listaEmails.add(contatoVO.asString("EMAIL"));
			// }
			// if (listaEmails.size() == 0)
			// contexto.mostraErro("Nenhum contato do parceiro configurado para receber
			// relatnota de dVerifique!");
			System.out.println("Antes de entrar no gerar despesas");
			GerarPdfDespesas gerar = new GerarPdfDespesas();
			byte[] bytesPdf = gerar.gerarPDF(nuNota);
			boolean update = nativeSql
					.executeUpdate("UPDATE TGFCAB SET AD_NOTANEXOTEDPIX = '" + bytesPdf + " 'WHERE NUNOTA = " + nuNota);
			System.out.println(update);
			System.out.println("Depois de gerar o pdf");
			System.out.println("PDF bytes " + bytesPdf);
			String bytString = new String(bytesPdf, "UTF-8");
			System.out.println("Notas ted pix linha 127 bytString" + bytString);
			
			if (bytesPdf != null)

				enviarEmail(dwfEntityFacade, contexto, bytesPdf, nuNota);
			contexto.setMensagemRetorno("Email enviado com sucesso.");

		} finally {
			JapeSession.close(hnd);

		}
	}

	private void enviarEmail(EntityFacade dwfEntityFacade, ContextoAcao contexto, byte[] bytesPdf, BigDecimal nuNota)
			throws Exception {
		BigDecimal codFila = null;
		JdbcWrapper jdbc = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(jdbc);
		byte[] notapix;

		String query = ("SELECT AD_NOTANEXOTEDPIX FROM TGFCAB WHERE NUNOTA = " + nuNota);
		ResultSet rs = nativeSql.executeQuery(query);
		while (rs.next()) {
			notapix = rs.getBytes("AD_NOTANEXOTEDPIX");
			System.out.println("notapix + " + notapix);
			try {
				
				System.out.println("PAssou pelo metodo de enviar email : ");
				System.out.println("bytesPdf " + bytesPdf);
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				DynamicVO dynamicVO1 = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
				System.out.println("notapix + " + notapix);
				dynamicVO1.setProperty("ANEXO", bytesPdf);
				dynamicVO1.setProperty("NOMEARQUIVO", "teste.pdf");
				// dynamicVO1.setProperty("NOMEARQUIVO", abrev.trim().replaceAll("\\s+", " ") +
				// " PC " + rdv + " "
				// + nomelUsuario.trim().replaceAll("\\s+", " ") +
				// ".pdf".trim().replaceAll("\\s+", " "));
				dynamicVO1.setProperty("TIPO", "application/pdf");
				PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoMensagem", (EntityVO) dynamicVO1);
				DynamicVO save = (DynamicVO) createEntity.getValueObject();
				// BigDecimal nuAnexo = save.asBigDecimal("NUANEXO");

				nuAnexo = (BigDecimal) save.getProperty("NUANEXO");

				System.out.println("NUANEXO " + nuAnexo);

				System.out.println("inserido na tela de anexo");

			} catch (Exception e) {
				e.printStackTrace();
				msg = "Erro na inclusao do anexo " + e.getMessage();
				System.out.println(msg);
			}

			String assuntoEmail = "Teste";

			char[] assuntoEmailchar = assuntoEmail.toCharArray();

			try {
				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				DynamicVO dynamicVO1 = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");

				dynamicVO1.setProperty("ASSUNTO", "teste");
				System.out.println("inserão do email");
				dynamicVO1.setProperty("CODMSG", null);
				dynamicVO1.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
				dynamicVO1.setProperty("STATUS", "Pendente");
				dynamicVO1.setProperty("CODCON", new BigDecimal(0));
				dynamicVO1.setProperty("TENTENVIO", new BigDecimal(0));
				dynamicVO1.setProperty("MENSAGEM", assuntoEmailchar);
				// System.out.println(assuntoEmailchar);
				dynamicVO1.setProperty("TIPOENVIO", "E");
				dynamicVO1.setProperty("MAXTENTENVIO", new BigDecimal(3));
				// dynamicVO1.setProperty("EMAIL", emailUsuario);
				dynamicVO1.setProperty("EMAIL", "tassio.vasconcelos@covenantit.com.br");
				dynamicVO1.setProperty("CODSMTP", new BigDecimal(10));
				dynamicVO1.setProperty("CODUSUREMET", contexto.getUsuarioLogado());
				// System.out.println("NUANEXO " + nuAnexo);
				dynamicVO1.setProperty("NUANEXO", nuAnexo);
				System.out.println("NUANEXO " + nuAnexo);
				dynamicVO1.setProperty("MIMETYPE", "text/html");
				PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDFilaMensagem", (EntityVO) dynamicVO1);
				DynamicVO save = (DynamicVO) createEntity.getValueObject();
				codFila = save.asBigDecimal("CODFILA");

			} catch (Exception e) {
				e.printStackTrace();
				msg = "Erro na inclusao do item " + e.getMessage();
				System.out.println(msg);
			}

			try {

				System.out.println("CODFILA na inserção do item" + codFila);
				System.out.println("alteração do cod");

				EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
				DynamicVO dynamicVO1 = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("MSDDestFilaMensagem");

				dynamicVO1.setProperty("CODFILA", codFila);
				// dynamicVO1.setProperty("EMAIL", "financeirosp@crowe.com.br");
				// dynamicVO1.setProperty("EMAIL", "jefferson.costa@covenantit.com.br");
				dynamicVO1.setProperty("EMAIL", "t.santos.vasconcelos@gmail.com");
				dynamicVO1.setProperty("SEQUENCIA", new BigDecimal(1));
				PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDDestFilaMensagem",
						(EntityVO) dynamicVO1);
				DynamicVO save = (DynamicVO) createEntity.getValueObject();

				System.out.println("CODFILA " + codFila);

			} catch (Exception e) {
				e.printStackTrace();
				msg = "Erro na inclusao do item " + e.getMessage();
				System.out.println(msg);
			}
		}
	}
}
