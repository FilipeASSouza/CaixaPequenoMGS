package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;

public class CriarLancamentoCentralCompras implements TarefaJava {

    @Override
    public void executar(ContextoTarefa ct) throws Exception {

        CentralComprasCRUD centralComprasCRUD = new CentralComprasCRUD();

        centralComprasCRUD.criandoCabeçalho(ct);
        centralComprasCRUD.criandoItens(ct);
        centralComprasCRUD.criandoRateio(ct);
        centralComprasCRUD.criandoFinanceiro(ct);
        centralComprasCRUD.integrandoAnexo(ct);
    }
}
