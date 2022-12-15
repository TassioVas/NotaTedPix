package br.com.crowe.notas;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.swing.text.MaskFormatter;

import com.sankhya.util.SessionFile;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper;
import br.com.sankhya.modelcore.util.AgendamentoRelatorioHelper.ParametroRelatorio;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class NotasTedPix implements AcaoRotinaJava {

	private static final SimpleDateFormat ddMMyyy = new SimpleDateFormat("dd/MM/yyyy");
	Timestamp dtVenc;
	BigDecimal codFila;
	BigDecimal codMsg;
	BigDecimal dtEntrada;
	BigDecimal status;
	BigDecimal tenteEnvio;
	BigDecimal codProj;
	BigDecimal rdv;
	BigDecimal nuAnexo;
	BigDecimal nuAnexo1;
	BigDecimal nuNotaOrig;
	BigDecimal numNfse;
	BigDecimal numNota;
	BigDecimal codParc;
	BigDecimal vlrTot;
	BigDecimal codEmp;
	BigDecimal nuRfe;
	BigDecimal codCtaBco;

	String nomeBco;
	BigDecimal codCtaBcoInt2;
	BigDecimal agencia;
	BigDecimal bco;
	BigDecimal sequencia;
	BigDecimal seq;

	String cnpj;
	String assunto;
	String mensagem;
	String msg;
	String emailParc;
	String assuntoEmail;
	String query;
	String query2;
	String queryNuanexo;
	String coduso;
	String emailFornecedor = "tassio.vasconcelos@covenantit.com.br";
	String nomeParc;
	String nomeFantasia;
	String ambiente;
	String marca;
	String site;
	String email;
	String emailCtt;

	FinderWrapper finde;

	private Timestamp dtInicio;
	// private Timestamp dtVenc;

	byte[] anexo;
	byte[] pdfBytes;

	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		// TODO Auto-generated method stub

		Scanner sc = new Scanner(System.in);
		Locale localeBR = new Locale("pt","BR");

		JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(JDBC);
		SessionHandle hnd = JapeSession.open();

		Timestamp dtalter = new Timestamp(System.currentTimeMillis());

		Locale l = new Locale("pt", "BR");

		NumberFormat formatar = NumberFormat.getInstance(l);

		System.out.println("INICIO DO CODIGO");

		for (int i = 0; i < (contexto.getLinhas()).length; i++) {
			if ((contexto.getLinhas()).length == 0) {
				contexto.mostraErro("Selecione um registro antes.");
				System.out.println("Entrou no if ");
			} else if ((contexto.getLinhas()).length > 1) {
				contexto.mostraErro("Selecione apenas um registro.");
			}
			Registro linha = contexto.getLinhas()[i];

			nuNotaOrig = (BigDecimal) linha.getCampo("NUNOTA");

			ResultSet query = nativeSql.executeQuery(" SELECT " + " CAB.AD_DTVENC," + " CAB.AD_DTVENC2,"
					+ " CAB.DTNEG, " + " CAB.DTALTER, " + " CAB.DTENTSAI, " + " CAB.VLRNOTA," + " CAB.CODPARC,"
					+ " PAR.NOMEPARC, CAB.NUMNOTA, CAB.NUNOTA, CAB.NUMNFSE, ITE.VLRTOT, PAR.EMAIL, CAB.CODEMP,"
					+ " CAST((SELECT CAB.VLRNOTA - CAB.VLRINSS - CASE WHEN CAB.ISSRETIDO = 'S' THEN CAB.VLRISS ELSE 0 END - CAB.VLRIRF-ISNULL((SUM(IMN.VALOR)),0) FROM TGFIMN IMN WHERE IMN.NUNOTA = CAB.NUNOTA)AS NUMERIC(20,2)) AS TOTALIQ "
					+ " FROM TGFCAB CAB	JOIN TGFITE ITE ON ITE.NUNOTA = CAB.NUNOTA"
					+ " JOIN TGFPAR PAR ON PAR.CODPARC = CAB.CODPARC" + " WHERE CAB.NUNOTA = " + nuNotaOrig);
			while (query.next()) {

				nomeParc = query.getString("NOMEPARC");
				emailParc = query.getString("EMAIL");
				codParc = query.getBigDecimal("CODPARC");
				numNfse = query.getBigDecimal("NUMNFSE");
				vlrTot = query.getBigDecimal("TOTALIQ");
				dtVenc = query.getTimestamp("AD_DTVENC2");
				numNota = query.getBigDecimal("NUMNOTA");
				codEmp = query.getBigDecimal("CODEMP");
				System.out.println("codemp" + codEmp);
				System.out.println("codparc" + codParc);

				// if (emailParc == null) {
				// contexto.mostraErro("Parceiro com email nao Cadastrado \n Favor Cadastrar
				// Email do parceiro : "
				// + nomeParc + "\n Codigo Parceiro : " + codParc);
				// }

				// char resp = sc.next().charAt(0);

				ResultSet query2 = nativeSql.executeQuery(
						"SELECT EMP.CODEMP, BCO.CODBCO, BCO.NOMEBCO, EMP.AD_CODCTABCOINT2, EMP.NOMEFANTASIA,\r\n"
								+ "EMP.AD_CODBCO,  CTA.CODCTABCO, CTA.CODAGE, CTA.CODCTABCO, EMP.CGC\r\n"
								+ "FROM TSIEMP EMP\r\n" + "RIGHT JOIN TSIBCO BCO ON BCO.CODBCO = EMP.AD_CODBCO\r\n"
								+ "INNER JOIN TSICTA CTA ON ( CTA.CODCTABCOINT = AD_CODCTABCOINT2)\r\n"
								+ "WHERE EMP.CODEMP = " + codEmp);
				while (query2.next()) {
					nomeBco = query2.getString("NOMEBCO");
					codCtaBcoInt2 = query2.getBigDecimal("AD_CODCTABCOINT2");
					agencia = query2.getBigDecimal("CODAGE");
					bco = query2.getBigDecimal("CODBCO");
					cnpj = query2.getString("CGC");
					nomeFantasia = query2.getString("NOMEFANTASIA");
					codCtaBco = query2.getBigDecimal("CODCTABCO");
				}

				MaskFormatter mask = new MaskFormatter("##.###.###/####-##");
				mask.setValueContainsLiteralCharacters(false);
				System.out.println("CNPJ : " + mask.valueToString(cnpj));

				ResultSet query3 = nativeSql.executeQuery("SELECT CON.AMBIENTE FROM TGFCAB CAB "
						+ "INNER JOIN TCSCON CON ON (CON.NUMCONTRATO = CAB.NUMCONTRATO) " + "WHERE CAB.NUNOTA = "
						+ nuNotaOrig);
				while (query3.next()) {
					ambiente = query3.getString("AMBIENTE");
				}

				/*
				 * assuntoEmail = "<!DOCTYPE html>" + "<html>" + "<head>" +
				 * "    <meta charset=\"utf-8\"/>" + "    <title>Teste Email</title>" +
				 * "</head>" + "    <body>" + "        Prezado cliente, " + nomeParc +
				 * "        <br></br>" + "        Segue NF "+ numNfse+ " (RPS "+numNota
				 * +"), referente ao contrato " +ambiente+ ", com vencimento em " +
				 * ddMMyyy.format(dtVenc) +"." + "        <br></br>" +
				 * "        O boleto no valor a pagar de R$ "
				 * +vlrTot+" também encontra-se anexo. " + "        <br></br>" +
				 * "        Quaisquer inconsistências devem ser apontadas no prazo máximo de 05 dias úteis."
				 * + "        <br></br>" + "        Atenciosamente," + "        <br><br>" +
				 * "        <b>Equipe de Faturamento - Grupo Crowe Macro</b><br>" +
				 * "        <b>Office (+55 11) 5632-3733</b><br>" +
				 * "        <b>Celular/Whats app (+55 11) 98714-1772</b><br>" +
				 * "        <br><br>" + "        cobranca@crowe.com.br" + site + "    </body>" +
				 * "</html>";
				 */

				if (codEmp.equals(new BigDecimal(2)) || codEmp.equals(new BigDecimal(3))
						|| codEmp.equals(new BigDecimal(5)) || codEmp.equals(new BigDecimal(7))
						|| codEmp.equals(new BigDecimal(8)) || codEmp.equals(new BigDecimal(9))
						|| codEmp.equals(new BigDecimal(12)) || codEmp.equals(new BigDecimal(13))
						|| codEmp.equals(new BigDecimal(14)) || codEmp.equals(new BigDecimal(15))
						|| codEmp.equals(new BigDecimal(16)) || codEmp.equals(new BigDecimal(19))
						|| codEmp.equals(new BigDecimal(25)) || codEmp.equals(new BigDecimal(30))
						|| codEmp.equals(new BigDecimal(31))) {
					nuRfe = new BigDecimal(4);
					System.out.println("primeiro if");
				} else if (codEmp.equals(new BigDecimal(17)) || codEmp.equals(new BigDecimal(18))
						|| codEmp.equals(new BigDecimal(29))) {
					nuRfe = new BigDecimal(5);
					System.out.println("segundo if");
				} else if (codEmp.equals(new BigDecimal(1)) || codEmp.equals(new BigDecimal(6))
						|| codEmp.equals(new BigDecimal(10)) || codEmp.equals(new BigDecimal(11))
						|| codEmp.equals(new BigDecimal(20)) || codEmp.equals(new BigDecimal(23))
						|| codEmp.equals(new BigDecimal(24))) {
					nuRfe = new BigDecimal(6);
					System.out.println("terceiro if");
				} else if (codEmp.equals(new BigDecimal(26))) {
					nuRfe = new BigDecimal(7);
					System.out.println("quarto if");
				} else if (codEmp.equals(new BigDecimal(4)) || codEmp.equals(new BigDecimal(21))
						|| codEmp.equals(new BigDecimal(28))) {
					nuRfe = new BigDecimal(8);
					System.out.println("quinto if");
				} else if (codEmp.equals(new BigDecimal(22)) || codEmp.equals(new BigDecimal(27))) {
					nuRfe = new BigDecimal(9);
					System.out.println("sexto if");
				} else {
					contexto.setMensagemRetorno("CODEMP FORA DA FAIXA DE RELATORIOS \nPARA IMPRESSÃO DA NOTA");
					System.out.println("passou aqui no else");
				}

				// nuRfe = new BigDecimal(6);

				System.out.println("nurfe : " + nuRfe);

				List<Object> lstParam = new ArrayList<Object>();
				byte[] pdfBytes = null;
				String chave = "chave.pdf";
				Registro[] regs = contexto.getLinhas();

				try {
					System.out.println("entrou no try do rel");

					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();

					ParametroRelatorio pk = new ParametroRelatorio("PK_NUNOTA", BigDecimal.class.getName(),
							regs[0].getCampo("NUNOTA"));
					lstParam.add(pk);
					System.out.println(pk);

					pdfBytes = AgendamentoRelatorioHelper.getPrintableReport(nuRfe, lstParam,
							contexto.getUsuarioLogado(), dwfFacade);
					SessionFile sessionFile = SessionFile.createSessionFile("Nota_Fiscal.pdf", "Nota_Fiscal", pdfBytes);
					ServiceContext.getCurrent().putHttpSessionAttribute(chave, sessionFile);
					System.out.println(pdfBytes);

					System.out.println("GErando anexo pdfbyte " + pdfBytes);

				} catch (Exception e) {
					e.printStackTrace();
				}

				ResultSet queryMarca = nativeSql
						.executeQuery("SELECT RAZAOSOCIAL ,codemp, AD_MARCA FROM TSIEMP WHERE CODEMP = " + codEmp);
				while (queryMarca.next()) {
					marca = queryMarca.getString("AD_MARCA");
					System.out.println("Marca : " + marca);
				}

				if (marca.equals("Crowe")) {
					site = "https://crowemacro.com.br/";
				} else if (marca.equals("Croma")) {
					site = " https://cromaconsultoria.com.br/";
				} else if (marca.equals("Macro")) {
					site = "https://macroconsultoria.com.br/";
				} else if (marca.equals("Covenant")) {
					site = "https://covenantit.com.br/";
				}
				
				NumberFormat dinheiro = NumberFormat.getCurrencyInstance(localeBR);

				System.out.println("site : " + site);

				assuntoEmail = "Prezado cliente, " + nomeParc + ".\r\n" + "\r\n" + "Segue NFs " + numNfse + " (RPS "
						+ numNota + "), referente ao contrato " + ambiente + ", com vencimento em "
						+ ddMMyyy.format(dtVenc) 
						+ "\r\n" + "\r\n" 
						+ "O Deposito no valor de " + dinheiro.format(vlrTot)
						+ "" 
						+ " deve ser realizado na conta abaixo:\r\n" 
						+ "\r\n" 
						+ " "
						+ nomeBco + "-" + bco + "\r\n" + "\r\n" + "Agencia: " + agencia + "\r\n" + "\r\n" + "Conta: "
						+ codCtaBco + "\r\n" + "\r\n" + nomeFantasia + "\r\n" + "\r\n" + "<b>PIX/CNPJ: "
						+ mask.valueToString(cnpj) + "</b>\r\n" + "\r\n" + "\r\n" + "\r\n" + "Atenciosamente,\r\n"
						+ "<b>Equipe de Faturamento  Grupo Crowe Macro</b><br>"
						+ "        <b>Office (+55 11) 5632-3733</b><br>"
						+ "        <b>Celular/Whats app (+55 11) 98714-1772</b><br>" + "        <br><br>"
						+ "        cobranca@crowe.com.br\r\n" + site + "";
				char[] assuntoEmailchar = assuntoEmail.toCharArray();

				String queryEmail = ("SELECT DISTINCT CTT.CODCONTATO AS CONTATO, CTT.ATIVO, CTT.RECEBEBOLETOEMAIL, CTT.CODPARC, PAR.NOMEPARC, CTT.EMAIL AS EMAIL "
						+ " FROM "
						+ " TGFCTT CTT "
						+ " INNER JOIN TGFPAR PAR ON PAR.CODPARC = CTT.CODPARC "
						+ " WHERE "
						+ "  PAR.CODPARC = " +codParc
						+ " AND CTT.CODCONTATO IN (SELECT MIN(CODCONTATO) "
						+ "							FROM TGFCTT CTT2 "
						+ "							WHERE CTT2.CODPARC = PAR.CODPARC  "
						+ "							AND  CTT2.ATIVO = 'S' "
						+ "							AND CTT2.RECEBEBOLETOEMAIL = 'S' "
						+ "							AND CTT2.EMAIL NOT IN ('cobranca@crowe.com.br                                                           '))");

				ResultSet rsEmail = nativeSql.executeQuery(queryEmail);
				System.out.println(rsEmail);
				if (rsEmail.next()) {
					email = rsEmail.getString("EMAIL");
					System.out.println("Dentro do while : " + email);
				} else {
					contexto.setMensagemRetorno("Parceiro "+codParc+" Ctt(1) nao esta ativo ou nao Recebe Email Boleto, Favor verificar.");
					return;
				}

				System.out.println("email" + email);

				try {

					System.out.println("pdfbyte dentro do try" + pdfBytes);
					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					DynamicVO dynamicVO1 = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("AnexoMensagem");
					dynamicVO1.setProperty("ANEXO", pdfBytes);
					dynamicVO1.setProperty("NOMEARQUIVO", "Nota_Fiscal.pdf");
					dynamicVO1.setProperty("TIPO", "application/pdf");
					PersistentLocalEntity createEntity = dwfFacade.createEntity("AnexoMensagem", (EntityVO) dynamicVO1);
					DynamicVO save = (DynamicVO) createEntity.getValueObject();
					nuAnexo = save.asBigDecimal("NUANEXO");

					System.out.println("NUANEXO " + nuAnexo);

					System.out.println("inserido na tela de anexo");

				} catch (Exception e) {
					e.printStackTrace();
					msg = "Erro na inclusao do anexo " + e.getMessage();
					System.out.println(msg);
				}

				try {
					System.out.println("SYSOUT ENTROU NO TRY");

					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					DynamicVO dynamicVO1 = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("MSDFilaMensagem");

					System.out.println(codFila);
					dynamicVO1.setProperty("ASSUNTO",
							"Honorários " + marca.trim().replaceAll("\\s+", " ") + " "
									+ nomeParc.trim().replaceAll("\\s+", " ") + " NFs  " + numNfse + "  " + "("
									+ ambiente.trim().replaceAll("\\s+", " ") + ")");
					dynamicVO1.setProperty("CODMSG", null);
					dynamicVO1.setProperty("DTENTRADA", new Timestamp(System.currentTimeMillis()));
					dynamicVO1.setProperty("STATUS", "Pendente");
					dynamicVO1.setProperty("CODCON", new BigDecimal(0));
					dynamicVO1.setProperty("TENTENVIO", new BigDecimal(0));
					dynamicVO1.setProperty("MENSAGEM", assuntoEmailchar);
					System.out.println(assuntoEmailchar);
					dynamicVO1.setProperty("TIPOENVIO", "E");
					dynamicVO1.setProperty("MAXTENTENVIO", new BigDecimal(3));
					 //dynamicVO1.setProperty("EMAIL", "tassio.vasconcelos@covenantit.com.br");
					// dynamicVO1.setProperty("EMAIL", "cobranca@crowe.com.br");
					dynamicVO1.setProperty("EMAIL", email);
					dynamicVO1.setProperty("CODSMTP", new BigDecimal(2));
					dynamicVO1.setProperty("CODUSUREMET", this.coduso);
					dynamicVO1.setProperty("NUANEXO", nuAnexo);
					PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDFilaMensagem",
							(EntityVO) dynamicVO1);
					DynamicVO save = (DynamicVO) createEntity.getValueObject();
					codFila = save.asBigDecimal("CODFILA");

					System.out.println("CODFILA Dentro da inclusao do email " + codFila);

					contexto.setMensagemRetorno("Email enviado com sucesso");

				} catch (Exception e) {
					e.printStackTrace();
					msg = "Erro na inclusao do item " + e.getMessage();
					System.out.println(msg);
				}

				System.out.println("codparc" + codParc);

				System.out.println("email do contato : " + email);

				String queryEmailParc = (" SELECT EMAIL, CODCONTATO, RECEBEBOLETOEMAIL, ATIVO  FROM TGFCTT WHERE CODPARC ="
						+ codParc + " AND NOMECONTATO <> 'COBRANCA'" + " AND EMAIL <> '" + email + "'"
						+ " AND RECEBEBOLETOEMAIL = 'S'" + " AND ATIVO = 'S'"
						+ " --AND EMAIL <> 'cobranca@crowe.com.br'");
				ResultSet rs = nativeSql.executeQuery(queryEmailParc);

				System.out.println(queryEmailParc);

				while (rs.next()) {

					emailCtt = rs.getString("EMAIL");
					sequencia = rs.getBigDecimal("CODCONTATO");
					System.out.println("emailCtt do contato : " + emailCtt);
					System.out.println("email do contato : " + email);
					System.out.println("Dentro do while" + queryEmailParc);

					try {

						System.out.println("CODFILA na inserção do item" + codFila);
						System.out.println("alteração do cod");

						EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
						DynamicVO dynamicVO1 = (DynamicVO) dwfFacade
								.getDefaultValueObjectInstance("MSDDestFilaMensagem");

						dynamicVO1.setProperty("CODFILA", codFila);
						dynamicVO1.setProperty("EMAIL", emailCtt);
						// dynamicVO1.setProperty("EMAIL", "jefferson.costa@covenantit.com.br");
						// dynamicVO1.setProperty("EMAIL", "t.santos.vasconcelos@gmail.com");
						dynamicVO1.setProperty("SEQUENCIA", sequencia);
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
				
				String querySeq  = ("SELECT MAX(SEQUENCIA) + 1 AS SEQUENCIA FROM TMDFMD WHERE CODFILA = " + codFila);
				
				ResultSet rsSeq = nativeSql.executeQuery(querySeq);
				
				while (rsSeq.next()) {
					seq = rsSeq.getBigDecimal("SEQUENCIA");
				}

				try {

					System.out.println("CODFILA na inserção do item" + codFila);
					System.out.println("alteração do cod");

					EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
					DynamicVO dynamicVO1 = (DynamicVO) dwfFacade.getDefaultValueObjectInstance("MSDDestFilaMensagem");

					dynamicVO1.setProperty("CODFILA", codFila);
					dynamicVO1.setProperty("EMAIL", "cobranca@crowe.com.br");
					// dynamicVO1.setProperty("EMAIL", "jefferson.costa@covenantit.com.br");
					 //dynamicVO1.setProperty("EMAIL", "t.santos.vasconcelos@hotmail.com");
					dynamicVO1.setProperty("SEQUENCIA", seq);
					PersistentLocalEntity createEntity = dwfFacade.createEntity("MSDDestFilaMensagem",
							(EntityVO) dynamicVO1);
					DynamicVO save = (DynamicVO) createEntity.getValueObject();

					System.out.println("CODFILA " + codFila);

				} catch (Exception e) {
					e.printStackTrace();
					msg = "Erro na inclusao do item " + e.getMessage();
					System.out.println(msg);
				}

				boolean update = nativeSql
						.executeUpdate(" UPDATE TGFCAB SET AD_STATUSEMAIL = 'S' WHERE NUNOTA = " + nuNotaOrig);
				System.out.println(update);
			}
		}
	}
}
