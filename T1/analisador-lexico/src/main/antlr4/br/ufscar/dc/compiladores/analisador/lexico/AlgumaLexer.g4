lexer grammar AlgumaLexer;

// ---------------- PALAVRAS RESERVADAS ----------------
// Tokens que representam palavras-chave da linguagem Alguma.
// Essas palavras possuem significado fixo e não podem ser usadas como identificadores.
PALAVRAS_CHAVES: 'algoritmo' | 'fim_algoritmo' | 'declare' | 'leia' | 'escreva' | 'fim_registro' | 'procedimento' | 'fim_procedimento' | 'funcao' | 'fim_funcao' | 'retorne';

// ---------------- TIPOS E DECLARAÇÕES ----------------
// Representa palavras relacionadas à definição de tipos, variáveis e estruturas.
TIPOS: 'literal' | 'inteiro' | 'real' | 'tipo' | 'var' | '[' | ']' | 'constante' | 'logico' | 'registro';

// ---------------- ESTRUTURAS CONDICIONAIS ----------------
// Palavras utilizadas em comandos condicionais e expressões booleanas.
CONDICIONAIS: 'se' | 'senao' | 'entao' | 'fim_se' | 'caso' | 'seja' | 'fim_caso' | 'verdadeiro' | 'falso';

// ---------------- ESTRUTURAS DE REPETIÇÃO ----------------
// Tokens relacionados a estruturas de repetição (loops).
LOOPINGS: 'para' | 'fim_para' | 'ate' | 'faca' | 'enquanto' | 'fim_enquanto';

// ---------------- OPERADORES RELACIONAIS E DE ATRIBUIÇÃO ----------------
// Inclui operadores de comparação e atribuição.
OPERADORES: '<-' | '<' | '>' | '<=' | '>=' | '=' | '<>' | '..' | '.';

// ---------------- OPERADORES DE ENDEREÇO ----------------
// Utilizados para manipulação de ponteiros/endereço de memória.
ENDERECOS: '^' | '&';

// ---------------- OPERADORES LÓGICOS ----------------
// Utilizados em expressões booleanas.
OPERADORES_LOGICOS: 'e' | 'ou' | 'nao';

// ---------------- NÚMEROS ----------------
// Representa números inteiros positivos.
NUM_INT: ('0'..'9') ('0'..'9')*;

// Representa números reais (com parte decimal).
NUM_REAL: ('0'..'9') ('0'..'9')* '.' ('0'..'9') ('0'..'9')*;

// ---------------- IDENTIFICADORES ----------------
// Representa nomes de variáveis, funções, etc.
// Devem começar com letra e podem conter letras, números e underscore.
IDENT: ('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

// ---------------- DELIMITADORES ----------------
// Símbolos utilizados para separar elementos da linguagem.
DELIMITADORES: ',' | ':' | '(' | ')';

// ---------------- OPERADORES ARITMÉTICOS ----------------
// Operações matemáticas básicas.
OP_ARIT: '+' | '-' | '*' | '/' | '%';

// ---------------- CADEIAS DE CARACTERES ----------------
// Representa strings delimitadas por aspas duplas.
// Permite sequência de escape para aspas (\").
CADEIA: '"' ( ESC_SEQ | ~('\n'|'"'|'\\') )* '"';

// Sequência de escape válida dentro de strings.
ESC_SEQ: '\\"';

// ---------------- ESPAÇOS EM BRANCO ----------------
// Ignora espaços, tabulações e quebras de linha.
WS: ( ' ' | '\t' | '\r' | '\n') {skip();};

// ---------------- COMENTÁRIOS ----------------
// Comentários delimitados por { } são ignorados pelo analisador.
COMENTARIO: '{' ~('{'|'\n'|'\r'|'}')* '}' {skip();};

// ---------------- ERROS LÉXICOS ----------------

// Cadeia de caracteres que não foi fechada corretamente.
// Exemplo: "texto sem fechar
CADEIA_NAO_FECHADA: '"' ( ESC_SEQ | ~('\n'|'"'|'\\') )* '\n';

// Comentário que não foi fechado corretamente.
// Exemplo: { comentario sem fechar
COMENTARIO_NAO_FECHA: '{' ~('\n'|'\r'|'}')* '\n';

// Qualquer símbolo não reconhecido pela linguagem.
// Serve como captura genérica de erro léxico.
ERRO: .;