package br.com.flow.acao;

import br.com.flow.agendamento.DisparoEmailCaixaPequenoProcessamento;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.sql.ResultSet;

public class DisparoEmailCaixaPequenoAcao implements AcaoRotinaJava {

    private JdbcWrapper jdbcWrapper = EntityFacadeFactory.getCoreFacade().getJdbcWrapper();
    private NativeSql consultarCadastroEmailSQL = null;

    @Override
    public void doAction(ContextoAcao contextoAcao) {this.processar(this.jdbcWrapper);}

    public void processar(JdbcWrapper jdbcWrapper ) {

        try{

            consultarCadastroEmailSQL = new NativeSql(jdbcWrapper);
            consultarCadastroEmailSQL.appendSql("SELECT * FROM AD_EMAILCPQ WHERE SEQUENCIA = 49");

            ResultSet rs;
            rs = consultarCadastroEmailSQL.executeQuery();

            DisparoEmailCaixaPequenoProcessamento processamentoDisparo = new DisparoEmailCaixaPequenoProcessamento();
            processamentoDisparo.processarEmail(rs, consultarCadastroEmailSQL);

            rs.close();

        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (consultarCadastroEmailSQL != null){
                NativeSql.releaseResources(consultarCadastroEmailSQL);
            }
            jdbcWrapper.closeSession();
        }
    }
}
