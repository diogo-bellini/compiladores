/**
 * Analisador sintático
 *
 * Este programa recebe um arquivo de entrada e utiliza o ANTLR para realizar
 * a análise léxica e sintática, validando se o código segue as regras da gramática.
 *
 * Fluxo de execução:
 * 1. Leitura do arquivo de entrada
 * 2. Inicialização do lexer gerado pelo ANTLR
 * 3. Geração da lista de tokens
 * 4. Verificação de erros léxicos
 * 5. Inicialização do parser e análise sintática
 * 6. Saída dos resultados (erros léxicos/sintáticos ou sucesso)
 *
 * Entrada:
 * - args[0]: caminho do arquivo de entrada
 * - args[1]: caminho do arquivo de saída
 *
 * Saída:
 * - Arquivo contendo mensagens de erro léxico ou sintático
 *   ou apenas "Fim da compilacao" caso não haja erros
 */
package br.ufscar.dc.compiladores.alguma.sintatico;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.PrintWriter;

public class Principal {
    static boolean error = false;
    public static void main(String[] args) {
        try{
            // Cria um fluxo de caracteres a partir do arquivo de entrada
            // Esse fluxo será utilizado pelo lexer para leitura do conteúdo
            CharStream input = CharStreams.fromFileName(args[0]);

            // Inicializa o lexer gerado pelo ANTLR com o conteúdo do arquivo
            // O lexer será responsável por identificar os tokens da linguagem
            AlgumaLexer lex = new AlgumaLexer(input);

            // Cria um buffer de tokens gerados pelo lexer
            // Permite que o parser consuma os tokens posteriormente
            CommonTokenStream tokens = new CommonTokenStream(lex);
            tokens.fill();

            // Cria um escritor para o arquivo de saída
            // Todos os resultados da análise léxica e sintática serão escritos nesse arquivo
            PrintWriter writer = new PrintWriter(args[1]);

            // Percorre todos os tokens gerados pelo lexer
            // Objetivo: detectar erros léxicos antes da análise sintática
            for(Token t : tokens.getTokens()) {
                if(t.getType() == Token.EOF){
                    break;
                }

                // Obtém o nome simbólico do token (ex: IDENT, NUM_INT, etc.)
                String symbol = AlgumaLexer.VOCABULARY.getSymbolicName(t.getType());
                // Obtém o texto original do token encontrado no arquivo
                String text = t.getText();

                // Tratamento de erro: símbolo não identificado
                // Caso o lexer encontre um padrão inválido, interrompe a execução
                if (symbol.equals("ERRO")) {
                    writer.println("Linha " + t.getLine() + ": " + text + " - simbolo nao identificado");
                    error = true;
                    break;
                }

                // Tratamento de erro: cadeia de caracteres não fechada
                // Exemplo: "texto sem fechar
                if (symbol.equals("CADEIA_NAO_FECHADA")) {
                    writer.println("Linha " + t.getLine() + ": cadeia literal nao fechada");
                    error = true;
                    break;
                }

                // Tratamento de erro: comentário não fechado
                // Exemplo: { comentário sem fechar
                if (symbol.equals("COMENTARIO_NAO_FECHA")) {
                    writer.println("Linha " + t.getLine() + ": comentario nao fechado");
                    error = true;
                    break;
                }
            }
            // Caso não haja erro léxico, inicia a análise sintática
            if(!error){
                // Inicializa o parser com a lista de tokens
                AlgumaParser parser = new AlgumaParser(tokens);

                // Remove os listeners de erro padrão do ANTLR
                parser.removeErrorListeners();

                // Adiciona um listener customizado para tratar erros sintáticos
                MyCustomErrorListener errorListener = new MyCustomErrorListener(writer);
                parser.addErrorListener(errorListener);

                // Inicia a análise sintática a partir da regra inicial da gramática
                parser.programa();
            }
            // Indica o fim do processo de compilação
            writer.println("Fim da compilacao");

            // Fecha o arquivo de saída
            writer.close();
        } catch (IOException ex) {
            // Tratamento de erro de entrada/saída (arquivo não encontrado, etc.)
            ex.printStackTrace();
        }
    }
}