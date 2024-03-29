package br.com.flow.agendamento;

import br.com.flow.tarefa.ProcessarLancamento;
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

public class ProcessamentoLancamentoTemporarioJob implements ScheduledAction {

    private JdbcWrapper jdbcWrapper = EntityFacadeFactory.getCoreFacade().getJdbcWrapper();
    private NativeSql nativeSql = null;

    public void setup() {
        JapeSessionContext.putProperty("usuario_logado", BigDecimal.ZERO);
        JapeSessionContext.putProperty("emp_usu_logado", BigDecimal.ZERO);
        JapeSessionContext.putProperty("dh_atual", new Timestamp(System.currentTimeMillis()));
        JapeSessionContext.putProperty("d_atual", new Timestamp(TimeUtils.getToday()));
    }

    @Override
    public void onTime(ScheduledActionContext scheduledActionContext) {
        try{
            this.setup();
            this.processar();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private void processar() throws Exception {
        try {
            nativeSql = new NativeSql(this.jdbcWrapper);
            nativeSql.appendSql("SELECT * FROM TWFIPRN WHERE CODPRN = 24 AND DHINCLUSAO <= (SYSDATE - 2) AND DHCONCLUSAO IS NULL ORDER BY DHINCLUSAO");
            ResultSet rs;
            for (rs = this.nativeSql.executeQuery(); rs.next(); ) {
                ProcessarLancamento processarLancamento = new ProcessarLancamento();
                processarLancamento.excluirLancamentosTemporarios(rs.getBigDecimal("IDINSTPRN"));
            }
            rs.close();
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if( nativeSql != null ){
                NativeSql.releaseResources(nativeSql);
            }
            jdbcWrapper.closeSession();
        }
    }
}
