var tipoProcesso = getCampo("TIPPROC");
var tipoOperacao = getCampo("TIPOPER");
var tipOperacaoProdutoSeplag = 603;
var tipOperacaoServicoSeplag = 613;
var tipOperacaoProdutoMGS = 602;
var tipOperacaoServicoMGS = 612;
var codproj;
var codemp;

var codigoProduto = getCampo("CODPROD");

if(tipoOperacao == "S"){
    if (tipoProcesso == "S"){
        if(codigoProduto == "91576" ){
            throw new java.lang.Exception('O produto selecionado não é um Serviço, fineza verificar!');
        }
        codproj = 99990001;
        codemp = 1;
        setCampo("TOPSERV", tipOperacaoServicoSeplag);
    } else{
        if( codigoProduto != "91576" ){
            throw new java.lang.Exception('O produto foi selecionado incorretamente, fineza verificar!');
        }
        codproj = 99990001;
        codemp = 1;
        setCampo("TOPPROD", tipOperacaoProdutoSeplag);
    }
} else if( tipoOperacao == "M"){
    if (tipoProcesso == "S"){
            setCampo("TOPSERV", tipOperacaoServicoMGS);
        } else{
            setCampo("TOPPROD", tipOperacaoProdutoMGS);
    }
}


setCampo("CODPROJ", codproj);
setCampo("CODEMP", codemp);
setCampo("TIPPROC", tipoProcesso);