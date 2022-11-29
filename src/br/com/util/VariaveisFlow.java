package br.com.util;

import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.jape.wrapper.fluid.FluidCreateVO;
import br.com.sankhya.jape.wrapper.fluid.FluidUpdateVO;

import java.math.BigDecimal;
import java.util.Collection;

public class VariaveisFlow {

    public static final String MENSAGEM_PARAMETRO = "Parametro incorreto ou não encontrado, Fineza entrar em contato com o Tesouraria MGS!";
    public static final String PARCEIRO_NAO_ENCONTRADO = "Parceiro não foi localizado, fineza entrar em contato com o setor Tesouraria MGS!";
    public static final String NOTA_SEM_ANEXO = "Fineza anexar a nota ao lançamento!";
    public static final String CENTRO_RESULTADO_INATIVO = "Centro de resultado esta inativo, favor informar outro centro de resultado!";
    public static final String CENTRO_RESULTADO_NAO_ANALITICO = "Centro de resultado informado esta incorreto, favor informar um centro de resultado analitico!";
    public static final String SERIE_NOTA_INCORRETA = "Nº de Série da Nota foi informada incorretamente, fineza verificar!";

    public VariaveisFlow() {
    }

    public static Object getVariavel(BigDecimal idInstanciaProcesso, String nome) throws Exception {
        JapeWrapper instanciaVariavelDAO = JapeFactory.dao("InstanciaVariavel");
        Collection<DynamicVO> dynamicVOS = instanciaVariavelDAO.find("IDINSTPRN = ? AND NOME = ? ", new Object[]{idInstanciaProcesso, nome});
        if (!dynamicVOS.isEmpty()) {
            DynamicVO vo = (DynamicVO)dynamicVOS.iterator().next();
            String var5 = vo.asString("TIPO");
            byte var6 = -1;
            switch(var5.hashCode()) {
                case 67:
                    if (var5.equals("C")) {
                        var6 = 7;
                    }
                    break;
                case 68:
                    if (var5.equals("D")) {
                        var6 = 5;
                    }
                case 69:
                case 71:
                case 74:
                case 75:
                case 77:
                case 78:
                case 79:
                case 80:
                case 81:
                case 82:
                case 84:
                case 85:
                case 86:
                default:
                    break;
                case 70:
                    if (var5.equals("F")) {
                        var6 = 4;
                    }
                    break;
                case 72:
                    if (var5.equals("H")) {
                        var6 = 6;
                    }
                    break;
                case 73:
                    if (var5.equals("I")) {
                        var6 = 3;
                    }
                    break;
                case 76:
                    if (var5.equals("L")) {
                        var6 = 1;
                    }
                    break;
                case 83:
                    if (var5.equals("S")) {
                        var6 = 0;
                    }
                    break;
                case 87:
                    if (var5.equals("W")) {
                        var6 = 2;
                    }
            }

            switch(var6) {
                case 0:
                case 1:
                case 2:
                    return vo.asString("TEXTO");
                case 3:
                    return vo.asBigDecimal("NUMINT");
                case 4:
                    return vo.asBigDecimal("NUMDEC");
                case 5:
                case 6:
                    return vo.asTimestamp("DTA");
                case 7:
                    return vo.asString("TEXTOLONGO");
                default:
                    return null;
            }
        } else {
            return null;
        }
    }

    public static void setVariavel(BigDecimal idInstanciaProcesso, BigDecimal idInstanciaTarefa, String nome, Object variavel) throws Exception {
        JapeWrapper instanciaVariavelDAO = JapeFactory.dao("InstanciaVariavel");
        DynamicVO dynamicVO = instanciaVariavelDAO.findOne("IDINSTPRN = ? AND NOME = ? ", new Object[]{idInstanciaProcesso, nome});
        if (dynamicVO == null) {
            JapeWrapper instanciaProcessoDAO = JapeFactory.dao("InstanciaProcesso");
            JapeWrapper elementoProcessoDAO = JapeFactory.dao("ElementoProcesso");
            JapeWrapper variavelProcessoDAO = JapeFactory.dao("VariavelProcesso");
            DynamicVO instanciaProcessoVO = instanciaProcessoDAO.findByPK(new Object[]{idInstanciaProcesso});
            DynamicVO elementoProcessoVO = elementoProcessoDAO.findOne("TIPO = 'P' AND CODPRN = ? AND VERSAO = ?", new Object[]{instanciaProcessoVO.asBigDecimal("CODPRN"), instanciaProcessoVO.asBigDecimal("VERSAO")});
            DynamicVO variavelProcessoVO = variavelProcessoDAO.findOne("NUELE = ? AND NOME = ?", new Object[]{elementoProcessoVO.asBigDecimal("NUELE"), nome});
            FluidCreateVO fluidCreateVO = instanciaVariavelDAO.create();
            fluidCreateVO.set("IDINSTPRN", idInstanciaProcesso);
            fluidCreateVO.set("IDINSTTAR", idInstanciaTarefa);
            fluidCreateVO.set("NOME", nome);
            fluidCreateVO.set("TIPO", variavelProcessoVO.asString("TIPO"));
            dynamicVO = fluidCreateVO.save();
        }

        dynamicVO.asString("TIPO");
        FluidUpdateVO fluidUpdateVO = instanciaVariavelDAO.prepareToUpdate(dynamicVO);
        String var14 = dynamicVO.asString("TIPO");
        byte var15 = -1;
        switch(var14.hashCode()) {
            case 67:
                if (var14.equals("C")) {
                    var15 = 7;
                }
                break;
            case 68:
                if (var14.equals("D")) {
                    var15 = 5;
                }
            case 69:
            case 71:
            case 74:
            case 75:
            case 77:
            case 78:
            case 79:
            case 80:
            case 81:
            case 82:
            case 84:
            case 85:
            case 86:
            default:
                break;
            case 70:
                if (var14.equals("F")) {
                    var15 = 4;
                }
                break;
            case 72:
                if (var14.equals("H")) {
                    var15 = 6;
                }
                break;
            case 73:
                if (var14.equals("I")) {
                    var15 = 3;
                }
                break;
            case 76:
                if (var14.equals("L")) {
                    var15 = 1;
                }
                break;
            case 83:
                if (var14.equals("S")) {
                    var15 = 0;
                }
                break;
            case 87:
                if (var14.equals("W")) {
                    var15 = 2;
                }
        }

        switch(var15) {
            case 0:
            case 1:
            case 2:
                fluidUpdateVO.set("TEXTO", variavel);
                break;
            case 3:
                fluidUpdateVO.set("NUMINT", variavel);
                break;
            case 4:
                fluidUpdateVO.set("NUMDEC", variavel);
                break;
            case 5:
            case 6:
                fluidUpdateVO.set("DTA", variavel);
                break;
            case 7:
                fluidUpdateVO.set("TEXTOLONGO", variavel);
        }

        fluidUpdateVO.update();
    }
}
