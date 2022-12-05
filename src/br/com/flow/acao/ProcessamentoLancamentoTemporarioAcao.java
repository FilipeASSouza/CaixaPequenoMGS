package br.com.flow.acao;

import br.com.flow.tarefa.ProcessarLancamento;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class ProcessamentoLancamentoTemporarioAcao implements AcaoRotinaJava {

    private JdbcWrapper jdbcWrapper = EntityFacadeFactory.getCoreFacade().getJdbcWrapper();

    @Override
    public void doAction(ContextoAcao contextoAcao) throws Exception { this.processar(contextoAcao); }

    private void processar(ContextoAcao contextoAcao) throws Exception {

        boolean exclusaoRegistros = contextoAcao.confirmarSimNao("Exclusão de Registros Temporarios Flow", "Deseja continuar com a exclusão dos lançamentos temporários?", 1);

        if(Boolean.TRUE.equals(exclusaoRegistros)){
            QueryExecutor consultaRegistros = contextoAcao.getQuery();
            consultaRegistros.nativeSelect("SELECT * FROM TWFIPRN WHERE DHINCLUSAO <= (SYSDATE - 2)");
            while (consultaRegistros.next()){
                ProcessarLancamento processarLancamento = new ProcessarLancamento();
                processarLancamento.excluirLancamentosTemporarios(consultaRegistros.getBigDecimal("IDINSTPRN"));
            }
            consultaRegistros.close();
        }
    }
}
