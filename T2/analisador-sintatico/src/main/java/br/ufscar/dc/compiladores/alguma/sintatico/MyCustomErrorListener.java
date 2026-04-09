package br.ufscar.dc.compiladores.alguma.sintatico;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.PrintWriter;
import java.util.BitSet;

public class MyCustomErrorListener implements ANTLRErrorListener {
    private PrintWriter writer;
    private boolean error = false;

    public MyCustomErrorListener(PrintWriter writer) {
        this.writer = writer;
    }
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s, RecognitionException e) {
        if (!error) {
            error = true;
            Token t = (Token) o;
            String texto = t.getText();
            if (t.getType() == Token.EOF) {
                texto = "EOF";
            }
            writer.println("Linha " + i + ": erro sintatico proximo a " + texto);
        }
    }

    @Override
    public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean b, BitSet bitSet, ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitSet, ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {

    }
}
