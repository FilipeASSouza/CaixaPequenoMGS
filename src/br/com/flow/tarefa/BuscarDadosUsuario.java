package br.com.flow.tarefa;

import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.util.ErroUtils;
import br.com.util.NativeSqlDecorator;
import br.com.util.VariaveisFlow;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class BuscarDadosUsuario implements TarefaJava {

    public BuscarDadosUsuario() {
    }

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        BigDecimal usuarioLogado = contextoTarefa.getUsuarioLogado();
        Object idInstanceProcesso = contextoTarefa.getIdInstanceProcesso();

        QueryExecutor consultarUsuario = contextoTarefa.getQuery();
        consultarUsuario.setParam("CODUSU", usuarioLogado);
        consultarUsuario.nativeSelect("SELECT NVL( V.EMAIL, USU.EMAIL ) EMAIL FROM TSIUSU USU LEFT JOIN VIEW_CAIXAPQ V ON USU.CODUSU = V.CODUSU WHERE USU.CODUSU = {CODUSU} AND ROWNUM <= 1");

        if (consultarUsuario.next()) {
            String email = consultarUsuario.getString("EMAIL") == null ? "" : consultarUsuario.getString("EMAIL");

            //buscando o acesso
            JapeWrapper acessoCPQDAO = JapeFactory.dao("AD_ACESSOCPQ");
            DynamicVO acessoCPQVO = acessoCPQDAO.findOne("CODUSU = ?", new Object[]{usuarioLogado});
            if(acessoCPQVO == null){
                throw new Exception("Usuário não possue o acesso para realizar lançamento, gentileza entrar em contato com o Financeiro MGS!");
            }

            //consultando a unidade
            JapeWrapper lotacaoDAO = JapeFactory.dao("TGFLOT");
            DynamicVO lotacaoVO = lotacaoDAO.findOne("CODLOT = ?", new Object[]{acessoCPQVO.asBigDecimal("CODLOT")});

            BigDecimal unidade = lotacaoVO.asBigDecimal("CODSITE");

            try{
                JapeWrapper parametrosCPDAO = JapeFactory.dao("AD_PARAMCP");
                DynamicVO parametrosVO = parametrosCPDAO.findOne("PARAMETRO = ?", new Object[]{"EMAIL"});

                String[] emails = parametrosVO.asString("VALOR").split(";");

                VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILUSU", String.valueOf(email));
                VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILTES", emails[0]);
                VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILCOT", emails[1]);
            }catch (Exception e){
                ErroUtils.disparaErro(VariaveisFlow.MENSAGEM_PARAMETRO);
                e.printStackTrace();
            }
            if(unidade == null){
                unidade = BigDecimal.ZERO;
            }
            VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "UNID_FATURAMENTO", unidade);
        }
        consultarUsuario.close();
    }
}
