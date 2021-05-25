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
        NativeSqlDecorator nativeSqlBuscarDadosUsuario = new NativeSqlDecorator("SELECT NVL( USU.EMAIL, V.EMAIL ) EMAIL, V.UNIDADE FROM TSIUSU USU LEFT JOIN VIEW_CAIXAPQ V ON USU.CODUSU = USU.CODUSU WHERE USU.CODUSU = :CODUSU AND ROWNUM <= 1");
        nativeSqlBuscarDadosUsuario.setParametro("CODUSU", usuarioLogado);
        if (nativeSqlBuscarDadosUsuario.proximo()) {
            String email = nativeSqlBuscarDadosUsuario.getValorString("EMAIL");
            BigDecimal unidade = nativeSqlBuscarDadosUsuario.getValorBigDecimal("UNIDADE");
            String emailTesouraria = "tesouraria@mgs.srv.br";
            String emailContabilidade = "katia.vilela@mgs.srv.br";
            VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILUSU", String.valueOf(email));
            VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILTES", String.valueOf(emailTesouraria));
            VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILCOT", String.valueOf(emailContabilidade));
            if(unidade == null){
                unidade = BigDecimal.ZERO;
            }
            VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "UNID_FATURAMENTO", unidade);
        }
    }
}
