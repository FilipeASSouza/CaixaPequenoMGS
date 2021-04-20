package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.util.BuscarDadosUsuarioLogado;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;

public class AprovarRegistro implements TarefaJava {
    public AprovarRegistro() {
    }

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        BigDecimal usuarioLogado = new BigDecimal(String.valueOf(contextoTarefa.getUsuarioLogado()));
        BuscarDadosUsuarioLogado buscarDadosUsuarioLogado = new BuscarDadosUsuarioLogado();
        BigDecimal idInstanceProcesso = new BigDecimal(String.valueOf(contextoTarefa.getIdInstanceProcesso()));
        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "LOGIN_APR", usuarioLogado);
        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "EMAIL_APR", String.valueOf(buscarDadosUsuarioLogado.BuscarEmailUsuarioLogado(usuarioLogado)));
    }
}
