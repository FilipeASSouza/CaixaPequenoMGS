package br.com.Evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.ws.ServiceContext;
import br.com.util.ErroUtils;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collection;

public class EventoRateioFlow implements EventoProgramavelJava {

    private final static JapeWrapper rateioCPQ = JapeFactory.dao("AD_RATEIOCPQ");
    private final static JapeWrapper contaContabilCRDAO = JapeFactory.dao("TGFNATCCCR");
    BigDecimal valorTotalLiquidoRegistro = null;

    @Override
    public void beforeInsert(PersistenceEvent event) throws Exception {

        DynamicVO vo = (DynamicVO) event.getVo();
        BigDecimal paramNatureza;
        BigDecimal paramCentroResultado;
        BigDecimal usuarioLogado = ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID();

        if(vo.asBigDecimal("CODNAT") == null){
            paramNatureza = new BigDecimal(VariaveisFlow.getVariavel(vo.asBigDecimal("IDINSTPRN"), "CODNAT").toString());
        } else {
            paramNatureza = vo.asBigDecimal("CODNAT");
        }
        if ( vo.asBigDecimal("CODCENCUS") == null ){
            paramCentroResultado = new BigDecimal(VariaveisFlow.getVariavel(vo.asBigDecimal("IDINSTPRN"), "CODCENCUS").toString());
        } else {
            paramCentroResultado = vo.asBigDecimal("CODCENCUS");
        }

        DynamicVO contaContabilCRVO = contaContabilCRDAO.findOne("NUCLASSIFICACAO = 1 AND CODNAT = ?", new Object[]{paramNatureza});

        vo.setProperty("CODPROJ", new BigDecimal(99990001L));
        vo.setProperty("CODNAT", paramNatureza);
        vo.setProperty("CODCTACTB", contaContabilCRVO.asBigDecimalOrZero("CODCTACTB"));
        vo.setProperty("CODCENCUS", paramCentroResultado);
        vo.setProperty("CODUSU", usuarioLogado);

        validarCampos(vo);
        atualizarCentroResultado(vo);

        if (vo.asBigDecimal("VLRRATEIO") == null){
            valorTotalLiquidoRegistro = buscarValorTotal(event);
        } else if ( vo.asBigDecimal("VLRRATEIO") != null ) {
            BigDecimal valorTotal = VariaveisFlow.getVariavel(vo.asBigDecimal("IDINSTPRN"),"VLRTOT") == null ?
                    vo.asBigDecimal("VLRRATEIO") : new BigDecimal(VariaveisFlow.getVariavel(vo.asBigDecimal("IDINSTPRN"),"VLRTOT").toString());

            valorTotalLiquidoRegistro = valorTotal;
        }

        validarDuplicidade(vo);
        validarRateio(vo);
        Collection<DynamicVO> rateios = rateioCPQ.find("IDINSTPRN = ? AND CODREGISTRO <> ?"
                , new Object[]{vo.asBigDecimal("IDINSTPRN"), vo.asBigDecimal("CODREGISTRO")});
        if(rateios.size() > 0 ){
            atualizaValorRateio(vo);
        }
    }

    private void atualizarCentroResultado(DynamicVO vo) throws Exception {
        JapeWrapper lotacaoDAO = JapeFactory.dao("TGFLOT"); //AD_TGFLOT
        JapeWrapper lotacaoFlowDAO = JapeFactory.dao("AD_LOTCRCPQ"); //AD_LOTCRCPQ
        DynamicVO lotacaoVO;
        DynamicVO lotacaoFlowVO;
        try{
            lotacaoVO = lotacaoDAO.findOne("CODSITE = ?", new Object[]{vo.asBigDecimal("CODSITRATEIO")});
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("Unidade nao localizada na tabela de integracao de lotacoes do Sankhya, fineza verificar com a Tesouraria MGS!");
        }
        try{
            lotacaoFlowVO = lotacaoFlowDAO.findOne("CODLOT = ?", new Object[]{lotacaoVO.asBigDecimal("CODLOT")});
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("Lotacao nao localizada na tabela de apoio do Caixa Pequeno, fineza verificar com a Tesouraria MGS!");
        }
        vo.setProperty("CODCENCUS", lotacaoFlowVO.asBigDecimalOrZero("CODCENCUS"));
    }

