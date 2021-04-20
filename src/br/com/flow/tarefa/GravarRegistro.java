package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.util.BuscarDadosUsuarioLogado;
import br.com.util.ErroUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class GravarRegistro implements TarefaJava {

    public void executar(ContextoTarefa contextoTarefa) throws Exception {

        BuscarDadosUsuarioLogado buscarDadosUsuarioLogado = new BuscarDadosUsuarioLogado();
        BigDecimal usuarioLogado = contextoTarefa.getUsuarioLogado();
        //String emailUsuarioLogado = String.valueOf(buscarDadosUsuarioLogado.BuscarEmailUsuarioLogado(usuarioLogado));
        String numnota = String.valueOf(contextoTarefa.getCampo("NUMNOTA"));
        String codemp = String.valueOf(contextoTarefa.getCampo("CODEMP"));
        String numcontr = String.valueOf(contextoTarefa.getCampo("NUMCONTR"));
        String tpneg = String.valueOf(contextoTarefa.getCampo("TPNEG"));
        String codlot = String.valueOf(contextoTarefa.getCampo("CODLOT"));
        String codnat = String.valueOf(contextoTarefa.getCampo("CODNAT"));
        String codproj = String.valueOf(contextoTarefa.getCampo("CODPROJ"));
        String serienota = String.valueOf(contextoTarefa.getCampo("SERIENOTA"));
        Timestamp dtfatem = Timestamp.valueOf(String.valueOf(contextoTarefa.getCampo("DTFATEM")));
        Timestamp dtentrcont = Timestamp.valueOf(String.valueOf(contextoTarefa.getCampo("DTENTRCONT")));
        Timestamp dtmov = Timestamp.valueOf(String.valueOf(contextoTarefa.getCampo("DTMOV")));

        String obs = String.valueOf(contextoTarefa.getCampo("OBS"));
        String topserv = String.valueOf(contextoTarefa.getCampo("TOPSERV"));
        String topprod = String.valueOf(contextoTarefa.getCampo("TOPPROD"));
        String tipproc = String.valueOf(contextoTarefa.getCampo("TIPPROC"));
        String codprod = String.valueOf(contextoTarefa.getCampo("CODPROD"));
        String qtdneg = String.valueOf(contextoTarefa.getCampo("QTDNEG"));
        String cnpj = String.valueOf(contextoTarefa.getCampo("CNPJ"));
        String jstcompr = String.valueOf(contextoTarefa.getCampo("JSTCOMPR"));
        String vlrestpro = String.valueOf(contextoTarefa.getCampo("VLRESTPRO"));
        String codparc = String.valueOf(contextoTarefa.getCampo("CODPARC"));
        String vlrunit = String.valueOf(contextoTarefa.getCampo("VLRUNIT"));
        String vlrtot = String.valueOf(contextoTarefa.getCampo("VLRTOT"));
        String codcencus = String.valueOf(contextoTarefa.getCampo("CODCENCUS"));

        if (vlrestpro.equalsIgnoreCase(String.valueOf("null"))) {
            vlrestpro = String.valueOf("0");
        }

        JapeWrapper fincaixapqFCVO = JapeFactory.dao("AD_FINCAIXAPQ");
        FluidCreateVO fluidCreateVO = fincaixapqFCVO.create();

        fluidCreateVO.set("IDINSTPRN", new BigDecimal(contextoTarefa.getIdInstanceProcesso().toString()));
        fluidCreateVO.set("NUMNOTA", new BigDecimal(numnota));
        fluidCreateVO.set("CODEMP", new BigDecimal(codemp));
        fluidCreateVO.set("TPNEG", new BigDecimal(tpneg));
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
        //fluidCreateVO.set("EMAILUSU", String.valueOf(emailUsuarioLogado));
        fluidCreateVO.save();

    }
}
