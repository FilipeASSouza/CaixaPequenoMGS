package br.com.flow.agendamento;

import br.com.util.NativeSqlDecorator;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import com.sankhya.util.StringUtils;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.security.MessageDigest;

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

    public UsuarioFlowPortalModel() throws Exception {
        this.codigoGrupoUsuario = JapeFactory.dao("ParametroSistema").findByPK(new Object[]{"AD_GRUFLOWPORT", BigDecimal.ZERO}).asBigDecimal("INTEIRO");
        this.codigoParceiro = JapeFactory.dao("ParametroSistema").findByPK(new Object[]{"AD_PARFLOWPORT", BigDecimal.ZERO}).asBigDecimal("INTEIRO");
    }

    public void processarAcesso(BigDecimal idUsuario) throws Exception {
        this.idUsuario = idUsuario;
        NativeSqlDecorator nativeSqlDecorator = new NativeSqlDecorator("SELECT ACESSO, UNIDADE FROM VIEW_USUARIOSPORTAL WHERE ID_USUARIO = :ID_USUARIO");
        nativeSqlDecorator.setParametro("ID_USUARIO", idUsuario);

        this.criarUsuario();
        this.criarContato();

        while(nativeSqlDecorator.proximo()) {
            this.acesso = nativeSqlDecorator.getValorString("ACESSO");
            BigDecimal unidade = nativeSqlDecorator.getValorBigDecimal("UNIDADE");
            this.ingressarEquipe(unidade, this.acesso);
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

    private void ingressarEquipe(BigDecimal unidaadeFaturamento, String acesso) throws Exception {
        DynamicVO equipeVO = this.equipeDAO.findOne("AD_UNID_FAT = ? AND AD_TPACESSODV = ?", new Object[]{unidaadeFaturamento, acesso});
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

            System.out.println("Cadastrando a unidade na equipe " + unidaadeFaturamento.toString());

            NativeSqlDecorator numeroEquipeSQL = new NativeSqlDecorator("SELECT MAX(NUEQUIPE) NUEQUIPE FROM TCSEQP");
            if( numeroEquipeSQL.proximo() ){
                numeroEquipe = numeroEquipeSQL.getValorBigDecimal("NUEQUIPE");
            }

            NativeSqlDecorator dadosUsuarioMgs = new NativeSqlDecorator("SELECT ID_USUARIO, " +
                    " ACESSO, " +
                    " UNIDADE, " +
                    " RTRIM( DESCR_UNIDAD ) DESCR_UNIDAD " +
                    " FROM VIEW_USUARIOSPORTAL WHERE ID_USUARIO = :USUARIO AND UNIDADE = :UNIDADE AND ACESSO = :ACESSO");
            dadosUsuarioMgs.setParametro("USUARIO", this.idUsuario);
            dadosUsuarioMgs.setParametro("UNIDADE", unidaadeFaturamento );
            dadosUsuarioMgs.setParametro("ACESSO", acesso );

            if( dadosUsuarioMgs.proximo() ){

                System.out.println("Criando a equipe na unidade " + unidaadeFaturamento.toString() + " descrição " + dadosUsuarioMgs.getValorString("DESCR_UNIDAD"));

                FluidCreateVO equipeFCVO = equipeDAO.create();
                equipeFCVO.set("NOME", dadosUsuarioMgs.getValorString("DESCR_UNIDAD"));
                equipeFCVO.set("TIPOCOORD", String.valueOf("U") );
                equipeFCVO.set("ATIVA", String.valueOf("S") );
                equipeFCVO.set("NUEQUIPE", numeroEquipe.add(BigDecimal.ONE) );
                equipeFCVO.set("AD_UNID_FAT", dadosUsuarioMgs.getValorString("UNIDADE") );
                equipeFCVO.set("AD_TPACESSODV", dadosUsuarioMgs.getValorString("ACESSO") );
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
        }
    }

    /*
    Reunião: 18/08/2021
    Conforme alinhado com o Renato deverá ser implementado a partir da solução proposta pelo Tulio.

    public void removendoMembroEquipe( BigDecimal usuarioPortal ) throws Exception {

        NativeSqlDecorator verificandoEquipesSQL = new NativeSqlDecorator("SELECT\n" +
                "DISTINCT EQP.NUEQUIPE\n" +
                ", MEQ.SEQUENCIA\n" +
                "FROM TCSEQP EQP\n" +
                "INNER JOIN TCSMEQ MEQ ON EQP.NUEQUIPE = MEQ.NUEQUIPE\n" +
                "INNER JOIN TGFCTT CTT ON CTT.CODUSU = MEQ.CODMEMBRO AND CTT.CODPARC = 9999\n" +
                "WHERE\n" +
                "CTT.AD_USUARIOPORTAL = :CODUSUARIOPORTAL\n" +
                "AND NOT EXISTS( SELECT\n" +
                "          1\n" +
                "          FROM VIEW_USUARIOSPORTAL UP\n" +
                "          WHERE\n" +
                "          UP.UNIDADE = EQP.AD_UNID_FAT\n" +
                "          AND UP.ID_USUARIO = CTT.AD_USUARIOPORTAL )");


        verificandoEquipesSQL.setParametro("CODUSUARIOPORTAL", usuarioPortal );
        while( verificandoEquipesSQL.proximo() ){

            System.out.println("REMOVENDO ACESSO MembroEquipe " + verificandoEquipesSQL.getValorBigDecimal("NUEQUIPE").toString() );

            JapeWrapper membroDAO = JapeFactory.dao("MembroEquipe");
            membroDAO.deleteByCriteria("NUEQUIPE = ? AND SEQUENCIA = ? "
                    , new Object[]{ verificandoEquipesSQL.getValorBigDecimal("NUEQUIPE")
                            , verificandoEquipesSQL.getValorBigDecimal("SEQUENCIA")} );

            System.out.println("ACESSO REMOVIDO " + verificandoEquipesSQL.getValorBigDecimal("NUEQUIPE").toString() );
        }    */
}
