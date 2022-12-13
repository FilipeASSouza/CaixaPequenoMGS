package br.com.Evento;

import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.flow.ContextoEvento;
import br.com.sankhya.extensions.flow.EventoProcessoJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;

public class BuscarDadosCliente implements EventoProcessoJava {

    private final static JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");
    private final static JapeWrapper rateioCPQ = JapeFactory.dao("AD_RATEIOCPQ");
    private final static JapeWrapper contaContabilCRDAO = JapeFactory.dao("TGFNATCCCR");
    private final static JapeWrapper centroResultadoLotacaoDAO = JapeFactory.dao("AD_LOTCRCPQ");

    public BuscarDadosCliente() {
    }

    public void executar(ContextoEvento contextoEvento) throws Exception {

        String cnpjTela = null;
        BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
        BigDecimal idInstanciaTarefa = new BigDecimal(0L);
        Object codigoLotacao = contextoEvento.getCampo("COD_LOTACAO") == null ? VariaveisFlow.getVariavel(idInstanciaProcesso, "COD_LOTACAO")
                : contextoEvento.getCampo("COD_LOTACAO");
        Object cnpj = contextoEvento.getCampo("CNPJ") == null ? VariaveisFlow.getVariavel(idInstanciaProcesso, "CNPJ") : contextoEvento.getCampo("CNPJ");
        String statusLimite = VariaveisFlow.getVariavel(idInstanciaProcesso, "STATUSLIMITE") == null ? ""
                : VariaveisFlow.getVariavel(idInstanciaProcesso, "STATUSLIMITE").toString();

        if(statusLimite == null || statusLimite.equalsIgnoreCase("") || statusLimite.equalsIgnoreCase("null") || statusLimite.equalsIgnoreCase("1")){

            QueryExecutor consultaParemtroTabApoio = contextoEvento.getQuery();
            consultaParemtroTabApoio.setParam("PARAM", "TABCRLOT");
            consultaParemtroTabApoio.nativeSelect("SELECT * FROM AD_PARAMCP WHERE PARAMETRO = {PARAM}");
            if (consultaParemtroTabApoio.next()){
                if (consultaParemtroTabApoio.getString("STATUS").equalsIgnoreCase("2")){

                    DynamicVO centroResultadoLotacaoVO = centroResultadoLotacaoDAO.findOne("CODLOT = ?", new Object[]{codigoLotacao});
                    Object tipoper = VariaveisFlow.getVariavel(idInstanciaProcesso, "TIPOPER");
                    if(tipoper.toString().equalsIgnoreCase("S")){
                        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", centroResultadoLotacaoVO.asBigDecimal("NUMCONTRATO"));
                    }
                    VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "CODCENCUS", centroResultadoLotacaoVO.asBigDecimal("CODCENCUS"));
                } else {

                    QueryExecutor consultarDadosContrato = contextoEvento.getQuery();
                    consultarDadosContrato.setParam("CODLOT", codigoLotacao);
                    consultarDadosContrato.nativeSelect("select mgstctcontrcent.numcontrato AD_NUMCONTRATO\n" +
                            "from mgstctcontrcent\n" +
                            "inner join mgstctcontrato on mgstctcontrato.numcontrato = Mgstctcontrcent.Numcontrato and Mgstctcontrato.Codtipsituacao = 1\n" +
                            "inner join ad_tgflot lot on (lot.codsite = mgstctcontrcent.codsite)\n" +
                            "inner join tgfsit    sit ON sit.codsite = lot.codsite\n" +
                            "where nvl(Mgstctcontrcent.Dtfim,sysdate) >= sysdate\n" +
                            "and lot.codlot = {CODLOT}\n" +
                            "and mgstctcontrcent.numcontrato = sit.ad_numcontrato");
                    if (consultarDadosContrato.next()){
                        //VariaveisFlow.setVariavel(idInstanciaProcesso, BigDecimal.ZERO,"COD_LOTACAO", consultarDadosContrato.getString("CODLOT"));

                        Object tipoper = VariaveisFlow.getVariavel(idInstanciaProcesso, "TIPOPER");
                        if(tipoper.toString().equalsIgnoreCase("S")){
                            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", consultarDadosContrato.getString("AD_NUMCONTRATO"));
                        }
                    }
        //        else if (!consultarDadosContrato.next()){
        //            ErroUtils.disparaErro("Unidade de faturamento e contrato não localizado para o Usuário, fineza verificar com COCOP!");
        //        }
                    consultarDadosContrato.close();
                }
            }
            consultaParemtroTabApoio.close();
        }

