package br.com.flow.agendamento;

import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.JapeSessionContext;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import com.sankhya.util.TimeUtils;
import org.cuckoo.core.ScheduledAction;
import org.cuckoo.core.ScheduledActionContext;
import org.springframework.jms.IllegalStateException;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class UsuarioFlowProcessamento implements ScheduledAction{

    private JdbcWrapper jdbcWrapper = EntityFacadeFactory.getCoreFacade().getJdbcWrapper();
    private NativeSql nativeSql = null;

    public void onTime(ScheduledActionContext scheduledActionContext) {
        try{
            this.setup();
            this.processar();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public void setup() {
        JapeSessionContext.putProperty("usuario_logado", BigDecimal.ZERO);
        JapeSessionContext.putProperty("emp_usu_logado", BigDecimal.ZERO);
        JapeSessionContext.putProperty("dh_atual", new Timestamp(System.currentTimeMillis()));
        JapeSessionContext.putProperty("d_atual", new Timestamp(TimeUtils.getToday()));
    }

    private void processar() {
        try {
            UsuarioFlowPortalModel usuarioFlowPortalModel = new UsuarioFlowPortalModel();
            nativeSql = new NativeSql(jdbcWrapper);
            nativeSql.appendSql("SELECT NUNICO, USUARIOPORTAL, TIPO FROM AD_INTEGRAUSURIOPORTAL WHERE ROWNUM <= 1 ORDER BY DHLANC");

            ResultSet rs;
            for(rs = this.nativeSql.executeQuery(); rs.next(); JapeFactory.dao("AD_INTEGRAUSURIOPORTAL").deleteByCriteria("NUNICO = ?", new Object[]{rs.getBigDecimal("NUNICO")})) {
                if (rs.getString("TIPO").equalsIgnoreCase("I")) {
                    usuarioFlowPortalModel.processarAcesso(rs.getBigDecimal("USUARIOPORTAL"), rs.getString("TIPO"), this.jdbcWrapper);
                } else if (rs.getString("TIPO").equalsIgnoreCase("D")) {
                    usuarioFlowPortalModel.removendoMembroEquipe(rs.getBigDecimal("USUARIOPORTAL"), rs.getString("TIPO"), this.jdbcWrapper);
                } else if (rs.getString("TIPO").equalsIgnoreCase("U")) {
                    usuarioFlowPortalModel.atualizandoMembroEquipe(rs.getBigDecimal("USUARIOPORTAL"), rs.getString("TIPO"), this.jdbcWrapper);
                }
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
