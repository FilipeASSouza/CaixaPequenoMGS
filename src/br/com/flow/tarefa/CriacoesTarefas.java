package br.com.flow.tarefa;

import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.util.NativeSqlDecorator;

import java.math.BigDecimal;
import java.util.Collection;

public class CriacoesTarefas implements TarefaJava {
    @Override
    public void executar(ContextoTarefa ct) throws Exception {

        JapeWrapper lancamentosDAO = JapeFactory.dao("AD_FINCAIXAPQ");
        DynamicVO lancamentosVO = lancamentosDAO.findOne("IDINSTPRN = ?"
                , new Object[]{ct.getIdInstanceProcesso().toString()});

        JapeWrapper usuariosPreferenciaisDAO = JapeFactory.dao("AD_USUARIOSPREFCPQ");
        Collection<DynamicVO> usuariosPreferenciaisVO = usuariosPreferenciaisDAO.find("CODUSU <> ?"
                , new Object[]{lancamentosVO.asBigDecimal("CODUSUARIO")});

        for(DynamicVO usuario : usuariosPreferenciaisVO){

            JapeWrapper tarefasDAO = JapeFactory.dao("InstanciaTarefa");
            Collection<DynamicVO> tarefasVO = tarefasDAO.find("IDINSTPRN = ? AND ROWNUM <= 2"
                    , new Object[]{ct.getIdInstanceProcesso().toString()});

            for(DynamicVO tarefa : tarefasVO){

                //consultando o número da ultima tarefa
                NativeSqlDecorator ultimaTarefaSQL = new NativeSqlDecorator("SELECT MAX(IDINSTTAR) IDINSTTAR FROM TWFITAR");
                BigDecimal numeroUltimaTarefa = null;
                if( ultimaTarefaSQL.proximo() ){
                    numeroUltimaTarefa = ultimaTarefaSQL.getValorBigDecimal("IDINSTTAR");
                }

                //espelhando as tarefas
                FluidCreateVO tarefasFCVO = tarefasDAO.create();
                tarefasFCVO.set("IDINSTPRN", BigDecimal.valueOf(Long.parseLong(ct.getIdInstanceProcesso().toString())));
                tarefasFCVO.set("IDINSTTAR", numeroUltimaTarefa.add(BigDecimal.ONE) );
                tarefasFCVO.set("IDELEMENTO", tarefa.asString("IDELEMENTO"));
                tarefasFCVO.set("DHCRIACAO", tarefa.asTimestamp("DHCRIACAO"));
                tarefasFCVO.set("DHACEITE", tarefa.asTimestamp("DHACEITE"));
                tarefasFCVO.set("DHCONCLUSAO", tarefa.asTimestamp("DHCONCLUSAO"));
                tarefasFCVO.set("CODUSUDONO", usuario.asBigDecimal("CODUSU"));
                tarefasFCVO.set("SITUACAOEXEC", tarefa.asString("SITUACAOEXEC"));
                tarefasFCVO.set("CODUSUSOLICITANTE", tarefa.asBigDecimal("CODUSUSOLICITANTE"));
                tarefasFCVO.save();
            }
        }
    }
}
