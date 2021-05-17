package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.util.ErroUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class GravarRegistro implements TarefaJava {

    public void executar(ContextoTarefa contextoTarefa) throws Exception {

        String numnota = String.valueOf(contextoTarefa.getCampo("NUMNOTA"));
        String codemp = String.valueOf(contextoTarefa.getCampo("CODEMP"));
        String numcontr = String.valueOf(contextoTarefa.getCampo("NUMCONTR"));
        String tpneg = String.valueOf(contextoTarefa.getCampo("TPNEG"));
        String codlot = String.valueOf(contextoTarefa.getCampo("COD_LOTACAO"));
        String codnat = String.valueOf(contextoTarefa.getCampo("CODNAT"));
        String codproj = String.valueOf(contextoTarefa.getCampo("CODPROJ"));
        String serienota = String.valueOf(contextoTarefa.getCampo("SERIENOTA"));
        Object dtfatem = contextoTarefa.getCampo("DTFATEM");
        Object dtentrcont = contextoTarefa.getCampo("DTENTRCONT");
        Object dtmov = contextoTarefa.getCampo("DTMOV");

        String obs = String.valueOf(contextoTarefa.getCampo("OBS"));
        String chavenfe = String.valueOf(contextoTarefa.getCampo("CHAVENFE"));
        String topserv = String.valueOf(contextoTarefa.getCampo("TOPSERV"));
        String topprod = String.valueOf(contextoTarefa.getCampo("TOPPROD"));
        String codprod = String.valueOf(contextoTarefa.getCampo("CODPROD"));
        String qtdneg = String.valueOf(contextoTarefa.getCampo("QTDNEG"));
        String cnpj = String.valueOf(contextoTarefa.getCampo("CNPJ"));
        String jstcompr = String.valueOf(contextoTarefa.getCampo("JSTCOMPR"));
        String vlrestpro = String.valueOf(contextoTarefa.getCampo("VLRESTPRO"));
        String vlrunit = String.valueOf(contextoTarefa.getCampo("VLRUNIT"));
        String vlrtot = String.valueOf(contextoTarefa.getCampo("VLRTOT"));
        String codcencus = String.valueOf(contextoTarefa.getCampo("CODCENCUS"));
        String parceiro = String.valueOf(contextoTarefa.getCampo("PARCEIRO"));

        if (vlrestpro.equalsIgnoreCase(String.valueOf("null"))) {
            vlrestpro = String.valueOf("0");
        }
        if(numnota.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Necessário informar o número da nota");
        }else if(cnpj.length() > 14){
            ErroUtils.disparaErro("CNPJ são apenas números, fineza verificar!");
        }else if(cnpj.equalsIgnoreCase(String.valueOf("null"))) {
            ErroUtils.disparaErro("Necessário informar o CNPJ, fineza verificar!");
        }else if(parceiro.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Parceiro informado não está cadastrado, fineza verificar com a MGS!");
        }else if(topserv == null
                && chavenfe.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Chave da nota não foi informada, fineza verificar!");
        }else if(topserv == null
            && chavenfe.length() < 44){
            ErroUtils.disparaErro("Chave da nota informada incorretamente, fineza verificar!");
        }else if(codlot.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Lotação não foi informada, fineza verificar!");
        }else if(codnat.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Natureza não foi informada, fineza verificar!");
        }else if(serienota.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Serie da nota não foi informada, fineza verificar!");
        }else if(tpneg.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Tipo de negociação não foi informada, fineza verificar!");
        }else if(codcencus.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Centro de custo não foi informado, fineza verificar!");
        }else if(dtfatem == null){
            ErroUtils.disparaErro("Data de faturamento não foi informado, fineza verificar!");
        }else if(dtentrcont == null){
            ErroUtils.disparaErro("Data contabil de entrada e saida não foi informado, fineza verificar!");
        }else if(dtmov == null){
            ErroUtils.disparaErro("Data de movimento não foi informado, fineza verificar!");
        }

        JapeWrapper fincaixapqFCVO = JapeFactory.dao("AD_FINCAIXAPQ");
        FluidCreateVO fluidCreateVO = fincaixapqFCVO.create();

        fluidCreateVO.set("IDINSTPRN", new BigDecimal(contextoTarefa.getIdInstanceProcesso().toString()));
        fluidCreateVO.set("NUMNOTA", new BigDecimal(numnota));
        fluidCreateVO.set("CODEMP", new BigDecimal(codemp));
        fluidCreateVO.set("NUMCONTR", new BigDecimal(Long.parseLong(numcontr)) );
        fluidCreateVO.set("TPNEG", new BigDecimal(tpneg));
        fluidCreateVO.set("CODLOT", new BigDecimal(Long.parseLong(codlot)));
        fluidCreateVO.set("CODNAT", new BigDecimal(codnat));
        fluidCreateVO.set("CODPROJ", new BigDecimal(codproj));
        fluidCreateVO.set("SERIENOTA", serienota);
        fluidCreateVO.set("DTFATEM", Timestamp.valueOf(String.valueOf(dtfatem)));
        fluidCreateVO.set("DTENTRCONT", Timestamp.valueOf(String.valueOf(dtentrcont)));
        fluidCreateVO.set("DTMOV", Timestamp.valueOf(String.valueOf(dtmov)));
        fluidCreateVO.set("OBS", obs);

        if (topprod != String.valueOf("null")) {
            fluidCreateVO.set("TOPPROD", new BigDecimal(topprod));
        } else {
            fluidCreateVO.set("TOPSERV", new BigDecimal(topserv));
        }

        fluidCreateVO.set("CODPROD", new BigDecimal(codprod));
        fluidCreateVO.set("QTDNEG", new BigDecimal(qtdneg));
        fluidCreateVO.set("JSTCOMPR", jstcompr);
        fluidCreateVO.set("VLRESTPRO", new BigDecimal(vlrestpro));
        fluidCreateVO.set("VLRUNIT", new BigDecimal(vlrunit));
        fluidCreateVO.set("VLRTOT", new BigDecimal(vlrtot));
        fluidCreateVO.set("CODCENCUS", new BigDecimal(codcencus));
        fluidCreateVO.set("CNPJ", cnpj);
        fluidCreateVO.set("CHAVENFE", chavenfe);
        fluidCreateVO.save();

    }
}
