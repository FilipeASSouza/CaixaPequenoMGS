package br.com.flow.agendamento;

import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.TimeUtils;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class DisparoEmailCaixaPequenoJob implements ScheduledAction {

    private JdbcWrapper jdbcWrapper = EntityFacadeFactory.getCoreFacade().getJdbcWrapper();
    private NativeSql consultarCadastroEmailSQL = null;

    public void setup() {
        JapeSessionContext.putProperty("usuario_logado", BigDecimal.ZERO);
        JapeSessionContext.putProperty("emp_usu_logado", (Object)null);
        JapeSessionContext.putProperty("dh_atual", new Timestamp(System.currentTimeMillis()));
        JapeSessionContext.putProperty("d_atual", new Timestamp(TimeUtils.getToday()));
    }

    @Override
    public void onTime(ScheduledActionContext scheduledActionContext) {
        try{
            this.setup();
            this.processar(this.jdbcWrapper);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void processar(JdbcWrapper jdbcWrapper ) {

        try{

            consultarCadastroEmailSQL = new NativeSql(jdbcWrapper);
            consultarCadastroEmailSQL.appendSql("SELECT * FROM AD_EMAILCPQ ORDER BY SEQUENCIA");

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
