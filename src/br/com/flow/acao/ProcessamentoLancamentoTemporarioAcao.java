package br.com.flow.acao;

import br.com.flow.tarefa.ProcessarLancamento;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.QueryExecutor;

import java.math.BigDecimal;

public class ProcessamentoLancamentoTemporarioAcao implements AcaoRotinaJava {

    @Override
    public void doAction(ContextoAcao contextoAcao) throws Exception { this.processar(contextoAcao); }

    private void processar(ContextoAcao contextoAcao) throws Exception {

        boolean exclusaoRegistros = contextoAcao.confirmarSimNao("Exclusão de Registros Temporarios Flow", "Deseja continuar com a exclusão dos lançamentos temporários?", 1);

        if(Boolean.TRUE.equals(exclusaoRegistros)){
            QueryExecutor consultaParametroPeriodo = contextoAcao.getQuery();
            consultaParametroPeriodo.setParam("PARAM", "PERIODOEXECLUSAO");
            consultaParametroPeriodo.nativeSelect("SELECT * FROM AD_PARAMCP WHERE PARAMETRO = {PARAM}");
            BigDecimal periodo = null;
            if(consultaParametroPeriodo.next()){
                periodo = consultaParametroPeriodo.getBigDecimal("VALOR");
            }

            QueryExecutor consultaRegistros = contextoAcao.getQuery();
            consultaRegistros.setParam("PERIODO", periodo );
            consultaRegistros.nativeSelect("SELECT * FROM TWFIPRN WHERE CODPRN = 24 AND DHINCLUSAO <= (SYSDATE - {PERIODO}) AND DHCONCLUSAO IS NULL ORDER BY DHINCLUSAO");
            while (consultaRegistros.next()){
                ProcessarLancamento processarLancamento = new ProcessarLancamento();
                processarLancamento.excluirLancamentosTemporarios(consultaRegistros.getBigDecimal("IDINSTPRN"));
            }
            consultaParametroPeriodo.close();
            consultaRegistros.close();
        }
    }
}
