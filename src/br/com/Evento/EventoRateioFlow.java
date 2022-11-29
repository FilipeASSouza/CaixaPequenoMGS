package br.com.Evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.util.ErroUtils;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;
import java.util.Collection;

public class EventoRateioFlow implements EventoProgramavelJava {

    private final JapeWrapper rateioCPQ = JapeFactory.dao("AD_RATEIOCPQ");
    BigDecimal valorTotalLiquidoRegistro = null;

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {
        DynamicVO vo = (DynamicVO) event.getVo();
        validarCampos(vo);
        if (vo.asBigDecimal("VLRRATEIO") == null){
            valorTotalLiquidoRegistro = buscarValorTotal(event);
        } else {
            valorTotalLiquidoRegistro = vo.asBigDecimal("VLRRATEIO");
        }
        validarDuplicidade(vo);
        validarRateio(vo);
        Collection<DynamicVO> rateios = rateioCPQ.find("IDINSTPRN = ? AND CODREGISTRO <> ?"
                , new Object[]{vo.asBigDecimal("IDINSTPRN"), vo.asBigDecimal("CODREGISTRO")});
        if(rateios.size() > 0 ){
            atualizaValorRateio(vo);
        }
    }

    private void validarDuplicidade(DynamicVO vo) throws Exception {
        Collection<DynamicVO> rateios = rateioCPQ.find("IDINSTPRN = ? AND CODREGISTRO <> ?"
                , new Object[]{vo.asBigDecimal("IDINSTPRN"), vo.asBigDecimal("CODREGISTRO")});
        for (DynamicVO rateio: rateios){
            if (rateio.asBigDecimal("CODPROJ").equals(vo.asBigDecimal("CODPROJ"))
                    && rateio.asBigDecimal("CODCENCUS").equals(vo.asBigDecimal("CODCENCUS"))
                    && rateio.asBigDecimal("CODNAT").equals(vo.asBigDecimal("CODNAT"))
                    && rateio.asBigDecimal("CODSITRATEIO").equals(vo.asBigDecimal("CODSITRATEIO"))){
                ErroUtils.disparaErro("Dados incorretos no rateio, fineza verificar!");
            }
        }
    }

    @Override
    public void beforeUpdate(PersistenceEvent event) throws Exception {
        DynamicVO vo = (DynamicVO) event.getVo();
        valorTotalLiquidoRegistro = buscarValorTotal(event);

        if(BigDecimal.valueOf(100L).compareTo(vo.asBigDecimal("PERCRATEIO")) < 0){
            ErroUtils.disparaErro("Percentual nao pode ultrapassar 100%! Fineza verificar!");
        }

        validarRateio(vo);
        atualizaValorRateio(vo);
        validarDuplicidade(vo);
        validarCampos(vo);
    }

    private void atualizaValorRateio(DynamicVO vo) {
        BigDecimal resultadoValorEvento;
        BigDecimal percentualEvento = vo.asBigDecimal("PERCRATEIO");
        percentualEvento = percentualEvento.divide(BigDecimal.valueOf(100L));
        resultadoValorEvento = percentualEvento.multiply(valorTotalLiquidoRegistro);
        vo.setProperty("VLRRATEIO", resultadoValorEvento);
    }

    private void validarCampos(DynamicVO vo) throws Exception {
        if(vo.asBigDecimal("CODPROJ") == null){
            ErroUtils.disparaErro("O projeto não foi informado, fineza verificar!");
        } else if (vo.asBigDecimal("CODCTACTB")==null) {
            ErroUtils.disparaErro("A conta contabil não foi informada, fineza verificar!");
        } else if (vo.asBigDecimal("CODSITRATEIO")==null) {
            ErroUtils.disparaErro("A unidade faturamento não foi informada, fineza verificar!");
        } else if (vo.asBigDecimal("CODNAT")==null) {
            ErroUtils.disparaErro("A natureza não foi informada, fineza verificar!");
        } else if (vo.asBigDecimal("CODCENCUS") == null) {
            ErroUtils.disparaErro("O centro de resultado não foi informado, fineza verificar!");
        }
    }

    private BigDecimal buscarValorTotal(PersistenceEvent event) throws Exception {

        DynamicVO vo = (DynamicVO) event.getVo();
        BigDecimal valorTotal = BigDecimal.ZERO;
        BigDecimal valorDesconto = BigDecimal.ZERO;
        BigDecimal idInstanciaProcesso = vo.asBigDecimal("IDINSTPRN");

        valorTotal = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "VLRTOT").toString());
        valorDesconto = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "VLRDESCTOT").toString());

        BigDecimal resultado = valorTotal.subtract(valorDesconto);

        return resultado;
    }

    @Override
    public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception {}

    @Override
    public void afterInsert(PersistenceEvent persistenceEvent) throws Exception {}

    @Override
    public void afterUpdate(PersistenceEvent persistenceEvent) throws Exception {}

    @Override
    public void afterDelete(PersistenceEvent persistenceEvent) throws Exception {}

    @Override
    public void beforeCommit(TransactionContext transactionContext) throws Exception {}

    private void validarRateio(DynamicVO vo) throws Exception {

        BigDecimal percentualEvento = vo.asBigDecimal("PERCRATEIO");
        Collection<DynamicVO> rateios = rateioCPQ.find("IDINSTPRN = ? AND CODREGISTRO <> ?"
                , new Object[]{vo.asBigDecimal("IDINSTPRN"), vo.asBigDecimal("CODREGISTRO")});
        if(rateios != null){
            for (DynamicVO registro: rateios){
                percentualEvento = percentualEvento.add(registro.asBigDecimal("PERCRATEIO"));
                if (BigDecimal.valueOf(100L).compareTo(percentualEvento) < 0){
                    ErroUtils.disparaErro("Percentual nao pode ultrapassar 100%! Fineza verificar!");
                }
            }
        }
    }
}
