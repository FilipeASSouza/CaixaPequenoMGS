package br.com.Evento;

import br.com.util.ErroUtils;
import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;

public class EventoApoioAcesso implements EventoProgramavelJava {

    private JapeWrapper unidadesDAO = JapeFactory.dao("Site");
    private JapeWrapper usuariosDAO = JapeFactory.dao("Usuario");
    private JapeWrapper centroResultadoDAO = JapeFactory.dao("CentroResultado");
    private SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    @Override
    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        DynamicVO unidadesVO = unidadesDAO.findByPK(vo.asBigDecimal("CODSITE"));
        DynamicVO usuariosVO = usuariosDAO.findByPK(AuthenticationInfo.getCurrent().getUserID());
        DynamicVO centroResultadoVO = centroResultadoDAO.findByPK(usuariosVO.asBigDecimal("CODCENCUSPAD"));

        if( unidadesVO.asString("ATIVO").equalsIgnoreCase(String.valueOf("N")) ){
            ErroUtils.disparaErro("Unidade não está ativada, favor selecionar outra unidade!");
        }else if( usuariosVO.asTimestamp("DTLIMACESSO") != null ){
            if( sdf.format(usuariosVO.asTimestamp("DTLIMACESSO")).compareTo(sdf.format(TimeUtils.getNow())) < 0 ){
                ErroUtils.disparaErro("Usuário seleciona está inativo, favor informar outro usuário!");
            }
        }else if( usuariosVO.asBigDecimal("CODCENCUSPAD").equals(BigDecimal.ZERO)
                || usuariosVO.asBigDecimal("CODCENCUSPAD") == null){
            ErroUtils.disparaErro("Centro de resultado não informado, favor informar no cadastro do usuário!");
        }else if( centroResultadoVO.asString("ATIVO").equalsIgnoreCase(String.valueOf("N")) ){
            ErroUtils.disparaErro("Centro de resultado esta inativo, favor informar outro centro de resultado!");
        }else if( centroResultadoVO.asString("ANALITICO").equalsIgnoreCase(String.valueOf("N")) ){
            ErroUtils.disparaErro("Centro de resultado informado esta incorreto, favor informar um centro de resultado analitico!");
        }

        vo.setProperty("CODUSUINCL", usuariosVO.asBigDecimal("CODUSU"));
        vo.setProperty("DTINCL", TimeUtils.getNow());

    }

    @Override
    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {

        DynamicVO vo = (DynamicVO) persistenceEvent.getVo();
        DynamicVO unidadesVO = unidadesDAO.findByPK(vo.asBigDecimal("CODSITE"));
        DynamicVO usuariosVO = usuariosDAO.findByPK(AuthenticationInfo.getCurrent().getUserID());
        DynamicVO centroResultadoVO = centroResultadoDAO.findByPK(usuariosVO.asBigDecimal("CODCENCUSPAD"));

        if( unidadesVO.asString("ATIVO").equalsIgnoreCase(String.valueOf("N")) ){
            ErroUtils.disparaErro("Unidade não está ativada, favor selecionar outra unidade!");
        }else if( usuariosVO.asTimestamp("DTLIMACESSO") != null ){
            if( sdf.format(usuariosVO.asTimestamp("DTLIMACESSO")).compareTo(sdf.format(TimeUtils.getNow())) < 0 ){
                ErroUtils.disparaErro("Usuário seleciona está inativo, favor informar outro usuário!");
            }
        }else if( usuariosVO.asBigDecimal("CODCENCUSPAD").equals(BigDecimal.ZERO)
                || usuariosVO.asBigDecimal("CODCENCUSPAD") == null){
            ErroUtils.disparaErro("Centro de resultado não informado, favor informar no cadastro do usuário!");
        }else if( centroResultadoVO.asString("ATIVO").equalsIgnoreCase(String.valueOf("N")) ){
            ErroUtils.disparaErro("Centro de resultado esta inativo, favor informar outro centro de resultado!");
        }else if( centroResultadoVO.asString("ANALITICO").equalsIgnoreCase(String.valueOf("N")) ){
            ErroUtils.disparaErro("Centro de resultado informado esta incorreto, favor informar um centro de resultado analitico!");
        }

        vo.setProperty("CODUSUALTER", usuariosVO.asBigDecimal("CODUSU"));
        vo.setProperty("DTALTER", TimeUtils.getNow());

    }

    @Override
    public void beforeDelete(PersistenceEvent persistenceEvent) { }

    @Override
    public void afterInsert(PersistenceEvent persistenceEvent) { }

    @Override
    public void afterUpdate(PersistenceEvent persistenceEvent) { }

    @Override
    public void afterDelete(PersistenceEvent persistenceEvent) { }

    @Override
    public void beforeCommit(TransactionContext transactionContext) { }
}
