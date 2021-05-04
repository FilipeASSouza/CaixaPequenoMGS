package br.com.flow.tarefa;

import br.com.sankhya.bh.dao.DynamicVOKt;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class CriarLancamentoCentralCompras implements TarefaJava {

    private JapeWrapper cabecalhoNotaDAO = JapeFactory.dao("CabecalhoNota");
    private JapeWrapper itemDAO = JapeFactory.dao("ItemNota");
    private JapeWrapper produtoDAO = JapeFactory.dao("Produto");
    private JapeWrapper rateioDAO = JapeFactory.dao("RateioRecDesp");
    private JapeWrapper centroCustoDAO = JapeFactory.dao("CentroResultado");
    private JapeWrapper contaContabilCRDAO = JapeFactory.dao("TGFNATCCCR");
    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");
    private BigDecimal unidade = null;
    private Timestamp datatipoNegociacao = null;

    @Override
    public void executar(ContextoTarefa ct) throws Exception {

    String aprovacao = (String) ct.getCampo("APROVACAO");
    BigDecimal codigoAprovador = ct.getUsuarioLogado();
    BigDecimal numeroNota = BigDecimal.valueOf(Long.parseLong((String) ct.getCampo("NUMNOTA")));

    if( aprovacao != null
    && !aprovacao.equals(String.valueOf("1"))){

        // Criando o cabeçalho

        DynamicVO parceiroVO = parceiroDAO.findOne("CGC_CPF =?", new Object[]{ct.getCampo("CNPJ")});
        NativeSqlDecorator tipoNegociacaoDecorator = new NativeSqlDecorator("SELECT MAX(DHALTER) AS DHALTER FROM TGFTPV WHERE CODTIPVENDA = :CODTIPVENDA");
        tipoNegociacaoDecorator.setParametro("CODTIPVENDA", ct.getCampo("TPNEG") );

        BigDecimal numeroUnicoModelo = null;

        if( tipoNegociacaoDecorator.proximo() ){
            datatipoNegociacao = tipoNegociacaoDecorator.getValorTimestamp("DHALTER");
        }

        if( ct.getCampo("TOPPROD") != null ){
            numeroUnicoModelo = BigDecimal.valueOf(27840L);
        }else{
            numeroUnicoModelo = BigDecimal.valueOf(57857L);
        }

        DynamicVO modeloNotaVO = cabecalhoNotaDAO.findByPK(new Object[]{numeroUnicoModelo});
        Map<String, Object> campos = new HashMap();
        campos.put("NUMNOTA", numeroNota );
        campos.put("NUMCONTRATO", BigDecimal.ZERO);
        campos.put("CODEMP", BigDecimal.ONE);
        campos.put("CODPARC", parceiroVO.asBigDecimal("CODPARC") != null ? parceiroVO.asBigDecimal("CODPARC") : BigDecimal.ONE );
        campos.put("CODCENCUS", BigDecimal.valueOf(Long.parseLong((String) ct.getCampo("CODCENCUS"))));
        campos.put("CODNAT", BigDecimal.valueOf(Long.parseLong((String) ct.getCampo("CODNAT"))));
        campos.put("SERIENOTA", ct.getCampo("SERIENOTA"));
        campos.put("DTENTSAI", ct.getCampo("DTENTRCONT"));
        campos.put("DTNEG", ct.getCampo("DTMOV"));
        campos.put("DTFATUR", ct.getCampo("DTFATEM"));
        campos.put("DTMOV", ct.getCampo("DTMOV"));
        campos.put("CODPROJ", BigDecimal.valueOf(99990001));
        campos.put("OBSERVACAO", ct.getCampo("OBS") +" - Justificativa: "+ ct.getCampo("JSTCOMPR"));
        campos.put("CODUSU", AuthenticationInfo.getCurrent().getUserID());
        campos.put("CODUSUINC", AuthenticationInfo.getCurrent().getUserID());
        BigDecimal quantidade = BigDecimal.valueOf(Long.parseLong(String.valueOf(ct.getCampo("QTDNEG"))));
        Double valorUnitario = (Double) ct.getCampo("VLRUNIT");
        campos.put("VLRNOTA", quantidade.multiply(BigDecimal.valueOf(valorUnitario)));
        campos.put("STATUSNOTA", String.valueOf("A"));
        campos.put("PENDENTE", String.valueOf("S"));
        campos.put("CODTIPVENDA", BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("TPNEG"))));
        campos.put("DHTIPVENDA", datatipoNegociacao );
        campos.put("AD_CODLOT", BigDecimal.valueOf(Long.parseLong( (String) ct.getCampo("COD_LOTACAO"))));
        campos.put("CHAVENFE", ct.getCampo("CHAVENFE"));
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
        itemFCVO.set("PENDENTE", "S");
        itemFCVO.set("CODLOCALORIG", produtoVO.asBigDecimal("CODLOCALPADRAO"));
        itemFCVO.save();

        // Criando o Rateio
        FluidCreateVO rateioFCVO = rateioDAO.create();
        rateioFCVO.set("ORIGEM", String.valueOf("E"));
        rateioFCVO.set("NUFIN", numeroUnicoNota );
        rateioFCVO.set("CODNAT", BigDecimal.valueOf( Long.parseLong( (String) ct.getCampo("CODNAT"))));
        rateioFCVO.set("CODCENCUS", BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("CODCENCUS"))));
        rateioFCVO.set("CODPROJ", BigDecimal.valueOf(20001));

        NativeSqlDecorator unidadeUsuario = new NativeSqlDecorator("SELECT LOT.CODSITE FROM AD_TGFLOT LOT WHERE CODLOT = :CODLOT");
        unidadeUsuario.setParametro("CODLOT", BigDecimal.valueOf(Long.parseLong( (String) ct.getCampo("COD_LOTACAO"))) );

        if(unidadeUsuario.proximo()){
            unidade = unidadeUsuario.getValorBigDecimal("CODSITE");
        }

        rateioFCVO.set("CODSITE", unidade );
        rateioFCVO.set("PERCRATEIO", BigDecimal.valueOf(100));
        BigDecimal codnat = BigDecimal.valueOf( Long.parseLong( (String) ct.getCampo("CODNAT")));
        DynamicVO contaContabilCRVO = contaContabilCRDAO.findOne("NUCLASSIFICACAO = 1 AND CODNAT = ?", new Object[]{codnat});
        rateioFCVO.set("CODCTACTB", contaContabilCRVO.asBigDecimal("CODCTACTB"));
        rateioFCVO.set("NUMCONTRATO", BigDecimal.ZERO );
        rateioFCVO.set("CODPARC", notaDestino.asBigDecimal("CODPARC"));
        rateioFCVO.save();

        // Gravando o Número único da nota

        JapeWrapper caixapequenoDAO = JapeFactory.dao("AD_FINCAIXAPQ");
        DynamicVO caixapequenoVO = caixapequenoDAO.findOne("NUMNOTA = ?", new Object[]{numeroNota});

        FluidUpdateVO caixaPequenoFUVO = caixapequenoDAO.prepareToUpdate(caixapequenoVO);
        caixaPequenoFUVO.set("NUNOTA", numeroUnicoNota);
        caixaPequenoFUVO.set("CODAPROVADOR", codigoAprovador);
        caixaPequenoFUVO.update();

        BigDecimal idInstanceProcesso = new BigDecimal(String.valueOf(ct.getIdInstanceProcesso()));
        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "NUNOTA", numeroUnicoNota);

        }
    }
}
