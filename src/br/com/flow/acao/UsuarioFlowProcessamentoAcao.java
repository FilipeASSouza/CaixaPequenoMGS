package br.com.flow.acao;

import br.com.flow.agendamento.UsuarioFlowPortalModel;
import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

import java.sql.ResultSet;

public class UsuarioFlowProcessamentoAcao implements AcaoRotinaJava {

    private JdbcWrapper jdbcWrapper = EntityFacadeFactory.getCoreFacade().getJdbcWrapper();
    private NativeSql nativeSql = null;

    @Override
    public void doAction(ContextoAcao contextoAcao) {
        this.processar();
    }

    private void processar() {
        try {
            UsuarioFlowPortalModel usuarioFlowPortalModel = new UsuarioFlowPortalModel();
            nativeSql = new NativeSql(jdbcWrapper);
            nativeSql.appendSql("SELECT NUNICO, USUARIOPORTAL, TIPO FROM AD_INTEGRAUSURIOPORTAL WHERE ROWNUM <= 1 ORDER BY DHLANC");
            ResultSet rs = nativeSql.executeQuery();

            while(rs.next()) {

                /*
                Reunião: 10/08/2021
                implementar na terça feira 10/08/2021
                */

                if( rs.getString("TIPO").equalsIgnoreCase("I")){
                    usuarioFlowPortalModel.processarAcesso(rs.getBigDecimal("USUARIOPORTAL"),
                            rs.getString("TIPO"), jdbcWrapper );
                }else if( rs.getString("TIPO").equalsIgnoreCase("D") ){
                    usuarioFlowPortalModel.removendoMembroEquipe(rs.getBigDecimal("USUARIOPORTAL")
                            , rs.getString("TIPO"), jdbcWrapper );
                }else if( rs.getString("TIPO").equalsIgnoreCase("U") ){
                    usuarioFlowPortalModel.atualizandoMembroEquipe( rs.getBigDecimal("USUARIOPORTAL")
                            , rs.getString("TIPO"), jdbcWrapper );
                }

                //Excluindo o registro da tabela de integração
                JapeFactory.dao("AD_INTEGRAUSURIOPORTAL").deleteByCriteria("NUNICO = ?"
                        , new Object[]{ rs.getBigDecimal("NUNICO") });
            }
            rs.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if( nativeSql != null ){
                NativeSql.releaseResources(nativeSql);
            }
            jdbcWrapper.closeSession();
        }
    }
}
