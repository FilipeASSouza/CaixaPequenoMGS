package br.com.Evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.helper.SaldoBancarioHelpper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;

public class FinanceiroCaixaPQEvento implements EventoProgramavelJava {

    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");//TGFPAR
    private JapeWrapper centroResultadoDAO = JapeFactory.dao("CentroResultado");//TSICUS
    private JapeWrapper financeiroDAO = JapeFactory.dao("Financeiro");//TGFFIN
    private JapeWrapper anexoSistemaDAO = JapeFactory.dao("AnexoSistema");//TSIANX
    private JapeWrapper instanciaTarefaDAO = JapeFactory.dao("InstanciaTarefa");//TWFITAR
    private JapeWrapper instanciaVariavelDAO = JapeFactory.dao("InstanciaVariavel");//TWFIVAR
    private JapeWrapper liberacaoEventoDAO = JapeFactory.dao("LiberacaoLimite");//TSILIB

    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
        validaCamposGravacao(persistenceEvent);

        validaSaldoCaixaPequeno(persistenceEvent);
    }

    public void beforeUpdate(PersistenceEvent persistenceEvent) { }
    public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception{

        JdbcWrapper jdbcWrapper = persistenceEvent.getJdbcWrapper();

        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        BigDecimal numeroUnico = vo.asBigDecimalOrZero("NUNOTA");
        BigDecimal nroinstanciaProcesso = vo.asBigDecimalOrZero("IDINSTPRN");
        BigDecimal codigoTipoOperacaoProduto = vo.asBigDecimalOrZero("TOPPROD");

        if( !numeroUnico.equals(BigDecimal.ZERO) ){
            numeroUnico = vo.asBigDecimalOrZero("NUNOTA");

            //Excluindo o lançamento do portal de compras
            excluindoLancamentoCentral(numeroUnico, jdbcWrapper );

            if( codigoTipoOperacaoProduto == null
                    || codigoTipoOperacaoProduto.equals(String.valueOf(""))
                    || codigoTipoOperacaoProduto.equals(String.valueOf("null")) ){

                liberacaoEventoDAO.deleteByCriteria("NUCHAVE = ?"
                        , new Object[]{ numeroUnico } );
            }

            financeiroDAO.deleteByCriteria("NUNOTA = ?"
                    , numeroUnico );
        }

        if( !nroinstanciaProcesso.equals(BigDecimal.ZERO)){
            nroinstanciaProcesso = vo.asBigDecimalOrZero("IDINSTPRN");

            Collection<DynamicVO> instanciasProcessosVOS = instanciaTarefaDAO.find("IDINSTPRN = ?"
                    , new Object[]{nroinstanciaProcesso});

            if( instanciasProcessosVOS != null ){

                for(DynamicVO instanciaProcessoVO : instanciasProcessosVOS){
                    instanciaTarefaDAO.deleteByCriteria("IDINSTPRN = ?"
                            , new Object[]{nroinstanciaProcesso});
                }
            }


            Collection<DynamicVO> instanciasVariaveisVOS = instanciaVariavelDAO.find("IDINSTPRN = ?"
                    , new Object[]{nroinstanciaProcesso});

            if( instanciasVariaveisVOS != null ){

                for( DynamicVO instanciaVariavelVO : instanciasVariaveisVOS ){
                    instanciaVariavelDAO.deleteByCriteria("IDINSTPRN = ?"
                            , new Object[]{ nroinstanciaProcesso });
                }
            }
        }

        String pkRegistro = nroinstanciaProcesso.toString().concat(String.valueOf("_InstanciaProcesso"));
        Collection <DynamicVO> anexosSistemaVO = anexoSistemaDAO.find("PKREGISTRO = ?", new Object[]{pkRegistro});

        if( anexosSistemaVO != null ){

            for(DynamicVO anexoSistemaVO : anexosSistemaVO ){
                anexoSistemaDAO.deleteByCriteria("NUATTACH = ?"
                        , new Object[]{ anexoSistemaVO.asBigDecimalOrZero("NUATTACH") });
            }
        }
    }

    public void afterInsert(PersistenceEvent persistenceEvent) { }
    public void afterUpdate(PersistenceEvent persistenceEvent) { }
    public void afterDelete(PersistenceEvent persistenceEvent) { }
    public void beforeCommit(TransactionContext transactionContext) { }

    public void excluindoLancamentoCentral(BigDecimal numeroUnico, JdbcWrapper jdbcWrapper) throws Exception{
        NativeSql cabecalhoNotaSql = new NativeSql(jdbcWrapper);
        NativeSql itemNotaSQL = new NativeSql(jdbcWrapper);

        try{

            if(!numeroUnico.equals(BigDecimal.ZERO)){

                cabecalhoNotaSql.appendSql("DELETE FROM TGFCAB WHERE NUNOTA = :NUNOTA");
                cabecalhoNotaSql.setNamedParameter("NUNOTA", numeroUnico );
                cabecalhoNotaSql.executeUpdate();

                itemNotaSQL.appendSql("DELETE FROM TGFITE WHERE NUNOTA = :NUNOTA");
                itemNotaSQL.setNamedParameter("NUNOTA", numeroUnico );
                itemNotaSQL.executeUpdate();
            }

        }catch(Exception e){
            e.printStackTrace();
        }finally {

            if(cabecalhoNotaSql != null){
                NativeSql.releaseResources(cabecalhoNotaSql);
            }

            if(itemNotaSQL != null){
                NativeSql.releaseResources(itemNotaSQL);
            }
            jdbcWrapper.closeSession();
        }
    }

    public void validaCamposGravacao(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        JdbcWrapper jdbcWrapper = persistenceEvent.getJdbcWrapper();

        if(vo.asString("CNPJ") == null ){
            throw new Exception("CNPJ não informado, fineza verificar novamente!");
        }

        DynamicVO parceiroVO = parceiroDAO.findOne("CGC_CPF = ?", new Object[]{vo.asString("CNPJ")});
        if( parceiroVO == null ){
            throw new Exception("CNPJ informado não foi encontrado, fineza verificar com o setor financeiro MGS!");
        }else if( parceiroVO.asString("FORNECEDOR").equalsIgnoreCase(String.valueOf("N")) ){
            throw new Exception("CNPJ informado não pertence a um fornecedor, fineza verificar com o setor financeiro MGS!");
        }else if( parceiroVO.asString("ATIVO").equalsIgnoreCase(String.valueOf("N")) ){
            throw new Exception("Cadastro não está ativo, fineza verificar com o setor financeiro MGS!");
        }

        if(vo.asBigDecimalOrZero("CODCENCUS").equals(BigDecimal.ZERO)){
            throw new Exception("Centro de resultado não informado, fineza verificar novamente!");
        }

        DynamicVO centroResultadoVO = centroResultadoDAO.findByPK(vo.asBigDecimal("CODCENCUS"));
        if(centroResultadoVO.asString("ATIVO").equalsIgnoreCase(String.valueOf("N"))){
            throw new Exception(VariaveisFlow.CENTRO_RESULTADO_INATIVO);
        }else if( centroResultadoVO.asString("ANALITICO").equalsIgnoreCase(String.valueOf("N")) ){
            throw new Exception(VariaveisFlow.CENTRO_RESULTADO_NAO_ANALITICO);
        }

        if(vo.asBigDecimalOrZero("NUMNOTA").equals(BigDecimal.ZERO)){
            throw new Exception("Número da nota não informado, fineza verificar novamente!");
        }

        if(vo.asBigDecimalOrZero("NUMCONTR").equals(BigDecimal.ZERO)){
            throw new Exception("Contrato não vinculado com a unidade/ lotação, fineza verificar com o setor financeiro da MGS!");
        }else if(vo.asBigDecimalOrZero("TPNEG").equals(BigDecimal.ZERO)){
            throw new Exception("Tipo de negociação não informado, fineza verificar novamente!");
        }else if(vo.asBigDecimalOrZero("CODNAT").equals(BigDecimal.ZERO)){
            throw new Exception("Natureza não informado, fineza verificar novamente!");
        }else if(vo.asString("SERIENOTA") == null){
            throw new Exception("Serie não informada, fineza verificar novamente!");
        }else if(vo.asTimestamp("DTFATEM") == null
                || vo.asTimestamp("DTENTRCONT") == null
                || vo.asTimestamp("DTMOV") == null ){
            throw new Exception("Fineza verificar se as datas foram informadas corretamente!");
        }else if(vo.asBigDecimal("QTDNEG") == null || vo.asBigDecimal("VLRUNIT") == null){
            throw new Exception("Quantidade ou Valor Unitario não informado, fineza verificar novamente!");
        }

        BigDecimal codigoTipoOperacao = vo.asBigDecimal("TOPPROD") == null ? vo.asBigDecimal("TOPSERV") : vo.asBigDecimal("TOPPROD");
        NativeSqlDecorator verificarNaturezaTopSQL = null;
        BigDecimal natureza = null;
        try{

            verificarNaturezaTopSQL = new NativeSqlDecorator("SELECT CODNAT FROM VIEW_NATUREZA_CAIXAPEQUENO WHERE CODTIPOPER = :CODTIPOPER AND CODNAT = :CODNAT ", jdbcWrapper);
            verificarNaturezaTopSQL.setParametro("CODTIPOPER", codigoTipoOperacao );
            verificarNaturezaTopSQL.setParametro("CODNAT", vo.asBigDecimalOrZero("CODNAT"));

            if( verificarNaturezaTopSQL.proximo() ){
                natureza = verificarNaturezaTopSQL.getValorBigDecimal("CODNAT");
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("Restrições no tipo de operação, natureza informada incorretamente. Fineza verificar!");
        } finally {
            if(verificarNaturezaTopSQL != null){
                verificarNaturezaTopSQL.close();
            }
        }

        String registroAnexado = vo.asBigDecimal("IDINSTPRN").toString().concat(String.valueOf("_InstanciaProcesso"));

        NativeSqlDecorator verificarAnexoSQL = null;
        String validaAnexo = null;

        try{

            verificarAnexoSQL = new NativeSqlDecorator("SELECT NUATTACH FROM TSIANX WHERE PKREGISTRO = :PKREGISTRO", jdbcWrapper);
            verificarAnexoSQL.setParametro("PKREGISTRO", registroAnexado);

            if( verificarAnexoSQL.proximo() ){
                validaAnexo = verificarAnexoSQL.getValorString("NUATTACH");
            }
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception(VariaveisFlow.NOTA_SEM_ANEXO);
        }finally {
            if (verificarAnexoSQL != null){
                verificarAnexoSQL.close();
            }
        }

        if( validaAnexo == null){
            throw new Exception(VariaveisFlow.NOTA_SEM_ANEXO);
        }

        NativeSqlDecorator verificaRestricaoSerieTop = null;
        try{

            verificaRestricaoSerieTop = new NativeSqlDecorator("SELECT CODTIPOPER FROM TGFREP WHERE CODTIPOPER = :CODTIPOPER AND SERIE = :SERIE AND TIPREST = 'S'", jdbcWrapper);
            verificaRestricaoSerieTop.setParametro("CODTIPOPER", codigoTipoOperacao);
            verificaRestricaoSerieTop.setParametro("SERIE", vo.asString("SERIENOTA"));

            if(verificaRestricaoSerieTop.proximo()){
                if(verificaRestricaoSerieTop.getValorBigDecimal("CODTIPOPER") != null ){
                    throw new Exception("Restrições no tipo de operação, essa serie não pode ser utilizada para esse lançamento. Fineza verificar!");
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if (verificaRestricaoSerieTop != null){
                verificaRestricaoSerieTop.close();
            }
        }
    }


    public void validaSaldoCaixaPequeno(PersistenceEvent persistenceEvent) throws Exception {

        DynamicVO movimentoBancarioVO = (DynamicVO)persistenceEvent.getVo();
        JapeWrapper tipoNegociacaoDAO = JapeFactory.dao("TipoNegociacao");

        DynamicVO tipoNegociacaoVO = tipoNegociacaoDAO.findOne("CODTIPVENDA = ?",new Object[]{movimentoBancarioVO.asBigDecimal("TPNEG")});

        DynamicVO parcelaVO = JapeFactory.dao("ParcelaPagamento").findOne("CODTIPVENDA = ?"
                , new Object[]{ tipoNegociacaoVO.asBigDecimal("CODTIPVENDA") });

        DynamicVO contaVo = JapeFactory.dao("ContaBancaria").findOne("CODCTABCOINT = ?"
                , new Object[]{ parcelaVO.asBigDecimal("CODCTABCOINT") });
        BigDecimal limiteSuperior = contaVo.asBigDecimal("AD_VLRLIMIT");

        JdbcWrapper jdbcWrapper = persistenceEvent.getJdbcWrapper();
        BigDecimal saldoConta = SaldoBancarioHelpper.getSaldoRealAntesDaReferencia(jdbcWrapper, parcelaVO.asBigDecimal("CODCTABCOINT"), new Timestamp(TimeUtils.add(TimeUtils.getNow().getTime(), 1, 6)));
        if (limiteSuperior != null) {

            if (BigDecimal.ZERO.compareTo(saldoConta) > 0) {
                throw new Exception("Saldo da conta insuficiente");
            }

            if (saldoConta.compareTo(limiteSuperior) > 0) {
                throw new Exception("Saldo da conta não pode ultrapassar o limite");
            }

            if( movimentoBancarioVO.asBigDecimal("VLRTOT").compareTo(saldoConta) > 0 ){
                throw new Exception("Valor ultrapassou o saldo permitido para essa conta, gentileza procurar o setor financeiro!");
            }
        }
    }
}
