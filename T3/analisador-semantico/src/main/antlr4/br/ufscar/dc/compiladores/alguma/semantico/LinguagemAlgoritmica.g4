grammar LinguagemAlgoritmica;

// ---------------- Análise Léxica ---------------------
//
// Define os tokens da linguagem, incluindo palavras-chave,
// operadores, identificadores, números e símbolos especiais.

ALGORITMO: 'algoritmo';
FIM_ALGORITMO: 'fim_algoritmo';
DECLARE: 'declare';
LEIA: 'leia';
ESCREVA: 'escreva';

PROCEDIMENTO: 'procedimento';
FIM_PROCEDIMENTO: 'fim_procedimento';
FUNCAO: 'funcao';
FIM_FUNCAO: 'fim_funcao';
RETORNE: 'retorne';

DOIS_PONTOS: ':';
VIRGULA: ',';
PONTO: '.';
SETA: '<-';
PONTOS_SEGUIDOS: '..';

MENOR_QUE: '<';
MAIOR_QUE: '>';
MENOR_IGUAL_QUE: '<=';
MAIOR_IGUAL_QUE: '>=';
IGUAL: '=';
DIFERENTE: '<>';

E: 'e';
OU: 'ou';
NAO: 'nao';

SOMA: '+';
SUBTRACAO: '-';
MULTIPLICACAO: '*';
DIVISAO: '/';
RESTO: '%';

ABRE_COLCHETE: '[';
FECHA_COLCHETE: ']';
ABRE_PARENTESE: '(';
FECHA_PARENTESE: ')';

PONTEIRO: '^';
ENDERECO: '&';

TIPO: 'tipo';
CONSTANTE: 'constante';
VAR: 'var';
NUM_INT: ('0'..'9') ('0'..'9')*;
NUM_REAL: ('0'..'9') ('0'..'9')* '.' ('0'..'9') ('0'..'9')*;
LITERAL: 'literal';
INTEIRO: 'inteiro';
REAL: 'real';
LOGICO: 'logico';
REGISTRO: 'registro';
FIM_REGISTRO: 'fim_registro';
VERDADEIRO: 'verdadeiro';
FALSO: 'falso';

SE: 'se';
FIM_SE: 'fim_se';
SENAO: 'senao';
ENTAO: 'entao';
CASO: 'caso';
FIM_CASO: 'fim_caso';
SEJA: 'seja';

PARA: 'para';
FIM_PARA: 'fim_para';
ATE: 'ate';
FACA: 'faca';
ENQUANTO: 'enquanto';
FIM_ENQUANTO: 'fim_enquanto';

