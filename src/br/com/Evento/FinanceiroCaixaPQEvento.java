package br.com.Evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.util.NativeSqlDecorator;

import java.math.BigDecimal;

public class FinanceiroCaixaPQEvento implements EventoProgramavelJava {

    private JapeWrapper parceiroDAO = JapeFactory.dao("Parceiro");
    private JapeWrapper centroResultadoDAO = JapeFactory.dao("CentroResultado");

    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
        validaCamposGravacao(persistenceEvent);
    }

    public void beforeUpdate(PersistenceEvent persistenceEvent) { }
    public void beforeDelete(PersistenceEvent persistenceEvent) { }
    public void afterInsert(PersistenceEvent persistenceEvent) { }
    public void afterUpdate(PersistenceEvent persistenceEvent) { }
    public void afterDelete(PersistenceEvent persistenceEvent) { }
    public void beforeCommit(TransactionContext transactionContext) { }

    public void validaCamposGravacao(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();

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
            throw new Exception("Centro de resultado esta inativo, favor informar outro centro de resultado!");
        }else if( centroResultadoVO.asString("ANALITICO").equalsIgnoreCase(String.valueOf("N")) ){
            throw new Exception("Centro de resultado informado esta incorreto, favor informar um centro de resultado analitico!");
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
        NativeSqlDecorator verificarNaturezaTopSQL = new NativeSqlDecorator("SELECT CODNAT FROM VIEW_NATUREZA_CAIXAPEQUENO WHERE CODTIPOPER = :CODTIPOPER AND CODNAT = :CODNAT ");
        verificarNaturezaTopSQL.setParametro("CODTIPOPER", codigoTipoOperacao );
        verificarNaturezaTopSQL.setParametro("CODNAT", vo.asBigDecimalOrZero("CODNAT"));

        BigDecimal natureza = null;

        if( verificarNaturezaTopSQL.proximo() ){
            natureza = verificarNaturezaTopSQL.getValorBigDecimal("CODNAT");
        }

        if(natureza == null){
            throw new Exception("Natureza informada incorretamente, fineza verificar!");
        }

        String registroAnexado = vo.asBigDecimal("IDINSTPRN").toString().concat(String.valueOf("_InstanciaProcesso"));
        String validaAnexo = null;

        NativeSqlDecorator verificarAnexoSQL = new NativeSqlDecorator("SELECT NUATTACH FROM TSIANX WHERE PKREGISTRO = :PKREGISTRO");
        verificarAnexoSQL.setParametro("PKREGISTRO", registroAnexado);

        if( verificarAnexoSQL.proximo() ){
            validaAnexo = verificarAnexoSQL.getValorString("NUATTACH");
        }

        if( validaAnexo == null ){
            throw new Exception("Fineza anexar a nota ao lançamento!");
        }
    }
}
