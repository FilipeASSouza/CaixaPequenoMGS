package br.com.flow.agendamento;

import br.com.flow.tarefa.ProcessarLancamento;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.util.NativeSqlDecorator;
import com.sankhya.util.TimeUtils;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ProcessamentoLancamentoTemporarioJob implements ScheduledAction {

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

        NativeSqlDecorator consultarLancamentos = new NativeSqlDecorator("SELECT * FROM TWFIPRN WHERE DHINCLUSAO <= (SYSDATE - 2) AND DHCONCLUSAO IS NULL ORDER BY DHINCLUSAO");
        while (consultarLancamentos.proximo()){
            ProcessarLancamento processarLancamento = new ProcessarLancamento();
            processarLancamento.excluirLancamentosTemporarios(consultarLancamentos.getValorBigDecimal("IDINSTPRN"));
        }
        consultarLancamentos.close();
    }
}
