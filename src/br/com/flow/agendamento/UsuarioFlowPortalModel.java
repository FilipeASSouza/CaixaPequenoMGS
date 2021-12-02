package br.com.flow.agendamento;

import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.util.Collection;

public class UsuarioFlowPortalModel {

    BigDecimal idUsuario;
    JapeWrapper usuarioDAO = JapeFactory.dao("Usuario");
    JapeWrapper contatoDAO = JapeFactory.dao("Contato");
    JapeWrapper equipeDAO = JapeFactory.dao("Equipe");
    JapeWrapper membroEquipeDAO = JapeFactory.dao("MembroEquipe");
    BigDecimal codigoGrupoUsuario;
    BigDecimal codigoUsuario;
    BigDecimal codigoParceiro;
    BigDecimal numeroEquipe;
    String acesso;
    NativeSql nativeSql = null;

    public UsuarioFlowPortalModel() throws Exception {
        this.codigoGrupoUsuario = JapeFactory.dao("ParametroSistema").findByPK(new Object[]{"AD_GRUFLOWPORT", BigDecimal.ZERO}).asBigDecimal("INTEIRO");
        this.codigoParceiro = JapeFactory.dao("ParametroSistema").findByPK(new Object[]{"AD_PARFLOWPORT", BigDecimal.ZERO}).asBigDecimal("INTEIRO");
    }

