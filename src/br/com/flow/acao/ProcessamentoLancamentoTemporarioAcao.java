package br.com.flow.acao;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class ProcessamentoLancamentoTemporarioAcao implements AcaoRotinaJava {

    private JdbcWrapper jdbcWrapper = EntityFacadeFactory.getCoreFacade().getJdbcWrapper();

    @Override
    public void doAction(ContextoAcao contextoAcao) throws Exception { this.processar(); }

    private void processar(){

    }
}
