package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;

public class BuscarDadosUsuario implements TarefaJava {

    public BuscarDadosUsuario() {
    }

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        BigDecimal usuarioLogado = contextoTarefa.getUsuarioLogado();
        Object idInstanceProcesso = contextoTarefa.getIdInstanceProcesso();
        NativeSqlDecorator nativeSqlBuscarDadosUsuario = new NativeSqlDecorator("SELECT V.COD_PARC, V.EMAIL, V.LOGIN CPF, V.UNIDADE FROM TSIUSU T INNER JOIN VIEW_USUARIOS_PORTALMGS V ON (SUBSTR(T.NOMEUSU, 4, 5)+0 = V.ID_USUARIO) WHERE T.CODUSU = :CODUSU");
        nativeSqlBuscarDadosUsuario.setParametro("CODUSU", usuarioLogado);
        if (nativeSqlBuscarDadosUsuario.proximo()) {
            String email = nativeSqlBuscarDadosUsuario.getValorString("EMAIL");
            String emailTesouraria = "tesouraria@mgs.srv.br";
            VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILUSU", String.valueOf(email));
            VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILTES", String.valueOf(emailTesouraria));
        }
    }
}
