package br.com.util;

import java.math.BigDecimal;

public class BuscarDadosUsuarioLogado {

    public BuscarDadosUsuarioLogado() {
    }

    public String BuscarEmailUsuarioLogado(BigDecimal usuarioLogado) throws Exception {
        NativeSqlDecorator nativeSqlBuscarDadosUsuario = new NativeSqlDecorator("SELECT \nV.COD_PARC,\nV.EMAIL, \nV.LOGIN CPF,\nV.UNIDADE\nFROM TSIUSU T INNER JOIN VIEW_USUARIOS_PORTALMGS V ON (SUBSTR(T.NOMEUSU, 4, 5)+0 = V.ID_USUARIO)\nWHERE T.CODUSU = :CODUSU");
        nativeSqlBuscarDadosUsuario.setParametro("CODUSU", usuarioLogado);
        if (nativeSqlBuscarDadosUsuario.proximo()) {
            String email = nativeSqlBuscarDadosUsuario.getValorString("EMAIL");
            return email;
        } else {
            return String.valueOf("tesouraria@mgs.srv.br");
        }
    }
}
