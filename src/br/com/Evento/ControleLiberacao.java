package br.com.Evento;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;

public class ControleLiberacao implements EventoProgramavelJava {
    public ControleLiberacao() {
    }

    public static void iniciaFila(BigDecimal numeroUnico, BigDecimal valorLiberacao, BigDecimal userID, String origemIniciaFila) throws Exception {
        inserefila(numeroUnico, valorLiberacao, userID, BigDecimal.ONE, origemIniciaFila);
    }

    public static void inserefila(BigDecimal numeroUnico, BigDecimal valorLiberacao, BigDecimal userID, BigDecimal ordem, String origemIniciaFila) throws Exception {
        BigDecimal topPagamento = JapeFactory.dao("ParametroSistema").findByPK(new Object[]{"TOPPAGEVETO1000", BigDecimal.ZERO}).asBigDecimal("INTEIRO");
        JapeWrapper liberacaoCascataDAO = JapeFactory.dao("LiberacaoCascata");
        Collection<DynamicVO> listaLiberacaoCascataVO = liberacaoCascataDAO.find("CODTIPOPER = ? AND EVENTO = 1000 AND ORDEM = ? AND ? BETWEEN VLRMIN AND VLRMAX", new Object[]{topPagamento, ordem, valorLiberacao});
        JapeWrapper liberacaoLimiteDAO = JapeFactory.dao("LiberacaoLimite");
        int seq = 1;
        Iterator var10 = listaLiberacaoCascataVO.iterator();

        while(var10.hasNext()) {
            DynamicVO liberacaoCascataVO = (DynamicVO)var10.next();
            ((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)((FluidCreateVO)liberacaoLimiteDAO.create().set("NUCHAVE", numeroUnico)).set("TABELA", "TGFFIN")).set("EVENTO", BigDecimal.valueOf(1000L))).set("CODUSUSOLICIT", userID)).set("DHSOLICIT", TimeUtils.getNow())).set("VLRLIMITE", valorLiberacao)).set("VLRATUAL", valorLiberacao)).set("SEQCASCATA", ordem)).set("SEQUENCIA", BigDecimal.valueOf((long)(seq++)))).set("NUCLL", BigDecimal.ZERO)).set("ORDEM", ordem)).set("CODUSULIB", liberacaoCascataVO.asBigDecimal("CODUSULIB"))).set("OBSERVACAO", origemIniciaFila)).save();
        }

    }

    public static void limpaFila(BigDecimal numeroUnico) throws Exception {
        JapeWrapper liberacaoLimiteDAO = JapeFactory.dao("LiberacaoLimite");
        liberacaoLimiteDAO.deleteByCriteria("NUCHAVE = ? AND TABELA = 'TGFFIN' AND  EVENTO = 1000 AND DHLIB IS NULL", new Object[]{numeroUnico});
    }

    public static void excluiFila(BigDecimal numeroUnico) throws Exception {
        JapeWrapper liberacaoLimiteDAO = JapeFactory.dao("LiberacaoLimite");
        liberacaoLimiteDAO.deleteByCriteria("NUCHAVE = ? AND TABELA = 'TGFFIN' AND  EVENTO = 1000", new Object[]{numeroUnico});
    }

    public static void autorizaFinanceiro(BigDecimal numeroUnico) throws Exception {
        JapeWrapper financeiroDAO = JapeFactory.dao("Financeiro");
        DynamicVO financeiroVO = financeiroDAO.findByPK(new Object[]{numeroUnico});
        FluidUpdateVO financeiroUpdateVO = financeiroDAO.prepareToUpdate(financeiroVO);
        financeiroUpdateVO.set("AD_PAGAMENTOAUTORIZADO", "S");
        financeiroUpdateVO.update();
    }

    public static void atualizaliberacao(BigDecimal numeroUnico) throws Exception {
        JapeWrapper liberacaoLimiteDAO = JapeFactory.dao("LiberacaoLimite");
        JapeWrapper financeiroDAO = JapeFactory.dao("Financeiro");
        DynamicVO libVO = liberacaoLimiteDAO.findOne("NUCHAVE=?", new Object[]{numeroUnico});
        DynamicVO finVO = financeiroDAO.findOne("NUFIN=?", new Object[]{numeroUnico});
        if (finVO != null) {
            FluidUpdateVO lib = liberacaoLimiteDAO.prepareToUpdate(libVO);
            lib.set("VLRLIMITE", finVO.asBigDecimal("VLRDESDOB"));
            lib.set("VLRATUAL", finVO.asBigDecimal("VLRDESDOB"));
            lib.update();
        }

    }

    public void beforeInsert(PersistenceEvent persistenceEvent) throws Exception {
    }

    public void beforeUpdate(PersistenceEvent persistenceEvent) throws Exception {
    }

    public void beforeDelete(PersistenceEvent persistenceEvent) throws Exception {
    }

    public void afterInsert(PersistenceEvent persistenceEvent) throws Exception {
    }

    public void afterUpdate(PersistenceEvent persistenceEvent) throws Exception {
        DynamicVO liberacaoVO = (DynamicVO)persistenceEvent.getVo();
        if (liberacaoVO.asInt("EVENTO") == 1000) {
            JapeWrapper liberacaoLimiteDAO = JapeFactory.dao("LiberacaoLimite");
            if (liberacaoVO.asTimestamp("DHLIB") != null) {
                DynamicVO liberacaoPendenteVO = liberacaoLimiteDAO.findOne("NUCHAVE = ? AND TABELA = ? AND EVENTO = ? AND DHLIB IS NULL", new Object[]{liberacaoVO.asBigDecimal("NUCHAVE"), liberacaoVO.asString("TABELA"), liberacaoVO.asBigDecimal("EVENTO")});
                if (liberacaoPendenteVO != null) {
                    limpaFila(liberacaoVO.asBigDecimal("NUCHAVE"));
                }

                if (!liberacaoVO.asString("REPROVADO").equals("S")) {
                    inserefila(liberacaoVO.asBigDecimal("NUCHAVE"), liberacaoVO.asBigDecimal("VLRATUAL"), liberacaoVO.asBigDecimal("CODUSUSOLICIT"), liberacaoVO.asBigDecimal("ORDEM").add(BigDecimal.ONE), "Valida Fila");
                }

                liberacaoPendenteVO = liberacaoLimiteDAO.findOne("NUCHAVE = ? AND TABELA = ? AND EVENTO = ? AND DHLIB IS NULL", new Object[]{liberacaoVO.asBigDecimal("NUCHAVE"), liberacaoVO.asString("TABELA"), liberacaoVO.asBigDecimal("EVENTO")});
                if (liberacaoPendenteVO == null) {
                    autorizaFinanceiro(liberacaoVO.asBigDecimal("NUCHAVE"));
                }
            }
        }

    }

    public void afterDelete(PersistenceEvent persistenceEvent) throws Exception {
    }

    public void beforeCommit(TransactionContext transactionContext) throws Exception {
    }
}
