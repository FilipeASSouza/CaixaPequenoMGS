package br.com.flow.tarefa;

import br.com.sankhya.bh.dao.DynamicVOKt;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.util.NativeSqlDecorator;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class CriarLancamentoCentralCompras implements TarefaJava {

    private JapeWrapper cabecalhoNotaDAO = JapeFactory.dao("CabecalhoNota");
    private JapeWrapper itemDAO = JapeFactory.dao("ItemNota");
    private JapeWrapper produtoDAO = JapeFactory.dao("Produto");
    private JapeWrapper rateioDAO = JapeFactory.dao("RateioRecDesp");
    private JapeWrapper centroCustoDAO = JapeFactory.dao("CentroResultado");
    private JapeWrapper contaContabilCRDAO = JapeFactory.dao("TGFNATCCCR");
    private JapeWrapper acessoCaixaPequenoDAO = JapeFactory.dao("AD_ACESSOCAIXAPEQUENO");
    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");

    @Override
    public void executar(ContextoTarefa ct) throws Exception {


    String aprovacao = (String) ct.getCampo("APROVACAO");

    if( aprovacao != null
    && !aprovacao.equals(String.valueOf("1"))){

        // Criando o cabeçalho

        DynamicVO parceiroVO = parceiroDAO.findOne("CGC_CPF =?", new Object[]{ct.getCampo("CNPJ")});
        NativeSqlDecorator tipoNegociacaoDecorator = new NativeSqlDecorator("SELECT MAX(DHALTER) AS DHALTER FROM TGFTPV WHERE CODTIPVENDA = :CODTIPVENDA");
        tipoNegociacaoDecorator.setParametro("CODTIPVENDA", ct.getCampo("TPNEG"));

        BigDecimal numeroUnicoModelo = null;

        if( ct.getCampo("TOPPROD") != null ){
            numeroUnicoModelo = BigDecimal.valueOf(27840L);
        }else{
            numeroUnicoModelo = BigDecimal.valueOf(57857L);
        }

        DynamicVO modeloNotaVO = cabecalhoNotaDAO.findByPK(new Object[]{numeroUnicoModelo});
        Map<String, Object> campos = new HashMap();
        campos.put("NUMNOTA", BigDecimal.valueOf(Long.parseLong((String) ct.getCampo("NUMNOTA"))));
        campos.put("NUMCONTRATO", BigDecimal.ZERO);
        campos.put("CODEMP", BigDecimal.ONE);
        campos.put("CODPARC", parceiroVO.asBigDecimal("CODPARC") != null ? parceiroVO.asBigDecimal("CODPARC") : BigDecimal.ONE );
        campos.put("CODCENCUS", BigDecimal.valueOf(Long.parseLong((String) ct.getCampo("CODCENCUS"))));
        campos.put("CODNAT", BigDecimal.valueOf(Long.parseLong((String) ct.getCampo("CODNAT"))));
        campos.put("SERIENOTA", ct.getCampo("SERIENOTA"));
        campos.put("DTENTSAI", ct.getCampo("DTENTRCONT"));
        campos.put("DTNEG", ct.getCampo("DTMOV"));
        campos.put("DTFATUR", ct.getCampo("DTFATEM"));
        campos.put("DTMOV", TimeUtils.getNow());
        campos.put("CODPROJ", BigDecimal.valueOf(99990001));
        campos.put("OBSERVACAO", ct.getCampo("OBS") +" - Justificativa: "+ ct.getCampo("JSTCOMPR"));
        campos.put("CODUSU", AuthenticationInfo.getCurrent().getUserID());
        campos.put("CODUSUINC", AuthenticationInfo.getCurrent().getUserID());
        BigDecimal quantidade = BigDecimal.valueOf(Long.parseLong(String.valueOf(ct.getCampo("QTDNEG"))));
        Double valorUnitario = (Double) ct.getCampo("VLRUNIT");
        campos.put("VLRNOTA", quantidade.multiply(BigDecimal.valueOf(valorUnitario)));
        campos.put("STATUSNOTA", String.valueOf("A"));
        DynamicVO notaDestino = DynamicVOKt.duplicaRegistro(modeloNotaVO, campos);
        BigDecimal numeroUnicoNota = notaDestino.asBigDecimal("NUNOTA");

        // Criando os itens

        FluidCreateVO itemFCVO = itemDAO.create();
        itemFCVO.set("NUNOTA", numeroUnicoNota );
        itemFCVO.set("CODPROD", BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("CODPROD"))));
        itemFCVO.set("QTDNEG", BigDecimal.valueOf((Long) ct.getCampo("QTDNEG")));
        DynamicVO produtoVO = produtoDAO.findByPK(BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("CODPROD"))));
        itemFCVO.set("CODVOL", produtoVO.asString("CODVOL"));
        itemFCVO.set("ATUALESTOQUE", BigDecimal.ZERO);
        itemFCVO.set("STATUSNOTA", "A");
        itemFCVO.set("USOPROD", produtoVO.asString("USOPROD"));
        itemFCVO.set("VLRUNIT", BigDecimal.valueOf((Double) ct.getCampo("VLRUNIT")));
        itemFCVO.set("VLRTOT", quantidade.multiply(BigDecimal.valueOf(valorUnitario)));
        itemFCVO.set("RESERVA", "N");
        itemFCVO.set("CODLOCALORIG", produtoVO.asBigDecimal("CODLOCALPADRAO"));
        itemFCVO.save();

        // Criando o Rateio
        FluidCreateVO rateioFCVO = rateioDAO.create();
        rateioFCVO.set("ORIGEM", String.valueOf("E"));
        rateioFCVO.set("NUFIN", numeroUnicoNota );
        rateioFCVO.set("CODNAT", BigDecimal.valueOf( Long.parseLong( (String) ct.getCampo("CODNAT"))));
        rateioFCVO.set("CODCENCUS", BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("CODCENCUS"))));
        rateioFCVO.set("CODPROJ", BigDecimal.valueOf(99990001));
        DynamicVO acessoCaixaPequenoVO = acessoCaixaPequenoDAO.findOne("CODUSU = ?", new Object[]{AuthenticationInfo.getCurrent().getUserID()});
        rateioFCVO.set("CODSITE", acessoCaixaPequenoVO.asBigDecimal("CODSITE"));
        rateioFCVO.set("PERCRATEIO", BigDecimal.valueOf(100));

        DynamicVO centroCustoVO = centroCustoDAO.findByPK(ct.getCampo("CODCENCUS"));
        DynamicVO contaContabilCRVO = contaContabilCRDAO.findOne("NUCLASSIFICACAO = ?", new Object[]{centroCustoVO.asBigDecimal("AD_NUCLASSIFICACAO")});
        rateioFCVO.set("CODCTACTB", contaContabilCRVO.asBigDecimal("CODCTACTB") );
        rateioFCVO.set("NUMCONTRATO", BigDecimal.ZERO );
        rateioFCVO.set("CODPARC", notaDestino.asBigDecimal("CODPARC"));
        rateioFCVO.save();
        }
    }
}
