package br.com.flow.agendamento;

import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;

public class DisparoEmailCaixaPequenoProcessamento {

    private JapeWrapper filaDAO = JapeFactory.dao("MSDFilaMensagem");
    private JapeWrapper controleNumeracaoDAO = JapeFactory.dao("ControleNumeracao");

    public void processarEmail(ResultSet rs, NativeSql consultarCadastroEmailSQL) throws Exception {

        DynamicVO ultimoNumeroVO = controleNumeracaoDAO.findOne("ARQUIVO = ?"
                , new Object[]{String.valueOf("TMDFMG")});
        char[] texto;
        Timestamp dataSolicitacao = new Timestamp(TimeUtils.getToday());

        for(rs = consultarCadastroEmailSQL.executeQuery(); rs.next();){
            StringBuilder mensagem = new StringBuilder("Prezados(a), bom dia!<br \\>"+
                    "Gentileza informar o caixa zerado ou registrar os lançamentos no sistema!<br \\>"+
                    "<br \\>"+
                    "Atenciosamente<br \\>"+
                    "Coordenadoria de Tesouraria<br \\>"+
                    "tesouraria@mgs.srv.br<br \\>");
            texto = new char[mensagem.length()];
            for(int i = 0; i < mensagem.length(); i++){
                texto[i] = mensagem.charAt(i);
            }

            BigDecimal codigoFila = ultimoNumeroVO.asBigDecimalOrZero("ULTCOD").add(BigDecimal.ONE);
            FluidCreateVO filaFCVO = filaDAO.create();
            filaFCVO.set("CODFILA", codigoFila );
            filaFCVO.set("CODMSG", null );
            filaFCVO.set("DTENTRADA", dataSolicitacao );
            filaFCVO.set("STATUS", "Pendente");
            filaFCVO.set("CODCON", BigDecimal.ZERO );
            filaFCVO.set("TENTENVIO", BigDecimal.ZERO );
            filaFCVO.set("MENSAGEM", texto );
            filaFCVO.set("TIPOENVIO", "E" );
            filaFCVO.set("MAXTENTENVIO", BigDecimal.valueOf(3L) );
            filaFCVO.set("ASSUNTO", "Lançamentos do Caixa Pequeno ");
            filaFCVO.set("EMAIL", "tesouraria@mgs.srv.br" );
            filaFCVO.set("CODUSU", BigDecimal.ZERO );
            filaFCVO.set("REENVIAR", "N" );
            filaFCVO.set("CODSMTP", BigDecimal.valueOf(4L) );
            filaFCVO.set("DHULTTENTA", dataSolicitacao );
            filaFCVO.set("DBHASHCODE", "29c0e113827a441024f5c71836fdd6eaea9b9410" );
            filaFCVO.save();

            FluidUpdateVO controleNumeracaoFUVO = controleNumeracaoDAO.prepareToUpdate( ultimoNumeroVO );
            controleNumeracaoFUVO.set("ULTCOD", codigoFila );
            controleNumeracaoFUVO.update();
        }
    }
}
