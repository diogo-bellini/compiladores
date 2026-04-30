package br.ufscar.dc.compiladores.alguma.semantico;

// Tipos de erros semânticos possíveis
public enum TipoErro {
    IDENTIFICADOR_REPETIDO,
    TIPO_NAO_DECLARADO,
    IDENTIFICADOR_NAO_DECLARADO,
    ATRIBUICAO_NAO_COMPATIVEL
}
