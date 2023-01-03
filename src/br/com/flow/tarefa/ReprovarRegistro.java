package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.util.BuscarDadosUsuarioLogado;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;

public class ReprovarRegistro implements TarefaJava {

    JapeWrapper lancamentoFlowDAO = JapeFactory.dao("AD_FINCAIXAPQ");

    public ReprovarRegistro() {
    }

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        BigDecimal usuarioLogado = new BigDecimal(String.valueOf(contextoTarefa.getUsuarioLogado()));
        BuscarDadosUsuarioLogado buscarDadosUsuarioLogado = new BuscarDadosUsuarioLogado();
        BigDecimal idInstanceProcesso = new BigDecimal(String.valueOf(contextoTarefa.getIdInstanceProcesso()));

        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "LOGIN_REP", usuarioLogado);
        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "EMAIL_REP", String.valueOf(buscarDadosUsuarioLogado.BuscarEmailUsuarioLogado(usuarioLogado)));

        DynamicVO lancamentoFlowVO = lancamentoFlowDAO.findOne("IDINSTPRN = ?", new Object[]{idInstanceProcesso});
        FluidUpdateVO lancamentoFlowFUVO = lancamentoFlowDAO.prepareToUpdate(lancamentoFlowVO);
        lancamentoFlowFUVO.set("CODREPROVADOR", usuarioLogado);
        lancamentoFlowFUVO.update();

        String statusLimite = VariaveisFlow.getVariavel(idInstanceProcesso, "STATUSLIMITE").toString();
        if (statusLimite.equalsIgnoreCase("2")){
            VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "LOGIN_REP_DIRETOR", usuarioLogado);
        }
    }
}
