package br.com.Evento;

import br.com.sankhya.extensions.flow.ContextoEvento;
import br.com.sankhya.extensions.flow.EventoProcessoJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;

public class BuscarDadosCliente implements EventoProcessoJava {

    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");

    public BuscarDadosCliente() {
    }

    public void executar(ContextoEvento contextoEvento) throws Exception {

        Object codigoLotacao = contextoEvento.getCampo("COD_LOTACAO");
        Object cnpj = contextoEvento.getCampo("CNPJ");
        String cnpjTela = null;

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
            BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
            BigDecimal idInstanciaTarefa = new BigDecimal(0);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", String.valueOf(ad_numcontrato));
        }else if( cnpj != null ) {
            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{String.valueOf(cnpj)});
            if(parceiroVO == null){
                throw new Exception("Parceiro não foi localizado, fineza entrar em contato com o setor financeiro!");
            }
            BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
            BigDecimal idInstanciaTarefa = new BigDecimal(0);
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
            BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
            BigDecimal idInstanciaTarefa = new BigDecimal(0);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", String.valueOf(ad_numcontrato));
        }
    }
}
