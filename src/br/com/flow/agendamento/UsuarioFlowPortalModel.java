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

    public UsuarioFlowPortalModel() throws Exception {
        this.codigoGrupoUsuario = JapeFactory.dao("ParametroSistema").findByPK(new Object[]{"AD_GRUFLOWPORT", BigDecimal.ZERO}).asBigDecimal("INTEIRO");
        this.codigoParceiro = JapeFactory.dao("ParametroSistema").findByPK(new Object[]{"AD_PARFLOWPORT", BigDecimal.ZERO}).asBigDecimal("INTEIRO");
    }

    public void processarAcesso(BigDecimal idUsuario) throws Exception {
        this.idUsuario = idUsuario;
        NativeSqlDecorator nativeSqlDecorator = new NativeSqlDecorator("SELECT ACESSO, COD_PARC, UNIDADE FROM VIEW_USUARIOS_PORTALMGS WHERE ID_USUARIO = :ID_USUARIO");
        nativeSqlDecorator.setParametro("ID_USUARIO", idUsuario);

        while(nativeSqlDecorator.proximo()) {
            String acesso = nativeSqlDecorator.getValorString("ACESSO");
            BigDecimal unidade = nativeSqlDecorator.getValorBigDecimal("UNIDADE");
            this.criarUsuario();
            this.criarContato();
            this.ingressarEquipe(unidade, acesso);
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
            usuarioFCVO.save();
        }

    }

    private void ingressarEquipe(BigDecimal unidaadeFaturamento, String acesso) throws Exception {
        DynamicVO equipeVO = this.equipeDAO.findOne("AD_UNID_FAT = ? AND AD_TPACESSODV = ?", new Object[]{unidaadeFaturamento, acesso});
        if (equipeVO != null) {
            DynamicVO membroEquipeVO = this.membroEquipeDAO.findOne("NUEQUIPE = ?  AND CODMEMBRO = ?", new Object[]{equipeVO.asBigDecimal("NUEQUIPE"), this.codigoUsuario});
            if (membroEquipeVO == null) {
                FluidCreateVO membroEquipeFCVO = this.membroEquipeDAO.create();
                membroEquipeFCVO.set("TIPOMEMBRO", "U");
                membroEquipeFCVO.set("INICIOPARTICIPA", TimeUtils.getNow());
                membroEquipeFCVO.set("NUEQUIPE", equipeVO.asBigDecimal("NUEQUIPE"));
                membroEquipeFCVO.set("CODMEMBRO", this.codigoUsuario);
                membroEquipeFCVO.save();
            }
        }else if( equipeVO == null ){
            BigDecimal numeroEquipe = null;

            NativeSqlDecorator numeroEquipeSQL = new NativeSqlDecorator("SELECT MAX(NUEQUIPE) NUEQUIPE FROM TCSEQP");
            if( numeroEquipeSQL.proximo() ){
                numeroEquipe = numeroEquipeSQL.getValorBigDecimal("NUEQUIPE");
            }

            NativeSqlDecorator dadosUsuarioMgs = new NativeSqlDecorator("SELECT ID_USUARIO, " +
                    " LOGIN, " +
                    " ACESSO, " +
                    " COD_PARC, " +
                    " UNIDADE, " +
                    " DESCR_UNIDADE, " +
                    " EMAIL " +
                    " FROM VIEW_USUARIOS_PORTALMGS WHERE ID_USUARIO = :USUARIO");
            dadosUsuarioMgs.setParametro("USUARIO", this.idUsuario);

            if(dadosUsuarioMgs.proximo()){

                FluidCreateVO equipeFCVO = equipeDAO.create();
                equipeFCVO.set("NOME", dadosUsuarioMgs.getValorString("DESCR_UNIDADE"));
                equipeFCVO.set("TIPOCOORD", String.valueOf("U"));
                equipeFCVO.set("ATIVA", String.valueOf("S"));
                equipeFCVO.set("NUEQUIPE", numeroEquipe.add(BigDecimal.ONE));
                equipeFCVO.set("AD_UNID_FAT", dadosUsuarioMgs.getValorString("UNIDADE"));
                equipeFCVO.set("AD_TPACESSODV", dadosUsuarioMgs.getValorString("ACESSO"));
                DynamicVO equipeNovaVO = equipeFCVO.save();

                DynamicVO membroEquipeVO = this.membroEquipeDAO.findOne("NUEQUIPE = ?  AND CODMEMBRO = ?", new Object[]{equipeNovaVO.asBigDecimal("NUEQUIPE"), this.codigoUsuario});
                if (membroEquipeVO == null) {
                    FluidCreateVO membroEquipeFCVO = this.membroEquipeDAO.create();
                    membroEquipeFCVO.set("TIPOMEMBRO", "U");
                    membroEquipeFCVO.set("INICIOPARTICIPA", TimeUtils.getNow());
                    membroEquipeFCVO.set("NUEQUIPE", equipeNovaVO.asBigDecimal("NUEQUIPE"));
                    membroEquipeFCVO.set("CODMEMBRO", this.codigoUsuario);
                    membroEquipeFCVO.save();
                }
            }
        }
    }
}
