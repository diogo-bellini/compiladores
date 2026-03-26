/**
 * Analisador léxico
 *
 * Este programa recebe um arquivo de entrada e utiliza o ANTLR para realizar
 * a análise léxica, identificando tokens definidos em uma gramática.
 *
 * Fluxo de execução:
 * 1. Leitura do arquivo de entrada
 * 2. Inicialização do lexer gerado pelo ANTLR
 * 3. Processamento dos tokens
 * 4. Saída dos resultados (tokens ou erros léxicos)
 *
 * Entrada:
 * - args[0]: caminho do arquivo de entrada
 * - args[1]: caminho do arquivo de saída
 *
 * Saída:
 * - Arquivo contendo os tokens reconhecidos ou mensagens de erro léxico
 */
package br.ufscar.dc.compiladores.analisador.lexico;

import java.io.IOException;
import java.io.PrintWriter;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

public class Principal {

    public static void main(String[] args) {
        try {
            // Cria um fluxo de caracteres a partir do arquivo de entrada
            // Esse fluxo será utilizado pelo lexer para leitura do conteúdo
            CharStream cs = CharStreams.fromFileName(args[0]);

            // Inicializa o lexer gerado pelo ANTLR com o conteúdo do arquivo
            // O lexer será responsável por identificar os tokens da linguagem
            AlgumaLexer lex = new AlgumaLexer(cs);

            // Cria um escritor para o arquivo de saída
            // Todos os resultados da análise léxica serão escritos nesse arquivo
            PrintWriter writer = new PrintWriter(args[1]);

            Token t = null;

            // Loop principal: percorre todos os tokens gerados pelo lexer
            // A execução continua até encontrar o token EOF (fim do arquivo)
            while ((t = lex.nextToken()).getType() != Token.EOF) {

                // Obtém o nome simbólico do token (ex: IDENT, NUM_INT, etc.)
                String symbol = AlgumaLexer.VOCABULARY.getSymbolicName(t.getType());

                // Obtém o texto original do token encontrado no arquivo
                String text = t.getText();

                // Tratamento de erro: símbolo não identificado
                // Caso o lexer encontre um padrão inválido, interrompe a execução
                if (symbol.equals("ERRO")) {
                    writer.println("Linha " + t.getLine() + ": " + text + " - simbolo nao identificado");
                    break;
                }

                // Tratamento de erro: cadeia de caracteres não fechada
                // Exemplo: "texto sem fechar
                if (symbol.equals("CADEIA_NAO_FECHADA")) {
                    writer.println("Linha " + t.getLine() + ": cadeia literal nao fechada");
                    break;
                }

                // Tratamento de erro: comentário não fechado
                // Exemplo: { comentário sem fechar
                if (symbol.equals("COMENTARIO_NAO_FECHA")) {
                    writer.println("Linha " + t.getLine() + ": comentario nao fechado");
                    break;
                }

                // Tokens que devem ser exibidos com seu tipo explícito
                // Exemplo: <'variavel',IDENT>
                if (symbol.equals("IDENT") || symbol.equals("CADEIA") || symbol.equals("NUM_INT") || symbol.equals("NUM_REAL")) {
                    writer.println("<'" + text + "'," + symbol + ">");
                    continue;
                }

                // Para outros tokens (palavras-chave, operadores, etc.),
                // o tipo do token é o próprio texto
                // Exemplo: <'+','+'>
                writer.println("<'" + text + "','" + text + "'>");
            }

            // Fecha o arquivo de saída garantindo que todos os dados sejam escritos
            writer.close();

        } catch (IOException ex) {
            // Tratamento de erro de entrada/saída (arquivo não encontrado, etc.)
            ex.printStackTrace();
        }
    }
}