    private void validarDuplicidade(DynamicVO vo) throws Exception {
        Collection<DynamicVO> rateios = rateioCPQ.find("IDINSTPRN = ? AND CODREGISTRO <> ?"
                , new Object[]{vo.asBigDecimal("IDINSTPRN"), vo.asBigDecimal("CODREGISTRO")});
        for (DynamicVO rateio: rateios){
            if (rateio.asBigDecimal("CODPROJ").equals(vo.asBigDecimal("CODPROJ"))
                    && rateio.asBigDecimal("CODCENCUS").equals(vo.asBigDecimal("CODCENCUS"))
                    && rateio.asBigDecimal("CODNAT").equals(vo.asBigDecimal("CODNAT"))
                    && rateio.asBigDecimal("CODSITRATEIO").equals(vo.asBigDecimal("CODSITRATEIO"))){
                throw new Exception("Dados incorretos no rateio, fineza verificar!");
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
        atualizarCentroResultado(vo);
    }

    private void atualizaValorRateio(DynamicVO vo) {
        BigDecimal resultadoValorEvento;
        BigDecimal percentualEvento = vo.asBigDecimal("PERCRATEIO");
        percentualEvento = percentualEvento.divide(BigDecimal.valueOf(100L));
        resultadoValorEvento = percentualEvento.multiply(valorTotalLiquidoRegistro);
        vo.setProperty("VLRRATEIO", resultadoValorEvento.setScale(3,RoundingMode.HALF_EVEN));
    }

    private void validarCampos(DynamicVO vo) throws Exception {
        if(vo.asBigDecimal("CODPROJ") == null){
            throw new Exception("O projeto não foi informado, fineza verificar!");
        } else if (vo.asBigDecimal("CODCTACTB")==null) {
            throw new Exception("A conta contabil não foi informada, fineza verificar!");
        } else if (vo.asBigDecimal("CODSITRATEIO")==null) {
            throw new Exception("A unidade faturamento não foi informada, fineza verificar!");
        } else if (vo.asBigDecimal("CODNAT")==null) {
            throw new Exception("A natureza não foi informada, fineza verificar!");
        } else if (vo.asBigDecimal("CODCENCUS") == null) {
            throw new Exception("O centro de resultado não foi informado, fineza verificar!");
        }
    }

    private BigDecimal buscarValorTotal(PersistenceEvent event) throws Exception {

        DynamicVO vo = (DynamicVO) event.getVo();
        BigDecimal valorTotal = BigDecimal.ZERO;
        BigDecimal idInstanciaProcesso = vo.asBigDecimal("IDINSTPRN");

        valorTotal = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "VLRTOT").toString());

        BigDecimal resultado = valorTotal;

        return resultado;
    }

    @Override
    public void beforeDelete(PersistenceEvent event) throws Exception {

        try{
            DynamicVO vo = (DynamicVO) event.getVo();
            JapeWrapper permissaoRateioFlowDAO = JapeFactory.dao("AD_PERRATEIOCPQ"); //AD_PERRATEIOCPQ
            DynamicVO permissaoRateioFlowVO = permissaoRateioFlowDAO.findOne("CODUSU = ?", new Object[]{vo.asBigDecimalOrZero("CODUSU")});
            if (permissaoRateioFlowVO.asString("PERMITEALTERACAO").equalsIgnoreCase("1")){
                throw new Exception("Usuário não possui permissão para alteração!");
            }
        } catch (Exception e){
            e.printStackTrace();
            throw new Exception("Usuário não possui permissão para alteração!");
        }
    }

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
                    throw new Exception("Percentual nao pode ultrapassar 100%! Fineza verificar!");
                }
            }
        }
    }
}