        if( cnpj != null ) {
            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{String.valueOf(cnpj)});
            if(parceiroVO == null){
                throw new Exception(VariaveisFlow.PARCEIRO_NAO_ENCONTRADO);
            }
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
        } else if(codigoLotacao != null){

            cnpjTela = VariaveisFlow.getVariavel(idInstanciaProcesso, "CNPJ").toString();

            if(cnpjTela == null){
                throw new Exception("Necessario informar o CNPJ, fineza verificar!");
            }

            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{cnpjTela});
            if(parceiroVO == null){
                throw new Exception(VariaveisFlow.PARCEIRO_NAO_ENCONTRADO);
            }

            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
        }

        if ( (statusLimite == null || statusLimite.equalsIgnoreCase("") || statusLimite.equalsIgnoreCase("null") || statusLimite.equalsIgnoreCase("1")) &&
                 contextoEvento.getCampo("VLRTOT") != null || contextoEvento.getCampo("VLRDESCTOT") != null ){
            gerarRateioFlow(idInstanciaProcesso, contextoEvento );
        }
    }

    public void gerarRateioFlow(BigDecimal idInstanciaProcesso, ContextoEvento contextoEvento ) throws Exception {

        BigDecimal paramUnidade = null;
        BigDecimal paramCodigoProjeto = null;
        BigDecimal paramCentroResultado = null;
        BigDecimal paramCodigoNatureza = null;
        BigDecimal valorDesconto = contextoEvento.getCampo("VLRDESCTOT") == null ? BigDecimal.ZERO : new BigDecimal(contextoEvento.getCampo("VLRDESCTOT").toString());
        BigDecimal paramValorRateio = BigDecimal.ZERO;

        if( valorDesconto == null ){
            valorDesconto = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "VLRDESCTOT").toString());
        }

        if( contextoEvento.getCampo("VLRTOT") != null ){
            paramValorRateio = new BigDecimal(Double.valueOf(contextoEvento.getCampo("VLRTOT").toString()));
        }else{
            paramValorRateio = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "VLRTOT").toString());
        }
        paramValorRateio = paramValorRateio.subtract(valorDesconto);

        if (contextoEvento.getCampo("UNID_FATURAMENTO") == null
                || contextoEvento.getCampo("UNID_FATURAMENTO").toString().equalsIgnoreCase("null")){
            paramUnidade = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "UNID_FATURAMENTO").toString());
        }else {
            paramUnidade = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODSITE").toString()));
        }

        if (contextoEvento.getCampo("CODPROJ") == null
                || contextoEvento.getCampo("CODPROJ").toString().equalsIgnoreCase("null")){
            paramCodigoProjeto = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "CODPROJ").toString());
        }else{
            paramCodigoProjeto = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODPROJ").toString()));
        }

        if (contextoEvento.getCampo("CODCENCUS") == null
                || contextoEvento.getCampo("CODCENCUS").toString().equalsIgnoreCase("null")){
            paramCentroResultado = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "CODCENCUS").toString());
        }else {
            paramCentroResultado = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODCENCUS").toString()));
        }

        if(contextoEvento.getCampo("CODNAT") == null
                || contextoEvento.getCampo("CODNAT").toString().equalsIgnoreCase("null")){
            paramCodigoNatureza = new BigDecimal(VariaveisFlow.getVariavel(idInstanciaProcesso, "CODNAT").toString());
        }else {
            paramCodigoNatureza = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODNAT").toString()));
        }

        DynamicVO contaContabilCRVO = contaContabilCRDAO.findOne("NUCLASSIFICACAO = 1 AND CODNAT = ?", new Object[]{paramCodigoNatureza});

        DynamicVO rateio = rateioCPQ.findOne("IDINSTPRN = ?", new Object[]{idInstanciaProcesso});
        if(rateio != null){
            rateioCPQ.deleteByCriteria("IDINSTPRN = ?", new Object[]{idInstanciaProcesso});
        }

        FluidCreateVO rateioCPQFCVO = rateioCPQ.create();
        rateioCPQFCVO.set("IDINSTPRN", idInstanciaProcesso);
        rateioCPQFCVO.set("IDINSTTAR", BigDecimal.ZERO);
        rateioCPQFCVO.set("CODREGISTRO", new BigDecimal(1));
        rateioCPQFCVO.set("IDTAREFA", new BigDecimal(0).toString());
        rateioCPQFCVO.set("VLRRATEIO", paramValorRateio);
        rateioCPQFCVO.set("PERCRATEIO", BigDecimal.valueOf(100));
        rateioCPQFCVO.set("CODPROJ", paramCodigoProjeto);
        rateioCPQFCVO.set("CODCTACTB", contaContabilCRVO.asBigDecimal("CODCTACTB"));
        rateioCPQFCVO.set("CODSITRATEIO", paramUnidade);
        rateioCPQFCVO.set("CODNAT", paramCodigoNatureza);
        rateioCPQFCVO.set("CODCENCUS", paramCentroResultado);
        rateioCPQFCVO.save();
    }
}
