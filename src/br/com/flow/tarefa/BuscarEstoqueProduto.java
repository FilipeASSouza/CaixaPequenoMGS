package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.util.NativeSqlDecorator;

import java.math.BigDecimal;

public class BuscarEstoqueProduto implements TarefaJava {
    public BuscarEstoqueProduto() {
    }

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        BigDecimal codigoproduto = new BigDecimal(contextoTarefa.getCampo("CODPROD").toString());
        NativeSqlDecorator consultaSaldoProduto = new NativeSqlDecorator("SELECT DISTINCT NVL( ( EST.ESTOQUE - EST.RESERVADO - EST.WMSBLOQUEADO ), 0 ) ESTOQUEREAL, PRO.CODPROD, PRO.DESCRPROD FROM TGFEST EST INNER JOIN TGFPRO PRO ON PRO.CODPROD = EST.CODPROD WHERE PRO.ATIVO = 'S' AND PRO.USOPROD IN ('S', 'C') AND PRO.CODPROD = :codigoProduto AND ROWNUM < 2");
        consultaSaldoProduto.setParametro("codigoProduto", codigoproduto);
        if (consultaSaldoProduto.proximo()) {
            BigDecimal estoquereal = consultaSaldoProduto.getValorBigDecimal("ESTOQUEREAL");
            if (estoquereal.compareTo(new BigDecimal(0)) > 0) {
                contextoTarefa.setCampo("VLRESTPRO", String.valueOf(new BigDecimal(estoquereal.toString())));
            } else {
                contextoTarefa.setCampo("VLRESTPRO", String.valueOf(new BigDecimal(0)));
            }
        }

    }
}
