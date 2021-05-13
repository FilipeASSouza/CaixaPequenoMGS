package br.com.flow.tarefa;

import br.com.sankhya.bh.dao.DynamicVOKt;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.SWRepositoryUtils;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;
import com.sankhya.util.TimeUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

public class CentralComprasCRUD {

    private JapeWrapper cabecalhoNotaDAO = JapeFactory.dao("CabecalhoNota");
    private JapeWrapper itemDAO = JapeFactory.dao("ItemNota");
    private JapeWrapper produtoDAO = JapeFactory.dao("Produto");
    private JapeWrapper rateioDAO = JapeFactory.dao("RateioRecDesp");
    private JapeWrapper contaContabilCRDAO = JapeFactory.dao("TGFNATCCCR");
    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");
    private BigDecimal unidade = null;
    private Timestamp datatipoNegociacao = null;
    private BigDecimal numeroUnicoNota = null;
    private BigDecimal codigoParceiro = null;
    private BigDecimal numeroNota = null;
    private BigDecimal codigotipoOperacao = null;
    private Timestamp dataTipoOperacao = null;
    private String statusNota = null;
    private BigDecimal codigoUsuario = null;
    private BigDecimal numeroUnicoFinanceiro = null;

    public void criandoCabeçalho(ContextoTarefa ct) throws Exception {

        String aprovacao = (String) ct.getCampo("APROVACAO");
        BigDecimal codigoAprovador = ct.getUsuarioLogado();
        numeroNota = BigDecimal.valueOf(Long.parseLong((String) ct.getCampo("NUMNOTA")));

        if( aprovacao != null
                && !aprovacao.equals(String.valueOf("1"))){

            // Criando o cabeçalho

            DynamicVO parceiroVO = parceiroDAO.findOne("CGC_CPF =?", new Object[]{ct.getCampo("CNPJ")});
            codigoParceiro = parceiroVO.asBigDecimal("CODPARC") != null ? parceiroVO.asBigDecimal("CODPARC") : BigDecimal.ONE;
            NativeSqlDecorator tipoNegociacaoDecorator = new NativeSqlDecorator("SELECT MAX(DHALTER) AS DHALTER FROM TGFTPV WHERE CODTIPVENDA = :CODTIPVENDA");
            tipoNegociacaoDecorator.setParametro("CODTIPVENDA", ct.getCampo("TPNEG") );

            BigDecimal numeroUnicoModelo = null;

            if( tipoNegociacaoDecorator.proximo() ){
                datatipoNegociacao = tipoNegociacaoDecorator.getValorTimestamp("DHALTER");
            }

            JapeWrapper caixapequenoDAO = JapeFactory.dao("AD_FINCAIXAPQ");
            DynamicVO caixapequenoVO = caixapequenoDAO.findOne("NUMNOTA = ?", new Object[]{numeroNota});
            codigoUsuario = caixapequenoVO.asBigDecimal("CODUSUARIO");

            if( ct.getCampo("TOPPROD") != null ){
                numeroUnicoModelo = BigDecimal.valueOf(27840L);
            }else{
                numeroUnicoModelo = BigDecimal.valueOf(28168L);
            }

            DynamicVO modeloNotaVO = cabecalhoNotaDAO.findByPK(new Object[]{numeroUnicoModelo});
            Map<String, Object> campos = new HashMap();
            campos.put("NUMNOTA", numeroNota );
            campos.put("NUMCONTRATO", BigDecimal.ZERO);
            campos.put("CODEMP", BigDecimal.ONE);
            campos.put("CODPARC", codigoParceiro );
            campos.put("CODCENCUS", new BigDecimal(Long.parseLong((String) ct.getCampo("CODCENCUS"))));
            campos.put("CODNAT", new BigDecimal(Long.parseLong((String) ct.getCampo("CODNAT"))));
            campos.put("SERIENOTA", ct.getCampo("SERIENOTA"));
            campos.put("DTENTSAI", ct.getCampo("DTENTRCONT"));
            campos.put("DTNEG", ct.getCampo("DTMOV"));
            campos.put("DTFATUR", ct.getCampo("DTFATEM"));
            campos.put("DTMOV", ct.getCampo("DTMOV"));
            campos.put("CODPROJ", BigDecimal.valueOf(99990001));
            campos.put("OBSERVACAO", ct.getCampo("OBS") +" - Justificativa: "+ ct.getCampo("JSTCOMPR"));
            campos.put("CODUSU", codigoUsuario );
            campos.put("CODUSUINC", codigoUsuario );
            BigDecimal quantidade = new BigDecimal(Long.parseLong(String.valueOf(ct.getCampo("QTDNEG"))));
            Double valorUnitario = (Double) ct.getCampo("VLRUNIT");
            statusNota = modeloNotaVO.asBigDecimal("CODTIPOPER").equals(BigDecimal.valueOf(603)) ? String.valueOf("L") : String.valueOf("A");
            campos.put("VLRNOTA", quantidade.multiply(BigDecimal.valueOf(valorUnitario)));
            campos.put("STATUSNOTA", statusNota );
            campos.put("PENDENTE", String.valueOf("S"));
            campos.put("CODTIPVENDA", new BigDecimal( Long.parseLong((String) ct.getCampo("TPNEG"))));
            campos.put("DHTIPVENDA", datatipoNegociacao );
            campos.put("AD_CODLOT", new BigDecimal(Long.parseLong( (String) ct.getCampo("COD_LOTACAO"))));
            campos.put("CHAVENFE", ct.getCampo("CHAVENFE"));
            if(modeloNotaVO.asBigDecimal("CODTIPOPER").equals(BigDecimal.valueOf(613L))){
                campos.put("NUMNFSE", numeroNota.toString() );
            }
            DynamicVO notaDestino = DynamicVOKt.duplicaRegistro(modeloNotaVO, campos);
            numeroUnicoNota = notaDestino.asBigDecimal("NUNOTA");
            codigotipoOperacao = notaDestino.asBigDecimal("CODTIPOPER");
            datatipoNegociacao = notaDestino.asTimestamp("DHTIPOPER");

            // Gravando o Número único da nota

            FluidUpdateVO caixaPequenoFUVO = caixapequenoDAO.prepareToUpdate(caixapequenoVO);
            caixaPequenoFUVO.set("NUNOTA", this.numeroUnicoNota);
            caixaPequenoFUVO.set("CODAPROVADOR", codigoAprovador);
            caixaPequenoFUVO.update();

            BigDecimal idInstanceProcesso = new BigDecimal(String.valueOf(ct.getIdInstanceProcesso()));
            VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "NUNOTA", numeroUnicoNota);

        }
    }

    public void criandoItens(ContextoTarefa ct) throws Exception{
        // Criando os itens

        BigDecimal quantidade = BigDecimal.valueOf(Long.parseLong(String.valueOf(ct.getCampo("QTDNEG"))));
        Double valorUnitario = (Double) ct.getCampo("VLRUNIT");

        FluidCreateVO itemFCVO = itemDAO.create();
        itemFCVO.set("NUNOTA", this.numeroUnicoNota );
        itemFCVO.set("CODPROD", BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("CODPROD"))));
        itemFCVO.set("QTDNEG", BigDecimal.valueOf((Long) ct.getCampo("QTDNEG")));
        DynamicVO produtoVO = produtoDAO.findByPK(BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("CODPROD"))));
        itemFCVO.set("CODVOL", produtoVO.asString("CODVOL"));
        itemFCVO.set("ATUALESTOQUE", BigDecimal.ZERO);
        itemFCVO.set("STATUSNOTA", this.statusNota );
        itemFCVO.set("USOPROD", produtoVO.asString("USOPROD"));
        itemFCVO.set("VLRUNIT", BigDecimal.valueOf((Double) ct.getCampo("VLRUNIT")));
        itemFCVO.set("VLRTOT", quantidade.multiply(BigDecimal.valueOf(valorUnitario)));
        itemFCVO.set("RESERVA", "N");
        itemFCVO.set("PENDENTE", "S");
        itemFCVO.set("CODLOCALORIG", produtoVO.asBigDecimal("CODLOCALPADRAO"));
        itemFCVO.save();
    }

    public void criandoRateio(ContextoTarefa ct) throws Exception{

        // Criando o Rateio
        FluidCreateVO rateioFCVO = rateioDAO.create();
        rateioFCVO.set("ORIGEM", String.valueOf("E"));
        rateioFCVO.set("NUFIN", this.numeroUnicoNota );
        rateioFCVO.set("CODNAT", BigDecimal.valueOf( Long.parseLong( (String) ct.getCampo("CODNAT"))));
        rateioFCVO.set("CODCENCUS", BigDecimal.valueOf( Long.parseLong((String) ct.getCampo("CODCENCUS"))));
        rateioFCVO.set("CODPROJ", BigDecimal.valueOf(20001));

        NativeSqlDecorator unidadeUsuario = new NativeSqlDecorator("SELECT LOT.CODSITE FROM AD_TGFLOT LOT WHERE CODLOT = :CODLOT");
        unidadeUsuario.setParametro("CODLOT", BigDecimal.valueOf(Long.parseLong( (String) ct.getCampo("COD_LOTACAO"))) );

        if(unidadeUsuario.proximo()){
            unidade = unidadeUsuario.getValorBigDecimal("CODSITE");
        }

        rateioFCVO.set("CODSITE", this.unidade );
        rateioFCVO.set("PERCRATEIO", BigDecimal.valueOf(100));
        BigDecimal codnat = BigDecimal.valueOf( Long.parseLong( (String) ct.getCampo("CODNAT")));
        DynamicVO contaContabilCRVO = contaContabilCRDAO.findOne("NUCLASSIFICACAO = 1 AND CODNAT = ?", new Object[]{codnat});
        rateioFCVO.set("CODCTACTB", contaContabilCRVO.asBigDecimal("CODCTACTB"));
        rateioFCVO.set("NUMCONTRATO", BigDecimal.ZERO );
        rateioFCVO.set("CODPARC", this.codigoParceiro );
        rateioFCVO.save();
    }

    public void criandoFinanceiro(ContextoTarefa ct) throws Exception{

        BigDecimal quantidade = BigDecimal.valueOf(Long.parseLong(String.valueOf(ct.getCampo("QTDNEG"))));
        Double valorUnitario = (Double) ct.getCampo("VLRUNIT");

        NativeSqlDecorator unidadeUsuario = new NativeSqlDecorator("SELECT LOT.CODSITE FROM AD_TGFLOT LOT WHERE CODLOT = :CODLOT");
        unidadeUsuario.setParametro("CODLOT", BigDecimal.valueOf(Long.parseLong( (String) ct.getCampo("COD_LOTACAO"))) );

        if(unidadeUsuario.proximo()){
            unidade = unidadeUsuario.getValorBigDecimal("CODSITE");
        }

        //Criando o registro na movimentação financeira

        JapeWrapper financeiroDAO = JapeFactory.dao("Financeiro");//TGFFIN

        NativeSqlDecorator dadosFinanceirosNegociacao = new NativeSqlDecorator("SELECT CODCTABCOINT, CODBCOPAD FROM TGFPPG WHERE CODTIPVENDA = :CODTIPVENDA");
        dadosFinanceirosNegociacao.setParametro("CODTIPVENDA", ct.getCampo("TPNEG") );

        BigDecimal codigoBanco = null;
        BigDecimal codigoConta = null;

        if(dadosFinanceirosNegociacao.proximo()){
            codigoConta = dadosFinanceirosNegociacao.getValorBigDecimal("CODCTABCOINT");
            codigoBanco = dadosFinanceirosNegociacao.getValorBigDecimal("CODBCOPAD");
        }

        //DESPESA
        FluidCreateVO financeiroDespesaFCVO = financeiroDAO.create();
        financeiroDespesaFCVO.set("NUMNOTA", this.numeroNota);
        financeiroDespesaFCVO.set("NUNOTA", this.numeroUnicoNota);
        financeiroDespesaFCVO.set("DTVENC", ct.getCampo("DTMOV"));
        financeiroDespesaFCVO.set("DTNEG", ct.getCampo("DTMOV") );
        financeiroDespesaFCVO.set("DTENTSAI", ct.getCampo("DTENTRCONT"));
        financeiroDespesaFCVO.set("VLRDESDOB", quantidade.multiply(BigDecimal.valueOf(valorUnitario)) );
        financeiroDespesaFCVO.set("ORIGEM", "E");
        financeiroDespesaFCVO.set("CODBCO", codigoBanco );
        financeiroDespesaFCVO.set("CODCTABCOINT", codigoConta );
        financeiroDespesaFCVO.set("CODTIPTIT", BigDecimal.ONE );
        financeiroDespesaFCVO.set("AD_CODSITE", this.unidade);
        financeiroDespesaFCVO.set("RECDESP", new BigDecimal(-1) );
        financeiroDespesaFCVO.set("CODNAT", new BigDecimal( Long.parseLong( (String) ct.getCampo("CODNAT"))) );
        financeiroDespesaFCVO.set("CODCENCUS", new BigDecimal( Long.parseLong((String) ct.getCampo("CODCENCUS"))) );
        financeiroDespesaFCVO.set("CODPROJ", new BigDecimal(99990001) );
        financeiroDespesaFCVO.set("CODEMP", new BigDecimal(1) );
        financeiroDespesaFCVO.set("CODPARC", this.codigoParceiro );
        financeiroDespesaFCVO.set("NUMCONTRATO", BigDecimal.ZERO );
        financeiroDespesaFCVO.set("CODTIPOPER", this.codigotipoOperacao );
        financeiroDespesaFCVO.set("DHTIPOPER", this.dataTipoOperacao );
        financeiroDespesaFCVO.set("CODUSU", this.codigoUsuario );
        financeiroDespesaFCVO.set("DHMOV", ct.getCampo("DTMOV") );
        financeiroDespesaFCVO.set("DTALTER", ct.getCampo("DTMOV") );
        financeiroDespesaFCVO.set("AD_CODLOT", new BigDecimal(Long.parseLong( (String) ct.getCampo("COD_LOTACAO"))) );

        DynamicVO financeiro = financeiroDespesaFCVO.save();
        numeroUnicoFinanceiro = financeiro.asBigDecimal("NUFIN");
    }

    public void integrandoAnexo(ContextoTarefa ct) throws Exception{

        //integrando anexo
        JapeWrapper anexoSistemaDAO = JapeFactory.dao("AnexoSistema");
        BigDecimal instancia = (BigDecimal) ct.getIdInstanceProcesso();
        String pkRegistro = instancia.toString().concat(String.valueOf("_InstanciaProcesso"));
        DynamicVO anexoSistemaVO = anexoSistemaDAO.findOne("PKREGISTRO = ?", new Object[]{pkRegistro});

        if(anexoSistemaVO != null){

            String chaveArquivo = anexoSistemaVO.asString("CHAVEARQUIVO");

            String diretorioBase = SWRepositoryUtils.getBaseFolder().getPath();
            String caminhoBase = diretorioBase+"/Sistema/Anexos/InstanciaProcesso/";
            String caminhoArquivo = caminhoBase+chaveArquivo;
            File file = new File(caminhoArquivo);

            JapeWrapper anexoDAO = JapeFactory.dao("Anexo");
            FluidCreateVO anexoFCVO = anexoDAO.create();
            anexoFCVO.set("DTALTER", TimeUtils.getNow());
            anexoFCVO.set("EDITA","N");
            anexoFCVO.set("ARQUIVO",  this.numeroUnicoNota.toString() );
            anexoFCVO.set("DESCRICAO", this.numeroUnicoNota.toString() );
            anexoFCVO.set("TIPOCONTEUDO","P");
            anexoFCVO.set("TIPO","N");
            anexoFCVO.set("CODUSU", this.codigoUsuario );
            anexoFCVO.set("CONTEUDO", FileUtils.readFileToByteArray(file));
            anexoFCVO.set("PUBLICO","N");
            anexoFCVO.set("SEQUENCIAPR", BigDecimal.ZERO );
            anexoFCVO.set("SEQUENCIA", BigDecimal.ZERO );
            anexoFCVO.set("DTINCLUSAO",TimeUtils.getNow() );
            anexoFCVO.set("CODATA", this.numeroUnicoNota );
            anexoFCVO.set("AD_TIPINCLUSAO", String.valueOf(1) );
            anexoFCVO.set("AD_NUATTACH", this.numeroUnicoFinanceiro );
            anexoFCVO.set("AD_CODUSUJOB", BigDecimal.ZERO );
            DynamicVO save = anexoFCVO.save();
        }
    }
}
