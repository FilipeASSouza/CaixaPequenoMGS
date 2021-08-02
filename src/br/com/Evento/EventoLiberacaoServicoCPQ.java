package br.com.Evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;

import java.math.BigDecimal;

public class EventoLiberacaoServicoCPQ implements EventoProgramavelJava {
    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {
    }

    @Override
    public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void afterInsert(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void afterUpdate(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        if( vo.asBigDecimal("CODTIPOPER") != null && ( vo.asBigDecimal("CODTIPOPER").equals(BigDecimal.valueOf(613L))
        && vo.asTimestamp("DHLIB") != null) ){
            JapeWrapper financeiroDAO = JapeFactory.dao("Financeiro");
            DynamicVO financeiroVO = financeiroDAO.findOne("NUNOTA = ?", new Object[]{vo.asBigDecimal("NUCHAVE")});
            ControleLiberacao.limpaFila(financeiroVO.asBigDecimal("NUFIN"));
        }
    }

    @Override
    public void afterDelete(PersistenceEvent persistenceEvent) throws Exception {

    }

    @Override
    public void beforeCommit(TransactionContext transactionContext) throws Exception {

    }
}