    public void processarAcesso(BigDecimal idUsuario, String acao, JdbcWrapper jdbcWrapper) throws Exception {
        this.idUsuario = idUsuario;
        nativeSql = new NativeSql(jdbcWrapper);

        nativeSql.appendSql("SELECT ACESSO, UNIDADE FROM VIEW_USUARIOSPORTAL WHERE ID_USUARIO = :ID_USUARIO");
        nativeSql.setNamedParameter("ID_USUARIO", idUsuario);

        this.criarUsuario();
        this.criarContato();

        ResultSet rs = nativeSql.executeQuery();

        try{
            while( rs.next() ) {
                this.acesso = rs.getString("ACESSO");
                BigDecimal unidade = rs.getBigDecimal("UNIDADE");
                this.ingressarEquipe(unidade, this.acesso, jdbcWrapper);
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if( nativeSql != null ){
                NativeSql.releaseResources( nativeSql );
            }

            rs.close();

            //Atualizando a tabela UNIDADES_TAREFAS_SANKHYA no schema PORTAL CLIENTE
            atualizandoPortalCliente( idUsuario, acao, jdbcWrapper );
        }
    }

    private void criarUsuario() throws Exception {
        DynamicVO usuarioVO = this.usuarioDAO.findOne("NOMEUSU = ?", new Object[]{"PTL" + this.idUsuario.toString()});
        if (usuarioVO == null) {
            FluidCreateVO usuarioFCVO = this.usuarioDAO.create();
            usuarioFCVO.set("NOMEUSU", "PTL" + this.idUsuario.toString());
            usuarioFCVO.set("CODGRUPO", this.codigoGrupoUsuario);
            usuarioFCVO.set("IGNORALDAP", "S");
            usuarioFCVO.set("INTERNO", "sankhyaw");
            this.codigoUsuario = usuarioFCVO.save().asBigDecimal("CODUSU");
        } else {
            this.codigoUsuario = usuarioVO.asBigDecimal("CODUSU");
        }
    }

    private void criarContato() throws Exception {
        DynamicVO contatoVO = this.contatoDAO.findOne("CODPARC = ? AND LOWER(EMAIL) = ?", new Object[]{this.codigoParceiro, this.idUsuario + "@portal.mgs.srv.br"});
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        if (contatoVO == null) {
            FluidCreateVO usuarioFCVO = this.contatoDAO.create();
            usuarioFCVO.set("CODPARC", this.codigoParceiro);
            usuarioFCVO.set("EMAIL", this.idUsuario + "@portal.mgs.srv.br");
            usuarioFCVO.set("SENHAACESSO", StringUtils.toHexString(md5.digest(this.idUsuario.toString().getBytes())));
            usuarioFCVO.set("CODUSU", this.codigoUsuario);
            usuarioFCVO.set("AD_USUARIOPORTAL", this.idUsuario );
            usuarioFCVO.save();
        }
    }

    private void ingressarEquipe(BigDecimal unidadeFaturamento, String acesso, JdbcWrapper jdbcWrapper ) throws Exception {
        DynamicVO equipeVO = this.equipeDAO.findOne("AD_UNID_FAT = ? AND AD_TPACESSODV = ?", new Object[]{unidadeFaturamento, acesso});
        if (equipeVO != null) {
            DynamicVO membroEquipeVO = this.membroEquipeDAO.findOne("NUEQUIPE = ?  AND CODMEMBRO = ?", new Object[]{equipeVO.asBigDecimal("NUEQUIPE"), this.codigoUsuario});
            if (membroEquipeVO == null) {

                System.out.println("EQUIPE EXISTE INSERINDO MEMBRO  NA EQUIPE" + equipeVO.asBigDecimal("NUEQUIPE").toString() );

                FluidCreateVO membroEquipeFCVO = this.membroEquipeDAO.create();
                membroEquipeFCVO.set("TIPOMEMBRO", "U");
                membroEquipeFCVO.set("INICIOPARTICIPA", TimeUtils.getNow());
                membroEquipeFCVO.set("NUEQUIPE", equipeVO.asBigDecimal("NUEQUIPE"));
                membroEquipeFCVO.set("CODMEMBRO", this.codigoUsuario);
                DynamicVO save = membroEquipeFCVO.save();

                System.out.println("MEMBRO CRIADO " + save.asBigDecimal("NUEQUIPE").toString() );
            }
        }else if( equipeVO == null ){

            System.out.println("Cadastrando a unidade na equipe " + unidadeFaturamento.toString());

            NativeSql numeroEquipeSQL = new NativeSql(jdbcWrapper);

            numeroEquipeSQL.appendSql("SELECT MAX(NUEQUIPE) NUEQUIPE FROM TCSEQP");
            ResultSet rs = numeroEquipeSQL.executeQuery();
            try{
                if( rs.next() ){
                    numeroEquipe = rs.getBigDecimal("NUEQUIPE");
                }
            }catch(Exception e){
                e.printStackTrace();
            }finally {
                if( numeroEquipeSQL != null ){
                    NativeSql.releaseResources(numeroEquipeSQL);
                }
                rs.close();
            }

            NativeSql dadosUsuarioMgsSQL = new NativeSql(jdbcWrapper);
            dadosUsuarioMgsSQL.appendSql("SELECT ID_USUARIO, " +
                    " ACESSO, " +
                    " UNIDADE, " +
                    " RTRIM( DESCR_UNIDAD ) DESCR_UNIDAD " +
                    " FROM VIEW_USUARIOSPORTAL WHERE ID_USUARIO = :USUARIO AND UNIDADE = :UNIDADE AND ACESSO = :ACESSO");
            dadosUsuarioMgsSQL.setNamedParameter("USUARIO", this.idUsuario);
            dadosUsuarioMgsSQL.setNamedParameter("UNIDADE", unidadeFaturamento );
            dadosUsuarioMgsSQL.setNamedParameter("ACESSO", acesso );

            ResultSet dadosUsuarioMgsRS = dadosUsuarioMgsSQL.executeQuery();

            try{

                if( dadosUsuarioMgsRS.next() ){

                    System.out.println("Criando a equipe na unidade " + unidadeFaturamento.toString() + " descrição " + dadosUsuarioMgsRS.getString("DESCR_UNIDAD"));

                    FluidCreateVO equipeFCVO = equipeDAO.create();
                    equipeFCVO.set("NOME", dadosUsuarioMgsRS.getString("DESCR_UNIDAD"));
                    equipeFCVO.set("TIPOCOORD", String.valueOf("U") );
                    equipeFCVO.set("ATIVA", String.valueOf("S") );
                    equipeFCVO.set("NUEQUIPE", numeroEquipe.add(BigDecimal.ONE) );
                    equipeFCVO.set("AD_UNID_FAT", dadosUsuarioMgsRS.getString("UNIDADE") );
                    equipeFCVO.set("AD_TPACESSODV", dadosUsuarioMgsRS.getString("ACESSO") );
                    DynamicVO equipeNovaVO = equipeFCVO.save();

                    // INSERINDO O MEMBRO NA EQUIPE NOVA

                    System.out.println("EQUIPE NAO EXISTE INSERINDO MEMBRO NA EQUIPE " + equipeNovaVO.asBigDecimal("NUEQUIPE").toString());

                    FluidCreateVO membroEquipeFCVO = this.membroEquipeDAO.create();
                    membroEquipeFCVO.set("TIPOMEMBRO", "U");
                    membroEquipeFCVO.set("INICIOPARTICIPA", TimeUtils.getNow());
                    membroEquipeFCVO.set("NUEQUIPE", equipeNovaVO.asBigDecimal("NUEQUIPE"));
                    membroEquipeFCVO.set("CODMEMBRO", this.codigoUsuario);
                    membroEquipeFCVO.save();

                    System.out.println("FINALIZADO " + equipeNovaVO.asBigDecimal("NUEQUIPE").toString() );

                }
            }catch(Exception e){
                e.printStackTrace();
            }finally {
                if(dadosUsuarioMgsSQL != null){
                    NativeSql.releaseResources(dadosUsuarioMgsSQL);
                }
                dadosUsuarioMgsRS.close();
            }
        }
    }

    /*
    Reunião: 18/08/2021
    Conforme alinhado com o Renato deverá ser implementado a partir da solução proposta pelo Tulio.
    */
    public void removendoMembroEquipe( BigDecimal usuarioPortal, String acao, JdbcWrapper jdbcWrapper ) throws Exception {

        NativeSql verificandoCentroDeCustoSQL = new NativeSql(jdbcWrapper);
        BigDecimal codigoUsuario;

        verificandoCentroDeCustoSQL.appendSql("SELECT\n" +
                "US.USUARIO_PORTAL\n" +
                ", TGFCTT.CODUSU \n" +
                ", US.CENTRO_CUSTO\n" +
                ", US.ACAO\n" +
                "FROM AD_INTEGRAUSURIOPORTAL IU\n" +
                "INNER JOIN PORTALCLIENTE.UNIDADES_TAREFAS_SANKHYA@DLINK_MGS US ON US.USUARIO_PORTAL = IU.USUARIOPORTAL AND US.ACAO = IU.TIPO\n" +
                "INNER JOIN TGFCTT ON TGFCTT.AD_USUARIOPORTAL = IU.USUARIOPORTAL \n" +
                "WHERE\n" +
                "IU.USUARIOPORTAL = :USUARIOPORTAL\n" +
                "AND IU.TIPO = :TIPO \n" +
                "AND US.INTEGRADO_SANKHYA IS NULL");
        verificandoCentroDeCustoSQL.setNamedParameter("USUARIOPORTAL", usuarioPortal );
        verificandoCentroDeCustoSQL.setNamedParameter("TIPO", acao );

        ResultSet verificandoCentroDeCustoRS = verificandoCentroDeCustoSQL.executeQuery();
        try{

            while ( verificandoCentroDeCustoRS.next() ) {

                System.out.println("REMOVENDO ACESSO Centro de custo " + verificandoCentroDeCustoRS.getString("CENTRO_CUSTO"));
                codigoUsuario = verificandoCentroDeCustoRS.getBigDecimal("CODUSU");
                Collection<DynamicVO> equipesVO = this.equipeDAO.find("AD_UNID_FAT = ? "
                        , new Object[]{ verificandoCentroDeCustoRS.getString("CENTRO_CUSTO") });

                if( equipesVO != null ){

                        for(DynamicVO equipeVO : equipesVO ){

                            System.out.println("REMOVENDO EQUIPE " + equipeVO.asBigDecimal("NUEQUIPE").toString()
                                    + " USUARIO " + codigoUsuario.toString() );

                            JapeWrapper membroDAO = JapeFactory.dao("MembroEquipe");
                            DynamicVO membroVO = membroDAO.findOne("NUEQUIPE = ? AND CODMEMBRO = ?"
                                    , new Object[]{ equipeVO.asBigDecimalOrZero("NUEQUIPE"), codigoUsuario });

                            if( membroVO != null ){
                                membroDAO.deleteByCriteria("NUEQUIPE = ? AND CODMEMBRO = ?"
                                        , new Object[]{ equipeVO.asBigDecimal("NUEQUIPE"), codigoUsuario });
                            }

                            System.out.println("ACESSO REMOVIDO " + equipeVO.asBigDecimalOrZero("NUEQUIPE").toString()
                                    + " USUARIO " + codigoUsuario.toString() );
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {
            if( verificandoCentroDeCustoSQL != null ){
                NativeSql.releaseResources(verificandoCentroDeCustoSQL);
            }
            verificandoCentroDeCustoRS.close();

            //Atualizando a tabela UNIDADES_TAREFAS_SANKHYA no schema PORTAL CLIENTE
            atualizandoPortalCliente( usuarioPortal, acao, jdbcWrapper );
        }
    }

    /*
        Reuniao 31/08/2021
        Alinhado com o Tulio para manter as informações chave do TIPO da tabela AD_INTEGRAUSURIOPORTAL com a ACAO da tabela UNIDADE_TAREFAS_SANKHYA
     */

    public void atualizandoMembroEquipe(BigDecimal usuarioPortal, String acao, JdbcWrapper jdbcWrapper ) throws Exception {

        nativeSql = new NativeSql(jdbcWrapper);
        nativeSql.appendSql("SELECT\n" +
                "US.USUARIO_PORTAL\n" +
                ", TGFCTT.CODUSU\n" +
                ", US.CENTRO_CUSTO\n" +
                ", US.ACAO\n" +
                "FROM AD_INTEGRAUSURIOPORTAL IU\n" +
                "INNER JOIN PORTALCLIENTE.UNIDADES_TAREFAS_SANKHYA@DLINK_MGS US ON US.USUARIO_PORTAL = IU.USUARIOPORTAL AND US.ACAO = IU.TIPO\n" +
                "LEFT JOIN TGFCTT ON TGFCTT.AD_USUARIOPORTAL = US.USUARIO_PORTAL\n" +
                "WHERE\n" +
                "IU.USUARIOPORTAL = :USUARIOPORTAL\n" +
                "AND IU.TIPO = :TIPO\n" +
                "AND US.INTEGRADO_SANKHYA IS NULL");
        nativeSql.setNamedParameter("USUARIOPORTAL", usuarioPortal );
        nativeSql.setNamedParameter("TIPO", acao );
        ResultSet rs = nativeSql.executeQuery();
        try{
            while ( rs.next() ) {

                NativeSql verificandoAcessoCentroDeCustoSQL = new NativeSql(jdbcWrapper);
                verificandoAcessoCentroDeCustoSQL.appendSql("SELECT ACESSO, UNIDADE FROM VIEW_USUARIOSPORTAL WHERE ID_USUARIO = :ID_USUARIO AND UNIDADE = :UNIDADE");
                verificandoAcessoCentroDeCustoSQL.setNamedParameter("ID_USUARIO", usuarioPortal );
                verificandoAcessoCentroDeCustoSQL.setNamedParameter("UNIDADE", rs.getBigDecimal("CENTRO_CUSTO"));
                ResultSet verificandoAcessoCentroDeCustoRS = verificandoAcessoCentroDeCustoSQL.executeQuery();

                try{

                    while( verificandoAcessoCentroDeCustoRS.next() ){
                        String acesso = verificandoAcessoCentroDeCustoRS.getString("ACESSO");
                        BigDecimal unidade = verificandoAcessoCentroDeCustoRS.getBigDecimal("UNIDADE");
                        this.codigoUsuario = rs.getBigDecimal("CODUSU");
                        this.ingressarEquipe( unidade, acesso, jdbcWrapper );
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }finally {
                    if( verificandoAcessoCentroDeCustoSQL != null ){
                        NativeSql.releaseResources(verificandoAcessoCentroDeCustoSQL);
                    }
                    verificandoAcessoCentroDeCustoRS.close();
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }finally {

            if( nativeSql != null ){
                NativeSql.releaseResources(nativeSql);
            }

            rs.close();

            //Atualizando a tabela UNIDADES_TAREFAS_SANKHYA no schema PORTAL CLIENTE
            atualizandoPortalCliente( usuarioPortal, acao, jdbcWrapper );
        }
    }

    public void atualizandoPortalCliente( BigDecimal usuarioPortal, String acao, JdbcWrapper jdbcWrapper ) throws Exception{

        NativeSql atualizandoPortalClienteSQL = new NativeSql(jdbcWrapper);

        atualizandoPortalClienteSQL.appendSql("UPDATE PORTALCLIENTE.UNIDADES_TAREFAS_SANKHYA@DLINK_MGS\n" +
                "SET INTEGRADO_SANKHYA = 'S'\n" +
                "WHERE\n" +
                "INTEGRADO_SANKHYA IS NULL AND \n" +
                "USUARIO_PORTAL = :USUARIO_PORTAL\n" +
                "AND ACAO = :ACAO");
        atualizandoPortalClienteSQL.setNamedParameter("USUARIO_PORTAL", usuarioPortal );
        atualizandoPortalClienteSQL.setNamedParameter("ACAO", acao );
        atualizandoPortalClienteSQL.executeUpdate();
    }
}
