package br.ufscar.dc.compiladores.analisador.lexico;

import java.io.IOException;
import java.io.PrintWriter;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

public class Principal {

    public static void main(String[] args) {
        try {
            CharStream cs = CharStreams.fromFileName(args[0]);
            AlgumaLexer lex = new AlgumaLexer(cs);

            PrintWriter writer = new PrintWriter(args[1]);

            Token t = null;
            while ((t = lex.nextToken()).getType() != Token.EOF) {
                String symbol = AlgumaLexer.VOCABULARY.getSymbolicName(t.getType());
                String text = t.getText();

                if (symbol.equals("ERRO")) {
                    writer.println("Linha " + t.getLine() + ": " + text + " - simbolo nao identificado");
                    break;
                }

                if (symbol.equals("CADEIA_NAO_FECHADA")) {
                    writer.println("Linha " + t.getLine() + ": cadeia literal nao fechada");
                    break;
                }

                if (symbol.equals("COMENTARIO_NAO_FECHA")) {
                    writer.println("Linha " + t.getLine() + ": comentario nao fechado");
                    break;
                }

                if (symbol.equals("IDENT") || symbol.equals("CADEIA")
                        || symbol.equals("NUM_INT") || symbol.equals("NUM_REAL")) {
                    writer.println("<'" + text + "'," + symbol + ">");
                    continue;
                }

                writer.println("<'" + text + "','" + text + "'>");
            }

            writer.close();

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}