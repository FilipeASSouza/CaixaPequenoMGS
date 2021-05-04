package br.com.flow.agendamento;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.util.NativeSqlDecorator;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;

public class UsuarioFlowProcessamento implements ScheduledAction, AcaoRotinaJava {
    public UsuarioFlowProcessamento() {
    }

    public void onTime(ScheduledActionContext scheduledActionContext) {
        this.processar();
    }

    private void processar() {
        try {
            NativeSqlDecorator nativeSqlDecorator = new NativeSqlDecorator("SELECT NUNICO, USUARIOPORTAL FROM AD_INTEGRAUSURIOPORTAL WHERE ROWNUM <= 20 ORDER BY DHLANC");

            while(nativeSqlDecorator.proximo()) {
                (new UsuarioFlowPortalModel()).processarAcesso(nativeSqlDecorator.getValorBigDecimal("USUARIOPORTAL"));
                JapeFactory.dao("AD_INTEGRAUSURIOPORTAL").deleteByCriteria("NUNICO = ?", new Object[]{nativeSqlDecorator.getValorBigDecimal("NUNICO")});
            }
        } catch (Exception var2) {
            var2.printStackTrace();
        }

    }

    public void doAction(ContextoAcao contextoAcao) throws Exception {
        this.processar();
    }
}