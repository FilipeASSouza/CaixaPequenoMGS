package br.com.flow.tarefa;

import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

import java.math.BigDecimal;

public class ProcessarLancamento {

    private static JapeWrapper instanciaTarefaDAO = JapeFactory.dao("InstanciaTarefa");//TWFITAR
    private static JapeWrapper instanciaVariavelDAO = JapeFactory.dao("InstanciaVariavel");//TWFIVAR
    private static JapeWrapper instanciaProcessoDAO = JapeFactory.dao("InstanciaProcesso");//TWFIPRN

    public void excluirLancamentosTemporarios(BigDecimal numeroLancamento) throws Exception {

        instanciaTarefaDAO.deleteByCriteria("IDINSTPRN = ?", new Object[]{numeroLancamento});
        instanciaVariavelDAO.deleteByCriteria("IDINSTPRN = ?", new Object[]{numeroLancamento});
        instanciaProcessoDAO.deleteByCriteria("IDINSTPRN = ?", new Object[]{numeroLancamento});

    }
}
