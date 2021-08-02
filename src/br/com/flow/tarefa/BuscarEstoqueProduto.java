package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;

public class BuscarEstoqueProduto implements TarefaJava {
    public BuscarEstoqueProduto() {
    }

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        /*BigDecimal codigoproduto = new BigDecimal(Long.parseLong(contextoTarefa.getCampo("CODPROD").toString()));
        NativeSqlDecorator consultaSaldoProduto = new NativeSqlDecorator("SELECT DISTINCT NVL( ( EST.ESTOQUE - EST.RESERVADO - EST.WMSBLOQUEADO ), 0 ) ESTOQUEREAL, PRO.CODPROD, PRO.DESCRPROD FROM TGFPRO PRO LEFT JOIN TGFEST EST ON PRO.CODPROD = EST.CODPROD WHERE PRO.ATIVO = 'S' AND PRO.CODPROD = :codigoProduto AND ROWNUM < 2");
        consultaSaldoProduto.setParametro("codigoProduto", codigoproduto);
        if (consultaSaldoProduto.proximo()) {
            //BigDecimal estoquereal = consultaSaldoProduto.getValorBigDecimal("ESTOQUEREAL");
            if (estoquereal.compareTo(new BigDecimal(0)) > 0) {
                contextoTarefa.setCampo("VLRESTPRO", String.valueOf(new BigDecimal(estoquereal.toString())));
            } else {
                contextoTarefa.setCampo("VLRESTPRO", String.valueOf(new BigDecimal(0)));
            }
        }*/
    }
}
