package br.com.Evento;

import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.flow.ContextoEvento;
import br.com.sankhya.extensions.flow.EventoProcessoJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;
import java.util.Map;

public class BuscarDadosCliente implements EventoProcessoJava {

    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");

    public BuscarDadosCliente() {
    }

    public void executar(ContextoEvento contextoEvento) throws Exception {

        Object codigoLotacao = contextoEvento.getCampo("COD_LOTACAO");
        Object cnpj = contextoEvento.getCampo("CNPJ");
        String cnpjTela = null;
        BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
        BigDecimal idInstanciaTarefa = new BigDecimal(0);
        JapeWrapper contaContabilCRDAO = JapeFactory.dao("TGFNATCCCR");

        if (codigoLotacao != null && cnpj != null) {

            NativeSqlDecorator nativeSqlDadosCliente = new NativeSqlDecorator("SELECT AD_NUMCONTRATO FROM AD_TGFLOT LOT LEFT JOIN TGFSIT SIT ON SIT.CODSITE = LOT.CODSITE WHERE CODLOT = :codigoLotacao");
            nativeSqlDadosCliente.setParametro("codigoLotacao", codigoLotacao.toString());
            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{String.valueOf(cnpj)});
            if (!nativeSqlDadosCliente.proximo()) {
                throw new Exception("Não existe um Contrato vinculado a Unidade/ Lotacao " + codigoLotacao.toString() +" ,fineza verificar!");
            }

            if(parceiroVO == null){
                throw new Exception("Parceiro não foi localizado, fineza entrar em contato com o setor financeiro!");
            }

            BigDecimal ad_numcontrato = nativeSqlDadosCliente.getValorBigDecimal("AD_NUMCONTRATO");
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", String.valueOf(ad_numcontrato));
        }else if( cnpj != null ) {
            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{String.valueOf(cnpj)});
            if(parceiroVO == null){
                throw new Exception("Parceiro não foi localizado, fineza entrar em contato com o setor financeiro!");
            }
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
        }else if(codigoLotacao != null){
            NativeSqlDecorator verificarCNPJSQL = new NativeSqlDecorator("SELECT TEXTO FROM TWFIVAR WHERE IDINSTPRN = :IDINSTPRN AND NOME = 'CNPJ'");
            verificarCNPJSQL.setParametro("IDINSTPRN", contextoEvento.getIdInstanceProcesso());
            if( verificarCNPJSQL.proximo()){
                cnpjTela = verificarCNPJSQL.getValorString("TEXTO");
            }
            NativeSqlDecorator nativeSqlDadosCliente = new NativeSqlDecorator("SELECT AD_NUMCONTRATO FROM AD_TGFLOT LOT LEFT JOIN TGFSIT SIT ON SIT.CODSITE = LOT.CODSITE WHERE CODLOT = :codigoLotacao");
            nativeSqlDadosCliente.setParametro("codigoLotacao", codigoLotacao.toString());

            if(cnpjTela == null){
                throw new Exception("Necessario informar o CNPJ, fineza verificar!");
            }

            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{cnpjTela});
            if(!nativeSqlDadosCliente.proximo()) {
                throw new Exception("Não existe um Contrato vinculado a Unidade/ Lotacao " + codigoLotacao.toString() +" ,fineza verificar!");
            }

            if(parceiroVO == null){
                throw new Exception("Parceiro não foi localizado, fineza entrar em contato com o setor financeiro!");
            }

            BigDecimal ad_numcontrato = nativeSqlDadosCliente.getValorBigDecimal("AD_NUMCONTRATO");
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", String.valueOf(ad_numcontrato));
        }

        if (contextoEvento.getCampo("VLRTOT") != null){
            gerarRateioFlow(codigoLotacao, idInstanciaProcesso, idInstanciaTarefa, contextoEvento, contaContabilCRDAO);
        }
    }

    public void gerarRateioFlow(Object codigoLotacaoEvento, BigDecimal idInstanciaProcesso, BigDecimal idInstanciaTarefa, ContextoEvento contextoEvento, JapeWrapper contaContabil ) throws Exception {

        BigDecimal paramUnidade = null;
        BigDecimal paramCodigoProjeto = null;
        BigDecimal paramCentroResultado = null;
        BigDecimal paramCodigoLotacao = null;
        BigDecimal paramCodigoNatureza = null;
        BigDecimal valorDesconto = new BigDecimal(Double.valueOf(contextoEvento.getCampo("VLRDESCTOT").toString()));
        BigDecimal paramValorRateio = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("VLRTOT").toString())).subtract(valorDesconto);

        if (codigoLotacaoEvento == null || codigoLotacaoEvento.toString().equalsIgnoreCase("null")) {
            QueryExecutor consultaCodigoLotacao = contextoEvento.getQuery();
            consultaCodigoLotacao.setParam("IDINSTPRN", idInstanciaProcesso);
            consultaCodigoLotacao.nativeSelect("SELECT TEXTO FROM TWFIVAR WHERE IDINSTPRN = {IDINSTPRN} AND NOME = 'COD_LOTACAO'");
            if (consultaCodigoLotacao.next()) {
                paramCodigoLotacao = consultaCodigoLotacao.getBigDecimal("TEXTO");
            }
            consultaCodigoLotacao.close();
        } else {
            paramCodigoLotacao = BigDecimal.valueOf(Long.parseLong(codigoLotacaoEvento.toString()));
        }

        if (contextoEvento.getCampo("UNID_FATURAMENTO") == null
                || contextoEvento.getCampo("UNID_FATURAMENTO").toString().equalsIgnoreCase("null")){
            if (contextoEvento.getCampo("CODSITRATEIO") == null){
                QueryExecutor consultaUnidadeFaturamento = contextoEvento.getQuery();
                consultaUnidadeFaturamento.setParam("CODLOT", paramCodigoLotacao);
                consultaUnidadeFaturamento.nativeSelect("SELECT LOT.CODSITE FROM AD_TGFLOT LOT WHERE CODLOT = {CODLOT}");
                if (consultaUnidadeFaturamento.next()) {
                    paramUnidade = consultaUnidadeFaturamento.getBigDecimal("CODSITE");
                }
                consultaUnidadeFaturamento.close();
            }else {
                paramUnidade = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODSITRATEIO").toString()));
            }
        }else {
            paramUnidade = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODSITE").toString()));
        }

        if (contextoEvento.getCampo("CODPROJ") == null
                || contextoEvento.getCampo("CODPROJ").toString().equalsIgnoreCase("null")){
            if (contextoEvento.getCampo("CODPROJRATEIO") == null){
                QueryExecutor consultaProjeto = contextoEvento.getQuery();
                consultaProjeto.setParam("IDINSTPRN", idInstanciaProcesso);
                consultaProjeto.nativeSelect("SELECT TEXTO FROM TWFIVAR WHERE IDINSTPRN = {IDINSTPRN} AND NOME = 'CODPROJ'");
                if (consultaProjeto.next()) {
                    paramCodigoProjeto = consultaProjeto.getBigDecimal("TEXTO");
                }
                consultaProjeto.close();
            }else {
                paramCodigoProjeto = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODPROJRATEIO").toString()));
            }
        }else{
            paramCodigoProjeto = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODPROJ").toString()));
        }

        if (contextoEvento.getCampo("CODCENCUS") == null
                || contextoEvento.getCampo("CODCENCUS").toString().equalsIgnoreCase("null")){
            if (contextoEvento.getCampo("CENTRORESULTADORAT") == null){
                QueryExecutor consultaCentroResultado = contextoEvento.getQuery();
                consultaCentroResultado.setParam("IDINSTPRN", idInstanciaProcesso);
                consultaCentroResultado.nativeSelect("SELECT NUMINT FROM TWFIVAR WHERE IDINSTPRN = {IDINSTPRN} AND NOME = 'CODCENCUS'");
                if(consultaCentroResultado.next()){
                    paramCentroResultado = consultaCentroResultado.getBigDecimal("NUMINT");
                }
                consultaCentroResultado.close();
            }else {
                paramCentroResultado = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CENTRORESULTADORAT").toString()));
            }
        }else {
            paramCentroResultado = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODCENCUS").toString()));
        }

        if(contextoEvento.getCampo("CODNAT") == null
                || contextoEvento.getCampo("CODNAT").toString().equalsIgnoreCase("null")){
            if(contextoEvento.getCampo("NATUREZARAT") == null){
                QueryExecutor consultaCodigoNatureza = contextoEvento.getQuery();
                consultaCodigoNatureza.setParam("IDINSTPRN", idInstanciaProcesso);
                consultaCodigoNatureza.nativeSelect("SELECT TEXTO FROM TWFIVAR WHERE IDINSTPRN = {IDINSTPRN} AND NOME = 'CODNAT'");
                if (consultaCodigoNatureza.next()){
                    paramCodigoNatureza = consultaCodigoNatureza.getBigDecimal("TEXTO");
                }
                consultaCodigoNatureza.close();
            }else {
                paramCodigoNatureza = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("NATUREZARAT").toString()));
            }
        }else {
            paramCodigoNatureza = BigDecimal.valueOf(Long.parseLong(contextoEvento.getCampo("CODNAT").toString()));
        }

        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "VLRRATEIO", paramValorRateio);
        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PERCRATEIO", BigDecimal.valueOf(100));
        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "CENTRORESULTADORAT", paramCentroResultado.toString());
        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NATUREZARAT", paramCodigoNatureza.toString());
        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "CODPROJRATEIO", paramCodigoProjeto.toString());
        DynamicVO contaContabilCRVO = contaContabil.findOne("NUCLASSIFICACAO = 1 AND CODNAT = ?", new Object[]{paramCodigoNatureza});
        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "CODCTARATEIO", contaContabilCRVO.asBigDecimal("CODCTACTB").toString());
        VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "CODSITRATEIO", paramUnidade.toString());

        contextoEvento.getQuery().close();
    }
}
