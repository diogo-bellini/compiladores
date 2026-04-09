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

            CommonTokenStream tokens = new CommonTokenStream(lex);
            tokens.fill();

            // Cria um escritor para o arquivo de saída
            // Todos os resultados da análise léxica e sintática serão escritos nesse arquivo
            PrintWriter writer = new PrintWriter(args[1]);

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
            if(!error){
                AlgumaParser parser = new AlgumaParser(tokens);
                parser.removeErrorListeners();
                MyCustomErrorListener errorListener = new MyCustomErrorListener(writer);
                parser.addErrorListener(errorListener);
                parser.programa();
            }
            writer.println("Fim da compilacao");
            writer.close();
        } catch (IOException ex) {
            // Tratamento de erro de entrada/saída (arquivo não encontrado, etc.)
            ex.printStackTrace();
        }
    }
}