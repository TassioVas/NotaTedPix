package br.com.crowe.notas;

import java.math.BigDecimal;
import java.sql.ResultSet;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.core.JapeSession.SessionHandle;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;

public class ExcluirCab implements AcaoRotinaJava {
	
	BigDecimal nuNota;
	BigDecimal count;


	@Override
	public void doAction(ContextoAcao contexto) throws Exception {
		// TODO Auto-generated method stub
		
		System.out.println("INICIO DO CODIGO INCLUIR");

		JdbcWrapper JDBC = JapeFactory.getEntityFacade().getJdbcWrapper();
		NativeSql nativeSql = new NativeSql(JDBC);
		SessionHandle hnd = JapeSession.open();

		if ((contexto.getLinhas()).length == 0) {
			contexto.mostraErro("Selecione um registro antes.");
			System.out.println("Entrou no if ");
		} else if ((contexto.getLinhas()).length > 1) {
			contexto.mostraErro("Selecione apenas um registro.");
		}
		System.out.println("Entrou na linha 36 : ");
		Registro linha = contexto.getLinhas()[0];
		nuNota = (BigDecimal) linha.getCampo("NUNOTA");
		System.out.println("Capturou o nunota  " + nuNota);
		

		ResultSet query = nativeSql.executeQuery(
				"SELECT AD_STATUSEMAIL," + " COUNT(*) AS CONTADOR FROM TGFCAB WHERE AD_STATUSEMAIL <> 'NULL' "
						+ " AND NUNOTA = " + nuNota + "GROUP BY AD_STATUSEMAIL");

		System.out.println("Entrou na linha 45 : ");

		if (!query.next() || query.getInt("CONTADOR") == 1) {
			boolean update = nativeSql
					.executeUpdate(" UPDATE TGFCAB SET AD_STATUSEMAIL = 'NULL' WHERE NUNOTA = " + nuNota);
			System.out.println("Entrou na linha 50 : ");

			contexto.setMensagemRetorno(
					"Nota : " + nuNota + "\r\n " + " Status de Email Enviado Alterado com sucesso."
							+ "\r\n"
							+ "Favor Verificar Nota na tela Nota Ted/Pix.");
			System.out.println("Entrou na linha 54 : ");
			
		} else {
			contexto.setMensagemRetorno("Nota : " + nuNota + "\r\n " + "Com Status de email Eviado ");
			System.out.println("Entrou na linha 58 : ");
			return;
		}
		
	}

}
