package br.com.Evento;

import br.com.sankhya.extensions.flow.ContextoEvento;
import br.com.sankhya.extensions.flow.EventoProcessoJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.util.ErroUtils;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;

public class BuscarDadosCliente implements EventoProcessoJava {
    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");

    public BuscarDadosCliente() {
    }

    public void executar(ContextoEvento contextoEvento) throws Exception {

        if(contextoEvento.getCampo("CNPJ") == null
            && contextoEvento.getCampo("CNPJ") != "null" ){
            ErroUtils.disparaErro("CNPJ está vazio precisa ser informado, fineza verificar!");
        }

        Object codigoLotacao = contextoEvento.getCampo("COD_LOTACAO");
        Object cnpj = contextoEvento.getCampo("CNPJ");

        if (codigoLotacao != null) {

            NativeSqlDecorator nativeSqlDadosCliente = new NativeSqlDecorator("SELECT * FROM VIEW_USUARIOSCP WHERE CODLOT = :codigoLotacao");
            nativeSqlDadosCliente.setParametro("codigoLotacao", codigoLotacao.toString());
            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{String.valueOf(cnpj)});
            if (!nativeSqlDadosCliente.proximo()) {
                throw new Exception("Parceiro e/ou nao do contrato nao encontrados na lotacao " + codigoLotacao.toString());
            }
            BigDecimal ad_numcontrato = nativeSqlDadosCliente.getValorBigDecimal("AD_NUMCONTRATO");
            BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
            BigDecimal idInstanciaTarefa = new BigDecimal(0);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "NUMCONTR", String.valueOf(ad_numcontrato));
        }else{
            DynamicVO parceiroVO = this.parceiroDAO.findOne("CGC_CPF = ?", new Object[]{String.valueOf(cnpj)});
            BigDecimal idInstanciaProcesso = new BigDecimal(contextoEvento.getIdInstanceProcesso().toString());
            BigDecimal idInstanciaTarefa = new BigDecimal(0);
            VariaveisFlow.setVariavel(idInstanciaProcesso, idInstanciaTarefa, "PARCEIRO", String.valueOf(parceiroVO.asString("RAZAOSOCIAL")));
        }
    }
}
