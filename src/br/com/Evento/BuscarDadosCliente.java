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

    public BuscarDadosCliente() {
    }

    public void executar(ContextoEvento contextoEvento) throws Exception {

        Object codigoLotacao = contextoEvento.getCampo("COD_LOTACAO");
        Object cnpj = contextoEvento.getCampo("CNPJ");
        String cnpjTela = null;
        BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
        BigDecimal idInstanciaTarefa = new BigDecimal(0L);
        BigDecimal codigoUsuario = new BigDecimal(contextoEvento.getIObjectInstanciaProcesso().get("CODUSUINC").toString());

        String unidade = VariaveisFlow.getVariavel(idInstanciaProcesso,"UNID_FATURAMENTO").toString();

        QueryExecutor consultarDadosContrato = contextoEvento.getQuery();
        consultarDadosContrato.setParam("CODUSU", codigoUsuario);
        consultarDadosContrato.setParam("CODSIT", unidade);
        consultarDadosContrato.nativeSelect("SELECT U.ID ID_USUARIO,\n" +
                "CTT.CODUSU, \n" +
                "SUBSTR(UU.COD_UNIDADE1, 4, 3)|| \n" +
                "SUBSTR(UU.COD_UNIDADE2, 4, 3)|| \n" +
                "SUBSTR(UU.COD_UNIDADE3, 4, 3) UNIDADE, \n" +
                "UU.NUM_CONTRATO, \n" +
                "LOT.CODLOT \n" +
                "FROM PORTALCLIENTE.USUARIO@DLINK_MGS U \n" +
                "INNER JOIN WEB.USUARIO_UNIDADE@DLINK_MGS UU  ON (U.LOGIN = UU.LOGIN) \n" +
                "INNER JOIN TGFCTT CTT ON CTT.CODPARC = 9999 AND CTT.AD_USUARIOPORTAL = U.ID \n" +
                "LEFT JOIN MGSTCTCONTRATO ON MGSTCTCONTRATO.NUMCONTRATO = UU.NUM_CONTRATO + 0 \n" +
                "LEFT JOIN AD_TGFLOT LOT ON LOT.CODSITE = ( SUBSTR(UU.COD_UNIDADE1, 4, 3)|| \n" +
                "SUBSTR(UU.COD_UNIDADE2, 4, 3)|| \n" +
                "SUBSTR(UU.COD_UNIDADE3, 4, 3) ) \n" +
                "WHERE CTT.CODUSU = {CODUSU} \n" +
                "AND (SUBSTR(UU.COD_UNIDADE1, 4, 3)||SUBSTR(UU.COD_UNIDADE2, 4, 3)||SUBSTR(UU.COD_UNIDADE3, 4, 3)) = {CODSIT}");
        if (consultarDadosContrato.next()){
            //VariaveisFlow.setVariavel(idInstanciaProcesso, BigDecimal.ZERO,"COD_LOTACAO", consultarDadosContrato.getString("CODLOT"));

            Object tipoper = VariaveisFlow.getVariavel(idInstanciaProcesso, "TIPOPER");
            if(tipoper.toString().equalsIgnoreCase("S")){
                VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", consultarDadosContrato.getString("NUM_CONTRATO"));
            }
        }
//        else if (!consultarDadosContrato.next()){
//            ErroUtils.disparaErro("Unidade de faturamento e contrato não localizado para o Usuário, fineza verificar com COCOP!");
//        }
        consultarDadosContrato.close();

        if( cnpj != null ) {
            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{String.valueOf(cnpj)});
            if(parceiroVO == null){
                throw new Exception("Parceiro não foi localizado, fineza entrar em contato com o setor financeiro!");
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

        String statusLimite = VariaveisFlow.getVariavel(idInstanciaProcesso, "STATUSLIMITE") == null ? ""
                : VariaveisFlow.getVariavel(idInstanciaProcesso, "STATUSLIMITE").toString();

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
