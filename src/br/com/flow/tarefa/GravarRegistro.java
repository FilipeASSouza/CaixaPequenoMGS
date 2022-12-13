package br.com.flow.tarefa;

import br.com.sankhya.extensions.actionbutton.QueryExecutor;
import br.com.sankhya.extensions.flow.ContextoTarefa;
import br.com.sankhya.extensions.flow.TarefaJava;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.util.ErroUtils;
import br.com.util.VariaveisFlow;
import com.sankhya.util.TimeUtils;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;

public class GravarRegistro implements TarefaJava {

    private static JapeWrapper rateioFlow = JapeFactory.dao("AD_RATEIOCPQ");

    public void executar(ContextoTarefa contextoTarefa) throws Exception {
        if(contextoTarefa.getCampo("TIPOPER").toString().equalsIgnoreCase("M")){
            validarLimiteAquisicao(contextoTarefa, "LIMITEMGS");
            validarCompraRecorrentes(contextoTarefa, "COMPRASMGS");
        } else {
            validarLimiteAquisicao(contextoTarefa, "LIMITESEPLAG");
            validarCompraRecorrentes(contextoTarefa, "COMPRASSEPLAG");
        }
        validarRateio(contextoTarefa);
        gravarRegistro(contextoTarefa);
    }

    private void validarRateio(ContextoTarefa contextoTarefa) throws Exception {

        BigDecimal resultadoPercentualRateio = BigDecimal.ZERO;
        Collection<DynamicVO> rateiosVO = rateioFlow.find("IDINSTPRN = ?", new Object[]{contextoTarefa.getIdInstanceProcesso()});
        for (DynamicVO rateio : rateiosVO){
            resultadoPercentualRateio = resultadoPercentualRateio.add(rateio.asBigDecimal("PERCRATEIO"));
        }
        if(resultadoPercentualRateio.compareTo(BigDecimal.valueOf(100L)) < 0){
            ErroUtils.disparaErro("Rateio incorreto, fineza verificar!");
        }
    }

