var tipoProcesso = getCampo("TIPPROC");
var tipoOperacao = getCampo("TIPOPER");
var tipOperacaoProdutoSeplag = 603;
var tipOperacaoServicoSeplag = 613;
var tipOperacaoProdutoMGS = 602;
var tipOperacaoServicoMGS = 612;
var codemp = 1;
var codproj = 99990001;
var contrato;

var codigoProduto = getCampo("CODPROD");

var parametroCaixaZerado = getCampo("CAIXAZERADO");
var campoTipProc = getCampo("TIPPROC");
var justificativa = getCampo("JSTCOMPR");
var quantidade = getCampo("QTDNEG");

if(parametroCaixaZerado !== 2){
    if( campoTipProc == ""  || codigoProduto == ""
        || quantidade == "" || justificativa == ""
        || tipoOperacao == "" ){
        throw new java.lang.Exception('Gentileza preencher o formulário!');
    }

    if(tipoOperacao == "S"){
        if (tipoProcesso == "S"){
            if(codigoProduto == "91576" ){
                throw new java.lang.Exception('O produto selecionado não é um Serviço, fineza verificar!');
            }
            setCampo("TOPSERV", tipOperacaoServicoSeplag);
        } else{
            if( codigoProduto != "91576" ){
                throw new java.lang.Exception('O produto foi selecionado incorretamente, fineza verificar!');
            }
            setCampo("TOPPROD", tipOperacaoProdutoSeplag);
        }
        setCampo("STATUSLIMITE", '1');
    } else if( tipoOperacao == "M"){
        if (tipoProcesso == "S"){
                if(codigoProduto == "91576" ){
                    throw new java.lang.Exception('O produto selecionado não é um Serviço, fineza verificar!');
                }
                setCampo("TOPSERV", tipOperacaoServicoMGS);
            } else{
                if( codigoProduto != "91576" ){
                    throw new java.lang.Exception('O produto foi selecionado incorretamente, fineza verificar!');
                }
                setCampo("TOPPROD", tipOperacaoProdutoMGS);
        }
        contrato = 1;
    }

    setCampo("CODPROJ", codproj);
    setCampo("CODEMP", codemp);
    setCampo("TIPPROC", tipoProcesso);
    setCampo("NUMCONTR", contrato);
}