IDENT: ('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

CADEIA: '"' ( ESC_SEQ | ~('\n'|'"'|'\\') )* '"';
ESC_SEQ: '\\"';
WS: ( ' ' | '\t' | '\r' | '\n') {skip();};
COMENTARIO: '{' ~('{'|'\n'|'\r'|'}')* '}' {skip();};
CADEIA_NAO_FECHADA: '"' ( ESC_SEQ | ~('\n'|'"'|'\\') )* '\n';
COMENTARIO_NAO_FECHA: '{' ~('\n'|'\r'|'}')* '\n';
ERRO: .;

// ---------------- Análise Sintática ---------------------
//
// Define as regras da linguagem, ou seja, como os tokens podem ser
// combinados para formar estruturas válidas.

programa: declaracoes ALGORITMO corpo FIM_ALGORITMO;

declaracoes: decl_local_global*;

decl_local_global: declaracao_local |
                   declaracao_global;

declaracao_local: DECLARE variavel |
                  CONSTANTE IDENT DOIS_PONTOS tipo_basico IGUAL valor_constante |
                  TIPO IDENT DOIS_PONTOS tipo;

variavel: identificador (VIRGULA identificador)* DOIS_PONTOS tipo;

identificador: IDENT (PONTO IDENT)* dimensao;

dimensao: (ABRE_COLCHETE exp_aritmetica FECHA_COLCHETE)*;

tipo: registro |
      tipo_estendido;

tipo_estendido: PONTEIRO? tipo_basico_ident;

tipo_basico_ident: tipo_basico |
                   IDENT;

tipo_basico: LITERAL |
             INTEIRO |
             REAL |
             LOGICO;

valor_constante: CADEIA |
                 NUM_INT |
                 NUM_REAL |
                 VERDADEIRO |
                 FALSO;

registro: REGISTRO variavel* FIM_REGISTRO;

declaracao_global: PROCEDIMENTO IDENT ABRE_PARENTESE parametros? FECHA_PARENTESE declaracao_local* cmd* FIM_PROCEDIMENTO |
                   FUNCAO IDENT ABRE_PARENTESE parametros? FECHA_PARENTESE DOIS_PONTOS tipo_estendido declaracao_local* cmd* FIM_FUNCAO;

parametros: parametro (VIRGULA parametro)*;
parametro: VAR? identificador (VIRGULA identificador)* DOIS_PONTOS tipo_estendido;

corpo: declaracao_local* cmd*;

// Comandos da linguagem
cmd: cmdLeia |
     cmdEscreva |
     cmdSe |
     cmdCaso |
     cmdPara |
     cmdEnquanto |
     cmdFaca |
     cmdAtribuicao |
     cmdChamada |
     cmdRetorne;

cmdLeia: LEIA ABRE_PARENTESE PONTEIRO? identificador (VIRGULA PONTEIRO? identificador)* FECHA_PARENTESE;

cmdEscreva: ESCREVA ABRE_PARENTESE expressao (VIRGULA expressao)* FECHA_PARENTESE;

cmdSe: SE expressao ENTAO cmd* (SENAO cmd*)? FIM_SE;

cmdCaso: CASO exp_aritmetica SEJA selecao (SENAO cmd*)? FIM_CASO;

cmdPara: PARA IDENT SETA exp_aritmetica ATE exp_aritmetica FACA cmd* FIM_PARA;

cmdEnquanto: ENQUANTO expressao FACA cmd* FIM_ENQUANTO;

cmdFaca: FACA cmd* ATE expressao;

cmdAtribuicao: PONTEIRO? identificador SETA expressao;

cmdChamada: IDENT ABRE_PARENTESE expressao (VIRGULA expressao)* FECHA_PARENTESE;

cmdRetorne: RETORNE expressao;

// Estruturas auxiliares
selecao: item_selecao*;

item_selecao: constantes DOIS_PONTOS cmd*;

constantes: numero_intervalo (VIRGULA numero_intervalo)*;

numero_intervalo: op_unario? NUM_INT (PONTOS_SEGUIDOS op_unario? NUM_INT)?;

op_unario: SUBTRACAO;

// Expressões aritméticas
exp_aritmetica: termo (op1 termo)*;

termo: fator (op2 fator)*;

fator: parcela (op3 parcela)*;

op1: SOMA |
     SUBTRACAO;

op2: MULTIPLICACAO |
     DIVISAO;

op3: RESTO;

// Parcelas
parcela: op_unario? parcela_unario |
         parcela_nao_unario;

parcela_unario: PONTEIRO? identificador |
                IDENT ABRE_PARENTESE expressao (VIRGULA expressao)* FECHA_PARENTESE |
                NUM_INT |
                NUM_REAL |
                ABRE_PARENTESE expressao FECHA_PARENTESE;

parcela_nao_unario: ENDERECO identificador |
                    CADEIA;

// Expressões relacionais e lógicas
exp_relacional: exp_aritmetica (op_relacional exp_aritmetica)?;

op_relacional: IGUAL |
               DIFERENTE |
               MAIOR_IGUAL_QUE |
               MENOR_IGUAL_QUE |
               MAIOR_QUE |
               MENOR_QUE;

expressao: termo_logico (op_logico_1 termo_logico)*;

termo_logico: fator_logico (op_logico_2 fator_logico)*;

fator_logico: NAO? parcela_logica;

parcela_logica: (VERDADEIRO | FALSO) |
                exp_relacional;

op_logico_1: OU;

op_logico_2: E;