package br.com.flow.tarefa;

import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.jape.core.JapeSession;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import br.com.sankhya.modelcore.util.SWRepositoryUtils;
import br.com.util.VariaveisFlow;
import com.sankhya.util.TimeUtils;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;

public class CentralComprasCRUD {

    private JapeWrapper cabecalhoNotaDAO = JapeFactory.dao("CabecalhoNota");
    private JapeWrapper itemDAO = JapeFactory.dao("ItemNota");
    private JapeWrapper produtoDAO = JapeFactory.dao("Produto");
    private JapeWrapper rateioDAO = JapeFactory.dao("RateioRecDesp");
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
    private BigDecimal valorDesconto = null;
    private BigDecimal valorTotal = null;

    public void criandoCabeçalho(ContextoTarefa ct) throws Exception {

        BigDecimal codigoAprovador = ct.getUsuarioLogado();
        numeroNota = BigDecimal.valueOf(Long.parseLong(ct.getCampo("NUMNOTA").toString()));
        BigDecimal chaveRegistro = new BigDecimal(Long.parseLong(ct.getIdInstanceProcesso().toString()));
        BigDecimal codigoCentroResultado = new BigDecimal(Long.parseLong(ct.getCampo("CODCENCUS").toString()));

        // Criando o cabeçalho

        DynamicVO parceiroVO = parceiroDAO.findOne("CGC_CPF =?", new Object[]{ct.getCampo("CNPJ")});
        codigoParceiro = parceiroVO.asBigDecimal("CODPARC") != null ? parceiroVO.asBigDecimal("CODPARC") : BigDecimal.ONE;

        QueryExecutor consultaTipoNegociacao = ct.getQuery();
        consultaTipoNegociacao.setParam("CODTIPVENDA", ct.getCampo("TPNEG"));
        consultaTipoNegociacao.nativeSelect("SELECT MAX(DHALTER) AS DHALTER FROM TGFTPV WHERE CODTIPVENDA = {CODTIPVENDA}");
        if (consultaTipoNegociacao.next()){
            datatipoNegociacao = consultaTipoNegociacao.getTimestamp("DHALTER");
        }
        consultaTipoNegociacao.close();

        BigDecimal numeroUnicoModelo = null;

        JapeWrapper caixapequenoDAO = JapeFactory.dao("AD_FINCAIXAPQ");
        DynamicVO caixapequenoVO = caixapequenoDAO.findOne("IDINSTPRN = ?", new Object[]{chaveRegistro});
        codigoUsuario = caixapequenoVO.asBigDecimal("CODUSUARIO");

        if( !ct.getCampo("TOPPROD").toString().equalsIgnoreCase(String.valueOf("")) ){
            numeroUnicoModelo = BigDecimal.valueOf(27840L);
        }else{
            numeroUnicoModelo = BigDecimal.valueOf(554470L);
        }
        BigDecimal codigoTipoOperacao = BigDecimal.valueOf(Long.parseLong(ct.getCampo("TOPSERV").toString().equalsIgnoreCase("") ? ct.getCampo("TOPPROD").toString() : ct.getCampo("TOPSERV").toString()));

        DynamicVO modeloNotaVO = cabecalhoNotaDAO.findByPK(new Object[]{numeroUnicoModelo});
        modeloNotaVO.setProperty("CODTIPOPER", codigoTipoOperacao);

        QueryExecutor consultaTipoOperacao = ct.getQuery();
        consultaTipoOperacao.setParam("CODTIPOPER", modeloNotaVO.asBigDecimal("CODTIPOPER"));
        consultaTipoOperacao.nativeSelect("SELECT MAX(DHALTER) DHALTER FROM TGFTOP WHERE CODTIPOPER = {CODTIPOPER}");
        if (consultaTipoOperacao.next()){
            this.dataTipoOperacao = consultaTipoOperacao.getTimestamp("DHALTER");
        }
        consultaTipoOperacao.close();

        FluidCreateVO cabecalhoNotaFCVO = cabecalhoNotaDAO.create();
        cabecalhoNotaFCVO.set("NUMNOTA", numeroNota );
        cabecalhoNotaFCVO.set("NUMCONTRATO", new BigDecimal(Long.parseLong(ct.getCampo("NUMCONTR").toString())));
        cabecalhoNotaFCVO.set("CODEMP", BigDecimal.ONE);
        cabecalhoNotaFCVO.set("CODPARC", codigoParceiro );
        cabecalhoNotaFCVO.set("CODCENCUS", codigoCentroResultado );
        cabecalhoNotaFCVO.set("CODNAT", new BigDecimal(Long.parseLong(ct.getCampo("CODNAT").toString())));
        cabecalhoNotaFCVO.set("SERIENOTA", ct.getCampo("SERIENOTA"));
        cabecalhoNotaFCVO.set("DTENTSAI", ct.getCampo("DTENTRCONT"));
        cabecalhoNotaFCVO.set("DTNEG", ct.getCampo("DTMOV"));
        cabecalhoNotaFCVO.set("DTFATUR", ct.getCampo("DTFATEM"));
        cabecalhoNotaFCVO.set("DTMOV", ct.getCampo("DTMOV"));
        cabecalhoNotaFCVO.set("CODTIPOPER", modeloNotaVO.asBigDecimal("CODTIPOPER") );
        cabecalhoNotaFCVO.set("CODEMPNEGOC", BigDecimal.ONE);
        cabecalhoNotaFCVO.set("CIF_FOB", String.valueOf("F"));
        cabecalhoNotaFCVO.set("RATEADO", String.valueOf("S"));
        cabecalhoNotaFCVO.set("DHTIPOPER", this.dataTipoOperacao );
        cabecalhoNotaFCVO.set("CODPROJ", BigDecimal.valueOf(99990001) );
        cabecalhoNotaFCVO.set("OBSERVACAO", ct.getCampo("OBS") +" - Justificativa: "+ ct.getCampo("JSTCOMPR"));
        cabecalhoNotaFCVO.set("CODUSU", codigoUsuario );
        cabecalhoNotaFCVO.set("CODUSUINC", codigoUsuario );

        BigDecimal quantidade = new BigDecimal(Long.parseLong(String.valueOf(ct.getCampo("QTDNEG"))));
        Double valorUnitario = (Double) ct.getCampo("VLRUNIT");
        valorDesconto = ct.getCampo("VLRDESCTOT") == "" || ct.getCampo("VLRDESCTOT") == null
                ? BigDecimal.ZERO : new BigDecimal(ct.getCampo("VLRDESCTOT").toString());
        valorTotal = quantidade.multiply(BigDecimal.valueOf(valorUnitario)).subtract(valorDesconto);
        statusNota = modeloNotaVO.asBigDecimal("CODTIPOPER").equals(BigDecimal.valueOf(603)) ? String.valueOf("L") : String.valueOf("A");

        cabecalhoNotaFCVO.set("VLRNOTA", valorTotal );
        cabecalhoNotaFCVO.set("VLRDESCTOT", valorDesconto);
        cabecalhoNotaFCVO.set("STATUSNOTA", statusNota );
        cabecalhoNotaFCVO.set("PENDENTE", String.valueOf("N"));
        cabecalhoNotaFCVO.set("CODTIPVENDA", new BigDecimal( Long.parseLong((String) ct.getCampo("TPNEG"))));
        cabecalhoNotaFCVO.set("DHTIPVENDA", datatipoNegociacao );
        cabecalhoNotaFCVO.set("AD_CODLOT", new BigDecimal(Long.parseLong( (String) ct.getCampo("COD_LOTACAO"))));
        cabecalhoNotaFCVO.set("CHAVENFE", ct.getCampo("CHAVENFE"));
        cabecalhoNotaFCVO.set("CODCONTATO", null );
        cabecalhoNotaFCVO.set("AD_DTVENC", null );
        cabecalhoNotaFCVO.set("QTDVOL", BigDecimal.ONE );
        cabecalhoNotaFCVO.set("PESO", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("TOTALCUSTOPROD", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("TOTALCUSTOSERV", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("PESOBRUTO", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("BASEIRF", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("ALIQIRF", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("VLRSTEXTRANOTATOT", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("CODCIDORIGEM", BigDecimal.valueOf(2754L) );
        cabecalhoNotaFCVO.set("CODCIDDESTINO", BigDecimal.valueOf(2754L) );
        cabecalhoNotaFCVO.set("CODCIDENTREGA", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("CODUFORIGEM", BigDecimal.valueOf(2L) );
        cabecalhoNotaFCVO.set("CODUFDESTINO", BigDecimal.valueOf(2L) );
        cabecalhoNotaFCVO.set("CODUFENTREGA", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("CLASSIFICMS", String.valueOf("C") );
        cabecalhoNotaFCVO.set("VLRREPREDTOTSEMDESC", BigDecimal.ZERO );
        cabecalhoNotaFCVO.set("VLRFETHAB", BigDecimal.ZERO );
        if(modeloNotaVO.asBigDecimal("CODTIPOPER").equals(BigDecimal.valueOf(613L))
            || modeloNotaVO.asBigDecimal("CODTIPOPER").equals(BigDecimal.valueOf(612L))){
            cabecalhoNotaFCVO.set("NUMNFSE", numeroNota.toString() );
        }

        DynamicVO notaDestino = cabecalhoNotaFCVO.save();
        numeroUnicoNota = notaDestino.asBigDecimal("NUNOTA");
        codigotipoOperacao = notaDestino.asBigDecimal("CODTIPOPER");

        // Gravando o Número único da nota

        FluidUpdateVO caixaPequenoFUVO = caixapequenoDAO.prepareToUpdate(caixapequenoVO);
        caixaPequenoFUVO.set("NUNOTA", this.numeroUnicoNota);
        caixaPequenoFUVO.set("CODAPROVADOR", codigoAprovador);
        caixaPequenoFUVO.update();

        BigDecimal idInstanceProcesso = new BigDecimal(String.valueOf(ct.getIdInstanceProcesso()));
        VariaveisFlow.setVariavel(idInstanceProcesso, new BigDecimal(0), "NUNOTA", numeroUnicoNota);

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
        itemFCVO.set("VLRUNIT", BigDecimal.valueOf(valorUnitario));
        itemFCVO.set("VLRTOT", quantidade.multiply(BigDecimal.valueOf(valorUnitario)));
        itemFCVO.set("RESERVA", "N");
        itemFCVO.set("PENDENTE", "N");
        itemFCVO.set("CODLOCALORIG", produtoVO.asBigDecimal("CODLOCALPADRAO"));
        itemFCVO.save();
    }

    public void criandoRateio(ContextoTarefa ct) throws Exception{

        JapeWrapper rateioFlowDAO = JapeFactory.dao("AD_RATEIOCPQ");
        BigDecimal idInstanceProcesso = new BigDecimal(ct.getIdInstanceProcesso().toString());
        Collection<DynamicVO> rateiosVO = rateioFlowDAO.find("IDINSTPRN = ?", new Object[]{idInstanceProcesso});

        for (DynamicVO rateio : rateiosVO){

            // Criando o Rateio
            FluidCreateVO rateioFCVO = rateioDAO.create();
            rateioFCVO.set("ORIGEM", String.valueOf("E"));
            rateioFCVO.set("NUFIN", this.numeroUnicoNota );
            rateioFCVO.set("CODNAT", rateio.asBigDecimal("CODNAT"));
            rateioFCVO.set("CODCENCUS", rateio.asBigDecimal("CODCENCUS"));
            rateioFCVO.set("CODPROJ", rateio.asBigDecimal("CODPROJ"));
            rateioFCVO.set("CODSITE", rateio.asBigDecimal("CODSITRATEIO"));
            rateioFCVO.set("PERCRATEIO", rateio.asBigDecimal("PERCRATEIO"));
            rateioFCVO.set("CODCTACTB", rateio.asBigDecimal("CODCTACTB"));
            rateioFCVO.set("NUMCONTRATO", new BigDecimal(Long.parseLong(ct.getCampo("NUMCONTR").toString())));
            rateioFCVO.set("CODPARC", this.codigoParceiro );
            rateioFCVO.save();
        }
    }

    public void criandoFinanceiro(ContextoTarefa ct) throws Exception{

        this.unidade = BigDecimal.valueOf(Long.parseLong(ct.getCampo("UNID_FATURAMENTO").toString()));

        //Criando o registro na movimentação financeira

        JapeWrapper financeiroDAO = JapeFactory.dao("Financeiro");//TGFFIN

        BigDecimal codigoBanco = null;
        BigDecimal codigoConta = null;

        QueryExecutor consultaFinanceiroNegociacao = ct.getQuery();
        consultaFinanceiroNegociacao.setParam("CODTIPVENDA", ct.getCampo("TPNEG"));
        consultaFinanceiroNegociacao.nativeSelect("SELECT CODCTABCOINT, CODBCOPAD FROM TGFPPG WHERE CODTIPVENDA = {CODTIPVENDA}");
        if (consultaFinanceiroNegociacao.next()){
            codigoConta = consultaFinanceiroNegociacao.getBigDecimal("CODCTABCOINT");
            codigoBanco = consultaFinanceiroNegociacao.getBigDecimal("CODBCOPAD");
        }
        consultaFinanceiroNegociacao.close();

        //DESPESA
        FluidCreateVO financeiroDespesaFCVO = financeiroDAO.create();
        financeiroDespesaFCVO.set("NUMNOTA", this.numeroNota);
        financeiroDespesaFCVO.set("NUNOTA", this.numeroUnicoNota);
        financeiroDespesaFCVO.set("DTVENC", ct.getCampo("DTMOV"));
        financeiroDespesaFCVO.set("DTNEG", ct.getCampo("DTMOV") );
        financeiroDespesaFCVO.set("DTENTSAI", ct.getCampo("DTENTRCONT"));
        financeiroDespesaFCVO.set("VLRDESDOB", this.valorTotal );
        financeiroDespesaFCVO.set("ORIGEM", "E");
        financeiroDespesaFCVO.set("RATEADO", "S");
        financeiroDespesaFCVO.set("PROVISAO", this.codigotipoOperacao.equals(BigDecimal.valueOf(612L)) ? "S" : "N");
        financeiroDespesaFCVO.set("CODBCO", codigoBanco );
        financeiroDespesaFCVO.set("CODCTABCOINT", codigoConta );
        financeiroDespesaFCVO.set("CODTIPTIT", BigDecimal.ONE );
        financeiroDespesaFCVO.set("AD_CODSITE", this.unidade );
        financeiroDespesaFCVO.set("RECDESP", new BigDecimal(-1) );
        financeiroDespesaFCVO.set("CODNAT", new BigDecimal( Long.parseLong( (String) ct.getCampo("CODNAT"))) );
        financeiroDespesaFCVO.set("CODCENCUS", BigDecimal.valueOf( Long.parseLong( ct.getCampo("CODCENCUS").toString())) );
        financeiroDespesaFCVO.set("CODPROJ", new BigDecimal(99990001) );
        financeiroDespesaFCVO.set("CODEMP", new BigDecimal(1) );
        financeiroDespesaFCVO.set("CODPARC", this.codigoParceiro );
        financeiroDespesaFCVO.set("NUMCONTRATO", new BigDecimal(ct.getCampo("NUMCONTR").toString()) );
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
        Collection <DynamicVO> anexosSistemaVO = anexoSistemaDAO.find("PKREGISTRO = ?", new Object[]{pkRegistro});

        int i = 1;

        for(DynamicVO anexoSistemaVO : anexosSistemaVO ){


            String chaveArquivo = anexoSistemaVO.asString("CHAVEARQUIVO");

            String diretorioBase = SWRepositoryUtils.getBaseFolder().getPath();
            String caminhoBase = diretorioBase+"/Sistema/Anexos/InstanciaProcesso/";
            String caminhoArquivo = caminhoBase+chaveArquivo;
            File file = new File(caminhoArquivo);

            JapeWrapper anexoDAO = JapeFactory.dao("Anexo");
            FluidCreateVO anexoCentralNotaFCVO = anexoDAO.create();
            anexoCentralNotaFCVO.set("DTALTER", TimeUtils.getNow());
            anexoCentralNotaFCVO.set("EDITA","N");
            anexoCentralNotaFCVO.set("ARQUIVO",  this.numeroUnicoNota.toString() + i );
            anexoCentralNotaFCVO.set("DESCRICAO", this.numeroUnicoNota.toString() + i );
            anexoCentralNotaFCVO.set("TIPOCONTEUDO","P");
            anexoCentralNotaFCVO.set("TIPO","N");
            anexoCentralNotaFCVO.set("CODUSU", this.codigoUsuario );
            anexoCentralNotaFCVO.set("CONTEUDO", FileUtils.readFileToByteArray(file));
            anexoCentralNotaFCVO.set("PUBLICO","N");
            anexoCentralNotaFCVO.set("SEQUENCIAPR", BigDecimal.ZERO );
            anexoCentralNotaFCVO.set("SEQUENCIA", BigDecimal.ZERO );
            anexoCentralNotaFCVO.set("DTINCLUSAO",TimeUtils.getNow() );
            anexoCentralNotaFCVO.set("CODATA", this.numeroUnicoNota );
            anexoCentralNotaFCVO.set("AD_TIPINCLUSAO", String.valueOf(1) );
            anexoCentralNotaFCVO.set("AD_NUATTACH", this.numeroUnicoFinanceiro );
            anexoCentralNotaFCVO.set("AD_CODUSUJOB", BigDecimal.ZERO );
            anexoCentralNotaFCVO.save();
            ++i;

        }
    }

    public void criandoLiberacao(ContextoTarefa ct) throws Exception{

        JapeWrapper liberacaoCascataDAO = JapeFactory.dao("LiberacaoCascata");
        Collection<DynamicVO> listaLiberacaoCascataVO = liberacaoCascataDAO.find("CODTIPOPER = ? "
                , new Object[]{this.codigotipoOperacao});

        //verificando se a operacao e 613(serviço)
        if(this.codigotipoOperacao.equals(BigDecimal.valueOf(613L))
                || this.codigotipoOperacao.equals(BigDecimal.valueOf(612L)) ){

            if(listaLiberacaoCascataVO != null){

                for(int i = 0; i < listaLiberacaoCascataVO.size(); i++){

                    Boolean confirmando = (Boolean) JapeSession.getProperty("CabecalhoNota.confirmando.nota", true);

                    JapeWrapper liberacaoDAO = JapeFactory.dao("LiberacaoLimite");

                    FluidCreateVO liberacaoFCVO = liberacaoDAO.create();
                    liberacaoFCVO.set("NUCHAVE", this.numeroUnicoNota);
                    liberacaoFCVO.set("TABELA", String.valueOf("TGFCAB"));
                    liberacaoFCVO.set("EVENTO", BigDecimal.valueOf(18L));
                    liberacaoFCVO.set("CODUSUSOLICIT", this.codigoUsuario);
                    liberacaoFCVO.set("DHSOLICIT", TimeUtils.getNow());
                    liberacaoFCVO.set("VLRLIMITE", BigDecimal.ZERO);
                    liberacaoFCVO.set("VLRATUAL", BigDecimal.ONE);
                    liberacaoFCVO.set("SEQCASCATA", BigDecimal.valueOf(Long.parseLong(String.valueOf(i))) );
                    liberacaoFCVO.set("SEQUENCIA", BigDecimal.valueOf(Long.parseLong(String.valueOf(i))) );
                    liberacaoFCVO.set("NUCLL", BigDecimal.ZERO );
                    liberacaoFCVO.set("CODUSULIB", BigDecimal.valueOf(447L));
                    liberacaoFCVO.set("ORDEM", BigDecimal.valueOf(2L));
                    liberacaoFCVO.set("OBSERVACAO", String.valueOf("Automacao Caixa Pequeno"));
                    liberacaoFCVO.set("CODTIPOPER", this.codigotipoOperacao);
                    liberacaoFCVO.set("VLRLIBERADO", BigDecimal.ZERO);
                    liberacaoFCVO.set("CODCENCUS", BigDecimal.valueOf( Long.parseLong(ct.getCampo("CODCENCUS").toString())));
                    liberacaoFCVO.save();
                }
            }
        }
    }
}
