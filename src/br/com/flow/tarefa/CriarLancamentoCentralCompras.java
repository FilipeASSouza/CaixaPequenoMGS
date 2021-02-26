package br.com.flow.tarefa;

import br.com.util.NativeSqlDecorator;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class CriarLancamentoCentralCompras implements TarefaJava {

    private JapeWrapper cabecalhoDAO = JapeFactory.dao("CabecalhoNota");
    private JapeWrapper itemDAO = JapeFactory.dao("ItemNota");
    private JapeWrapper tipoOperacaoDAO = JapeFactory.dao("TipoOperacao");
    private JapeWrapper produtoDAO = JapeFactory.dao("Produto");
    private JapeWrapper rateioDAO = JapeFactory.dao("RateioRecDesp");
    private JapeWrapper centroCustoDAO = JapeFactory.dao("CentroResultado");
    private JapeWrapper contaContabilCRDAO = JapeFactory.dao("TGFNATCCCR");
    private JapeWrapper acessoCaixaPequenoDAO = JapeFactory.dao("AD_ACESSOCAIXAPEQUENO");
    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
    private Timestamp dhTipVenda;

    @Override
    public void executar(ContextoTarefa ct) throws Exception {

        // Criando o cabeçalho

        FluidCreateVO cabecalhoFCVO = cabecalhoDAO.create();
        cabecalhoFCVO.set("NUMNOTA", BigDecimal.ZERO);
        cabecalhoFCVO.set("NUMCONTRATO", BigDecimal.ZERO);
        cabecalhoFCVO.set("CODEMP", BigDecimal.ONE);

        DynamicVO parceiroVO = parceiroDAO.findOne("CGC_CPF =?", new Object[]{ct.getCampo("CNPJ")});

        if(parceiroVO.asBigDecimal("CODPARC") != null ){
            cabecalhoFCVO.set("CODPARC", ct.getCampo("CODPARC"));
        }else{
            cabecalhoFCVO.set("CODPARC", BigDecimal.ONE);
        }
        if(ct.getCampo("TIPPROC").equals(String.valueOf("S"))){
            NativeSqlDecorator tipoOperacaoSqlDecorator = new NativeSqlDecorator("SELECT MAX(DHALTER) AS DHALTER, TIPMOV FROM TGFTOP WHERE CODTIPOPER = :CODTIPOPER GROUP BY TIPMOV");
            tipoOperacaoSqlDecorator.setParametro("CODTIPOPER", ct.getCampo("TOPSERV"));
            if( tipoOperacaoSqlDecorator.proximo() ){
                Timestamp dhtipoper = tipoOperacaoSqlDecorator.getValorTimestamp("DHALTER");
                cabecalhoFCVO.set("CODTIPOPER", ct.getCampo("TOPSERV"));
                cabecalhoFCVO.set("DHTIPOPER", dhtipoper );
                cabecalhoFCVO.set("TIPMOV", tipoOperacaoSqlDecorator.getValorString("TIPMOV"));
            }
        }else{
            NativeSqlDecorator tipoOperacaoSqlDecorator = new NativeSqlDecorator("SELECT MAX(DHALTER) AS DHALTER, TIPMOV FROM TGFTOP WHERE CODTIPOPER = :CODTIPOPER GROUP BY TIPMOV");
            tipoOperacaoSqlDecorator.setParametro("CODTIPOPER", ct.getCampo("TOPPROD"));
            if( tipoOperacaoSqlDecorator.proximo() ) {
                Timestamp dhtipoper = tipoOperacaoSqlDecorator.getValorTimestamp("DHALTER");
                cabecalhoFCVO.set("CODTIPOPER", ct.getCampo("TOPPROD") );
                cabecalhoFCVO.set("DHTIPOPER", dhtipoper );
                cabecalhoFCVO.set("TIPMOV", tipoOperacaoSqlDecorator.getValorString("TIPMOV"));
            }
        }

        NativeSqlDecorator tipoNegociacaoDecorator = new NativeSqlDecorator("SELECT MAX(DHALTER) AS DHALTER FROM TGFTPV WHERE CODTIPVENDA = :CODTIPVENDA");
        tipoNegociacaoDecorator.setParametro("CODTIPVENDA", ct.getCampo("TPNEG"));
        if( tipoNegociacaoDecorator.proximo() ){
            dhTipVenda = tipoNegociacaoDecorator.getValorTimestamp("DHALTER");
        }

        cabecalhoFCVO.set("CODTIPVENDA", ct.getCampo("TPNEG"));
        cabecalhoFCVO.set("DHTIPVENDA", dhTipVenda );
        cabecalhoFCVO.set("AD_CODLOT", ct.getCampo("CODLOT"));
        cabecalhoFCVO.set("CODCENCUS", ct.getCampo("CODCENCUS"));
        cabecalhoFCVO.set("CODNAT", ct.getCampo("CODNAT"));
        cabecalhoFCVO.set("CODPROJ", BigDecimal.valueOf(99990001L));
        cabecalhoFCVO.set("SERIENOTA", ct.getCampo("SERIENOTA"));
        cabecalhoFCVO.set("DTFAT", ct.getCampo("DTFATEM"));
        cabecalhoFCVO.set("DTENTSAI", ct.getCampo("DTENTRCONT")); // Data Contabil
        cabecalhoFCVO.set("DTNEG", TimeUtils.getNow());
        cabecalhoFCVO.set("DTALTER", TimeUtils.getNow());
        cabecalhoFCVO.set("DTMOV", ct.getCampo("DTENTRCONT"));
        cabecalhoFCVO.set("CODUSU", AuthenticationInfo.getCurrent().getUserID());
        cabecalhoFCVO.set("CODUSUINC", AuthenticationInfo.getCurrent().getUserID());
        cabecalhoFCVO.set("RATEADO", String.valueOf("S"));
        cabecalhoFCVO.set("CODVEND", BigDecimal.ZERO );
        cabecalhoFCVO.set("COMISSAO", BigDecimal.ZERO );
        cabecalhoFCVO.set("CODMOEDA", BigDecimal.ZERO );
        cabecalhoFCVO.set("OBSERVACAO", ct.getCampo("OBS") +" - Justificativa: "+ ct.getCampo("JSTCOMPR"));
        cabecalhoFCVO.set("VLRSEG", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLRICMSSEG", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLRDESTAQUE", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLRDESTAQUE", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLRJURO", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLRVENDOR", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLROUTROS", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLREMP", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLRICMSEMB", BigDecimal.ZERO );
        cabecalhoFCVO.set("VLRDESCSERV", BigDecimal.ZERO );
        cabecalhoFCVO.set("IPIEMB", BigDecimal.ZERO );
        cabecalhoFCVO.set("TIPIPIEMB", String.valueOf("N") );
        cabecalhoFCVO.set("TIPFRETE", String.valueOf("N") );
        cabecalhoFCVO.set("CIF_FOB", String.valueOf("F") );

        DynamicVO saveCAB = cabecalhoFCVO.save();

        // Criando os itens

        FluidCreateVO itemFCVO = itemDAO.create();
        itemFCVO.set("CODPROD", ct.getCampo("CODPROD"));
        itemFCVO.set("NUNOTA", saveCAB.asBigDecimal("NUNOTA"));
        itemFCVO.set("QTDNEG", ct.getCampo("QTDNEG"));

        DynamicVO produtoVO = produtoDAO.findByPK(ct.getCampo("CODPROD"));
        itemFCVO.set("CODVOL", produtoVO.asString("CODVOL"));
        itemFCVO.set("CODLOCALORIG", BigDecimal.ZERO );
        itemFCVO.set("CODEMP", BigDecimal.ONE );
        itemFCVO.set("USOPROD", produtoVO.asString("USOPROD") );
        itemFCVO.set("PERCDESC", BigDecimal.ZERO );
        itemFCVO.set("VLRUNIT", ct.getCampo("VLRUNIT"));
        BigDecimal quantidade = BigDecimal.valueOf(Long.parseLong(String.valueOf(ct.getCampo("QTDNEG"))));
        BigDecimal valorUnitario = BigDecimal.valueOf(Long.parseLong(String.valueOf(ct.getCampo("VLRUNIT"))));
        itemFCVO.set("VLRTOT", valorUnitario.multiply(quantidade) );
        itemFCVO.set("VLRICMS", BigDecimal.ZERO);
        itemFCVO.set("VLRIPI", BigDecimal.ZERO);
        itemFCVO.set("BASEIPI", BigDecimal.ZERO);
        itemFCVO.set("CONTROLE", "");
        itemFCVO.set("ATUALESTOQUE", BigDecimal.ZERO);
        itemFCVO.set("CODUSU", AuthenticationInfo.getCurrent().getUserID());
        itemFCVO.set("DTALTER", TimeUtils.getNow());
        itemFCVO.save();

        // Criando o Rateio
        FluidCreateVO rateioFCVO = rateioDAO.create();
        rateioFCVO.set("ORITEM", String.valueOf("E"));
        rateioFCVO.set("NUFIN", saveCAB.asBigDecimal("NUNOTA"));
        rateioFCVO.set("CODNAT", ct.getCampo("CODNAT")); //139221, 138677, chamado 238642 8509 lumena
        rateioFCVO.set("CODCENCUS", ct.getCampo("CODCENCUS"));
        rateioFCVO.set("CODPROJ", BigDecimal.valueOf(99990001L));
        rateioFCVO.set("PERCRATEIO", BigDecimal.valueOf(100L));

        DynamicVO centroCustoVO = centroCustoDAO.findByPK(ct.getCampo("CODCENCUS"));
        DynamicVO contaContabilCRVO = contaContabilCRDAO.findOne("NUCLASSIFICACAO = ?", new Object[]{centroCustoVO.asBigDecimal("AD_NUCLASSIFICACAO")});
        rateioFCVO.set("CODCTACTB", contaContabilCRVO.asBigDecimal("CODCTACTB") );
        rateioFCVO.set("NUMCONTRATO", BigDecimal.ZERO );
        rateioFCVO.set("DIGITADO", String.valueOf("S"));

        DynamicVO acessoCaixaPequenoVO = acessoCaixaPequenoDAO.findOne("CODUSU = ?", new Object[]{AuthenticationInfo.getCurrent()});
        rateioFCVO.set("CODSITE", acessoCaixaPequenoVO.asBigDecimal("CODSITE"));
        rateioFCVO.set("CODPARC", ct.getCampo("CODPARC"));
        rateioFCVO.set("CODUSU", AuthenticationInfo.getCurrent().getUserID());
        rateioFCVO.set("DTALTER", TimeUtils.getNow());
        rateioFCVO.save();

    }
}
