package br.com.Evento;

import br.com.sankhya.extensions.flow.ContextoEvento;
import br.com.sankhya.extensions.flow.EventoProcessoJava;
import br.com.util.ErroUtils;

public class ProcessoAquisicao implements EventoProcessoJava {
    @Override
    public void executar(ContextoEvento ct) throws Exception {
        if( ct.getCampo("JUSTIFICARLIMITE").toString().equalsIgnoreCase("")
            || ct.getCampo("JUSTIFICARLIMITE") == null ){
            ErroUtils.disparaErro("Necessario informar a justificativa de aquisição, fineza verificar!");
        }
    }
}
