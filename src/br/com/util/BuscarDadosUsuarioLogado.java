package br.com.util;

import java.math.BigDecimal;

public class BuscarDadosUsuarioLogado {

    public BuscarDadosUsuarioLogado() {
    }

    public String BuscarEmailUsuarioLogado(BigDecimal usuarioLogado) throws Exception {
        NativeSqlDecorator nativeSqlBuscarDadosUsuario = new NativeSqlDecorator("SELECT NVL( USU.EMAIL, V.EMAIL ) EMAIL FROM VIEW_CAIXAPQ V  LEFT JOIN TSIUSU USU ON USU.CODUSU = V.CODUSU WHERE USU.CODUSU = :CODUSU AND ROWNUM <= 1");
        nativeSqlBuscarDadosUsuario.setParametro("CODUSU", usuarioLogado);
        if (nativeSqlBuscarDadosUsuario.proximo()) {
            String email = nativeSqlBuscarDadosUsuario.getValorString("EMAIL");
            return email;
        } else {
            return String.valueOf("tesouraria@mgs.srv.br");
        }
    }
}
