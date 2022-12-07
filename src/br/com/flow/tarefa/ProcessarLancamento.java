package br.com.flow.tarefa;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

import java.math.BigDecimal;
import java.util.Collection;

public class ProcessarLancamento {

    private static JapeWrapper instanciaTarefaDAO = JapeFactory.dao("InstanciaTarefa");//TWFITAR
    private static JapeWrapper instanciaVariavelDAO = JapeFactory.dao("InstanciaVariavel");//TWFIVAR
    private static JapeWrapper instanciaProcessoDAO = JapeFactory.dao("InstanciaProcesso");//TWFIPRN
    private static JapeWrapper instanciaHistoricoDAO = JapeFactory.dao("HistoricoInstanciaProcesso");//TWFIHIS

    public void excluirLancamentosTemporarios(BigDecimal numeroLancamento) throws Exception {

        instanciaTarefaDAO.deleteByCriteria("IDINSTPRN = ?", new Object[]{numeroLancamento});
        instanciaVariavelDAO.deleteByCriteria("IDINSTPRN = ?", new Object[]{numeroLancamento});

        Collection<DynamicVO> lancamentos = instanciaHistoricoDAO.find("IDINSTPRN = ?", new Object[]{numeroLancamento});
        for (DynamicVO lancamento : lancamentos){
            instanciaHistoricoDAO.deleteByCriteria("IDINSTPRN = ?", new Object[]{lancamento.asBigDecimal("IDINSTPRN")});
        }

        instanciaProcessoDAO.deleteByCriteria("IDINSTPRN = ?", new Object[]{numeroLancamento});

    }
}
