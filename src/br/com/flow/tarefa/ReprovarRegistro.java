package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.util.BuscarDadosUsuarioLogado;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;

public class ReprovarRegistro implements TarefaJava {
    public ReprovarRegistro() {
    }

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        BigDecimal usuarioLogado = new BigDecimal(String.valueOf(contextoTarefa.getUsuarioLogado()));
        BuscarDadosUsuarioLogado buscarDadosUsuarioLogado = new BuscarDadosUsuarioLogado();
        BigDecimal idInstanceProcesso = new BigDecimal(String.valueOf(contextoTarefa.getIdInstanceProcesso()));

        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "LOGIN_REP", usuarioLogado);
        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "EMAIL_REP", String.valueOf(buscarDadosUsuarioLogado.BuscarEmailUsuarioLogado(usuarioLogado)));

        String statusLimite = VariaveisFlow.getVariavel(idInstanceProcesso, "STATUSLIMITE").toString();
        if (statusLimite.equalsIgnoreCase("2")){
            VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "LOGIN_REP_DIRETOR", usuarioLogado);
        }
    }
}