    private void validarLimiteAquisicao(ContextoTarefa contextoTarefa, String parametro) throws Exception {

        Object idInstanceProcesso = contextoTarefa.getIdInstanceProcesso();
        BigDecimal valorTotal = new BigDecimal(Double.valueOf(contextoTarefa.getCampo("VLRTOT").toString()));
        BigDecimal valorDesconto = contextoTarefa.getCampo("VLRDESCTOT") == "" || contextoTarefa.getCampo("VLRDESCTOT") == null
                ? BigDecimal.ZERO : new BigDecimal(contextoTarefa.getCampo("VLRDESCTOT").toString());
        BigDecimal valorTotalLiquido = valorTotal.subtract(valorDesconto);
        String status = null;

        BigDecimal parametroAquisicao = null;

        QueryExecutor consultaParametro = contextoTarefa.getQuery();
        consultaParametro.setParam("PARAM", parametro);
        consultaParametro.nativeSelect("SELECT * FROM AD_PARAMCP WHERE PARAMETRO = {PARAM}");

        if (consultaParametro.next()) {
            parametroAquisicao = new BigDecimal(Long.parseLong(consultaParametro.getString("VALOR")));
            status = consultaParametro.getString("STATUS");
        }

        if (status.equalsIgnoreCase("2")){
            if (parametroAquisicao.compareTo(valorTotalLiquido) < 0){
                String emailDiretor = null;

                QueryExecutor consultaEmailDiretoria = contextoTarefa.getQuery();
                consultaEmailDiretoria.setParam("PARAM", "EMAILDIRETOR");
                consultaEmailDiretoria.nativeSelect("SELECT * FROM AD_PARAMCP WHERE PARAMETRO = {PARAM}");
                if (consultaEmailDiretoria.next()){
                    emailDiretor = consultaEmailDiretoria.getString("VALOR");
                }

                if (emailDiretor == null){
                    ErroUtils.disparaErro(VariaveisFlow.MENSAGEM_PARAMETRO);
                }

                VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "STATUSLIMITE", "2");
                VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "EMAILDIRETORIA", emailDiretor);
            } else {
                VariaveisFlow.setVariavel(new BigDecimal(idInstanceProcesso.toString()), BigDecimal.ZERO, "STATUSLIMITE", "1");
            }
        } else if (consultaParametro == null) {
            ErroUtils.disparaErro(VariaveisFlow.MENSAGEM_PARAMETRO);
        }
    }

    private void validarCompraRecorrentes(ContextoTarefa contextoTarefa, String parametro) throws Exception{

        String codnat = String.valueOf(contextoTarefa.getCampo("CODNAT"));
        String cnpj = String.valueOf(contextoTarefa.getCampo("CNPJ"));
        BigDecimal codigoTipoOperacao = new BigDecimal(contextoTarefa.getCampo("TOPSERV") == null || contextoTarefa.getCampo("TOPSERV").toString().equals("")
                ? contextoTarefa.getCampo("TOPPROD").toString() : contextoTarefa.getCampo("TOPSERV").toString());

        String quantidadeLancamentos = null;
        String[] parametrosRecorrencia = null;
        String status = null;

        QueryExecutor consultaParametro = contextoTarefa.getQuery();
        consultaParametro.setParam("PARAM", parametro);
        consultaParametro.nativeSelect("SELECT * FROM AD_PARAMCP WHERE PARAMETRO = {PARAM}");
        if(consultaParametro.next()){
            parametrosRecorrencia = consultaParametro.getString("VALOR").split(";");
            status = consultaParametro.getString("STATUS");
        }

        if(status.equalsIgnoreCase("2")){

            QueryExecutor consultarLancamento = contextoTarefa.getQuery();
            consultarLancamento.setParam("CODNAT", codnat);
            consultarLancamento.setParam("CNPJ", cnpj);
            consultarLancamento.setParam("PERIODO", parametrosRecorrencia[1]);
            consultarLancamento.nativeSelect("SELECT COUNT(*) QTDLANCAMENTOS FROM AD_FINCAIXAPQ WHERE CODNAT = {CODNAT} AND CNPJ = {CNPJ} AND TO_CHAR(DTMOV,'MM') > (TO_CHAR(SYSDATE, 'MM') - {PERIODO})");

            if(consultarLancamento.next()){
                quantidadeLancamentos = consultarLancamento.getString("QTDLANCAMENTOS");
            }

            consultarLancamento.close();

            if( parametrosRecorrencia[0].compareTo(quantidadeLancamentos) < 0 ){
                ErroUtils.disparaErro("Quantidade de lançamentos excedida para este fornecedor CNPJ: " + cnpj + " e para essa natureza: "+codnat+" ! \nFineza verificar!");
            }
        } else if (consultaParametro == null) {
            ErroUtils.disparaErro(VariaveisFlow.MENSAGEM_PARAMETRO);
        }

        consultaParametro.close();
    }

    private static void gravarRegistro(ContextoTarefa contextoTarefa) throws Exception {
        String numnota = String.valueOf(contextoTarefa.getCampo("NUMNOTA"));
        String codemp = String.valueOf(contextoTarefa.getCampo("CODEMP"));
        String numcontr = String.valueOf(contextoTarefa.getCampo("NUMCONTR"));
        String tpneg = String.valueOf(contextoTarefa.getCampo("TPNEG"));
        String codlot = String.valueOf(contextoTarefa.getCampo("COD_LOTACAO"));
        String codnat = String.valueOf(contextoTarefa.getCampo("CODNAT"));
        String codproj = String.valueOf(contextoTarefa.getCampo("CODPROJ"));
        String serienota = String.valueOf(contextoTarefa.getCampo("SERIENOTA"));
        Object dtfatem = contextoTarefa.getCampo("DTFATEM");
        Object dtentrcont = contextoTarefa.getCampo("DTENTRCONT");
        Object dtmov = contextoTarefa.getCampo("DTMOV");
        String obs = String.valueOf(contextoTarefa.getCampo("OBS"));
        String chavenfe = String.valueOf(contextoTarefa.getCampo("CHAVENFE"));
        String topserv = String.valueOf(contextoTarefa.getCampo("TOPSERV"));
        String topprod = String.valueOf(contextoTarefa.getCampo("TOPPROD"));
        String codprod = String.valueOf(contextoTarefa.getCampo("CODPROD"));
        String qtdneg = String.valueOf(contextoTarefa.getCampo("QTDNEG"));
        String cnpj = String.valueOf(contextoTarefa.getCampo("CNPJ"));
        String jstcompr = String.valueOf(contextoTarefa.getCampo("JSTCOMPR"));
        String vlrestpro = String.valueOf(contextoTarefa.getCampo("VLRESTPRO"));
        String vlrunit = String.valueOf(contextoTarefa.getCampo("VLRUNIT"));
        String vlrtot = String.valueOf(contextoTarefa.getCampo("VLRTOT"));
        BigDecimal valorDescontoNota = contextoTarefa.getCampo("VLRDESCTOT") == "" || contextoTarefa.getCampo("VLRDESCTOT") == null
                ? BigDecimal.ZERO : new BigDecimal(contextoTarefa.getCampo("VLRDESCTOT").toString());
        String codcencus = String.valueOf(contextoTarefa.getCampo("CODCENCUS"));
        String parceiro = String.valueOf(contextoTarefa.getCampo("PARCEIRO"));
        Object justificaCompra = String.valueOf(contextoTarefa.getCampo("JSTCOMPR"));
        String codigoUsuario = String.valueOf(contextoTarefa.getUsuarioLogado().toString());

        if (vlrestpro.equalsIgnoreCase("null")
                || vlrestpro == null || vlrestpro.equalsIgnoreCase("")) {
            vlrestpro = String.valueOf("0");
        }
        if(numnota.equalsIgnoreCase("null")
                || numnota == null || numnota.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Necessário informar o número da nota");
        }else if(cnpj.length() > 14){
            ErroUtils.disparaErro("CNPJ são apenas números, fineza verificar!");
        }else if(cnpj.equalsIgnoreCase("null")
                || cnpj == null || cnpj.equalsIgnoreCase("")) {
            ErroUtils.disparaErro("Necessário informar o CNPJ, fineza verificar!");
        }else if(parceiro.equalsIgnoreCase("null")
                || parceiro == null || parceiro.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Parceiro informado não está cadastrado, fineza verificar com a MGS!");
        }else if(topserv.equalsIgnoreCase(String.valueOf("null"))
                && chavenfe.equalsIgnoreCase(String.valueOf("null"))){
            ErroUtils.disparaErro("Chave NFe não foi informada, fineza verificar!");
        }else if(topserv.equalsIgnoreCase(String.valueOf("null"))
            && chavenfe.length() < 44){
            ErroUtils.disparaErro("Chave NFe informada incorretamente, fineza verificar!");
        }else if(!topserv.equalsIgnoreCase(String.valueOf(""))
                && !chavenfe.equalsIgnoreCase(String.valueOf(""))){
            ErroUtils.disparaErro("Chave NFe foi informada para o processo de serviço, fineza remover!");
        }else if(codlot.equalsIgnoreCase("null")
                || codlot == null || codlot.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Lotação não foi informada, fineza verificar!");
        }else if(codnat.equalsIgnoreCase("null")
                || codnat == null || codnat.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Natureza não foi informada, fineza verificar!");
        }else if(serienota.equalsIgnoreCase("null")
                || serienota == null || serienota.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Nº de Série da Nota não foi informada, fineza verificar!");
        }else if(!topserv.equalsIgnoreCase(String.valueOf(""))
            && !serienota.equalsIgnoreCase(String.valueOf("NFS"))){
            ErroUtils.disparaErro(VariaveisFlow.SERIE_NOTA_INCORRETA);
        }else if(!topprod.equalsIgnoreCase(String.valueOf(""))
                && serienota.equalsIgnoreCase(String.valueOf("NFS"))){
            ErroUtils.disparaErro(VariaveisFlow.SERIE_NOTA_INCORRETA);
        }else if(tpneg.equalsIgnoreCase("null")
                || tpneg == null || tpneg.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Tipo de negociação não foi informada, fineza verificar!");
        }else if(codcencus.equalsIgnoreCase("null")
                || codcencus == null || codcencus.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Centro de resultado não foi informado, fineza verificar!");
        }else if(dtfatem == null){
            ErroUtils.disparaErro("Data do Faturamento/Emissão não foi informado, fineza verificar!");
        }else if(dtentrcont == null){
            ErroUtils.disparaErro("Data da Entrada Contábil não foi informado, fineza verificar!");
        }else if(dtmov == null){
            ErroUtils.disparaErro("Data da Movimentação não foi informado, fineza verificar!");
        }else if(obs.equalsIgnoreCase("null")
                || obs == null || obs.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Observação deve ser preenchida, fineza verificar!");
        }else if( justificaCompra == null || jstcompr.equalsIgnoreCase("")
                || jstcompr.equalsIgnoreCase("null")){
            ErroUtils.disparaErro("Justificativa não foi informada fineza verificar");
        }

        //Criar a validação a partir o fechamento contabil
        if( dtfatem != null && dtmov != null && dtentrcont != null ){

            Timestamp dataFaturamento = (Timestamp) dtfatem;
            Timestamp dataMovimentacao = (Timestamp) dtmov;
            Timestamp dataContabil = (Timestamp) dtentrcont;
            int mesFaturamento = TimeUtils.getMonth(dataFaturamento);
            int mesMovimentacao = TimeUtils.getMonth(dataMovimentacao);
            int mesContabil = TimeUtils.getMonth(dataContabil);
            int mesAtual = TimeUtils.getMonth(TimeUtils.getNow());

            if(mesFaturamento != mesAtual
                    || mesMovimentacao != mesAtual
                    || mesContabil != mesAtual ){
                ErroUtils.disparaErro("Fineza verificar as datas, não é possível realizar um lançamento com a data do mês anterior!");
            }
        }

        String codigoLotacao= null;

        //Valida a codigo de lotacao nas configurações de restrições do tipo de operação
        QueryExecutor consultaRestricoesTOP = contextoTarefa.getQuery();
        consultaRestricoesTOP.setParam("TIPNEG", tpneg );
        consultaRestricoesTOP.setParam("CODLOT", codlot );
        consultaRestricoesTOP.nativeSelect("SELECT CODLOT FROM AD_TPNEGCAIXAPQ WHERE CODTIPVENDA={TIPNEG} AND CODLOT={CODLOT}");
        if(consultaRestricoesTOP.next()){
            codigoLotacao = consultaRestricoesTOP.getString("CODLOT");
        }
        consultaRestricoesTOP.close();

        if(codigoLotacao == null || codigoLotacao.equalsIgnoreCase("")){
            ErroUtils.disparaErro("Codigo de lotação não esta vinculado ao tipo de negociação, fineza entrar em contato com a Tesouraria MGS!");
        }

        JapeWrapper fincaixapqFCVO = JapeFactory.dao("AD_FINCAIXAPQ");
        FluidCreateVO fluidCreateVO = fincaixapqFCVO.create();

        fluidCreateVO.set("IDINSTPRN", new BigDecimal(contextoTarefa.getIdInstanceProcesso().toString()));
        fluidCreateVO.set("NUMNOTA", new BigDecimal(numnota));
        fluidCreateVO.set("CODEMP", new BigDecimal(codemp));
        fluidCreateVO.set("NUMCONTR", new BigDecimal(Long.parseLong(numcontr)) );
        fluidCreateVO.set("TPNEG", new BigDecimal(tpneg));
        fluidCreateVO.set("CODLOT", new BigDecimal(Long.parseLong(codlot)));
        fluidCreateVO.set("CODNAT", new BigDecimal(codnat));
        fluidCreateVO.set("CODPROJ", new BigDecimal(codproj));
        fluidCreateVO.set("SERIENOTA", serienota);
        fluidCreateVO.set("DTFATEM", Timestamp.valueOf(String.valueOf(dtfatem)));
        fluidCreateVO.set("DTENTRCONT", Timestamp.valueOf(String.valueOf(dtentrcont)));
        fluidCreateVO.set("DTMOV", Timestamp.valueOf(String.valueOf(dtmov)));
        fluidCreateVO.set("OBS", obs);

        if (topprod != String.valueOf("")) {
            fluidCreateVO.set("TOPPROD", new BigDecimal(topprod));
        } else {
            fluidCreateVO.set("TOPSERV", new BigDecimal(topserv));
        }

        fluidCreateVO.set("CODPROD", new BigDecimal(codprod));
        fluidCreateVO.set("QTDNEG", new BigDecimal(qtdneg));
        fluidCreateVO.set("JSTCOMPR", jstcompr);
        fluidCreateVO.set("VLRESTPRO", new BigDecimal(vlrestpro));
        fluidCreateVO.set("VLRUNIT", new BigDecimal(vlrunit));
        fluidCreateVO.set("VLRTOT", new BigDecimal(vlrtot));
        fluidCreateVO.set("VLRDESCTOT", valorDescontoNota);
        fluidCreateVO.set("CODCENCUS", new BigDecimal(codcencus));
        fluidCreateVO.set("CNPJ", cnpj);
        fluidCreateVO.set("CHAVENFE", chavenfe);
        fluidCreateVO.set("CODUSUARIO", BigDecimal.valueOf(Long.parseLong(codigoUsuario)) );
        fluidCreateVO.save();

        Collection<DynamicVO> rateiosFlow = rateioFlow.find("IDINSTPRN = ?", new Object[]{contextoTarefa.getIdInstanceProcesso()});
        int i = 0;

        for (DynamicVO rateio: rateiosFlow){

            JapeWrapper rateioSankhyaDAO = JapeFactory.dao("AD_FINRATEIOCPQ");
            FluidCreateVO rateioSankhyaFCVO = rateioSankhyaDAO.create();
            rateioSankhyaFCVO.set("IDINSTPRN", contextoTarefa.getIdInstanceProcesso());
            rateioSankhyaFCVO.set("SEQUENCIA",new BigDecimal(i));
            rateioSankhyaFCVO.set("VLRRATEIO",rateio.asBigDecimal("VLRRATEIO"));
            rateioSankhyaFCVO.set("PERCRATEIO", rateio.asBigDecimal("PERCRATEIO"));
            rateioSankhyaFCVO.set("CODPROJ", rateio.asBigDecimal("CODPROJ"));
            rateioSankhyaFCVO.set("CODCTACTB", rateio.asBigDecimal("CODCTACTB"));
            rateioSankhyaFCVO.set("CODSITRATEIO", rateio.asBigDecimal("CODSITRATEIO"));
            rateioSankhyaFCVO.set("CODNAT", rateio.asBigDecimal("CODNAT"));
            rateioSankhyaFCVO.set("CODCENCUS", rateio.asBigDecimal("CODCENCUS"));
            rateioSankhyaFCVO.save();
            ++i;
        }
    }
}
