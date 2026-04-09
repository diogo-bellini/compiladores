/**
 * Listener de erros sintáticos customizado
 *
 * Esta classe implementa a interface ANTLRErrorListener para sobrescrever
 * o comportamento padrão de tratamento de erros do ANTLR durante a análise sintática.
 *
 * Seu objetivo é capturar erros sintáticos e formatar a saída de acordo
 * com o padrão exigido pelo projeto.
 *
 * Fluxo de execução:
 * 1. O parser detecta um erro sintático
 * 2. O método syntaxError é chamado automaticamente
 * 3. O erro é formatado e escrito no arquivo de saída
 * 4. Apenas o primeiro erro sintático é reportado
 *
 * Entrada:
 * - Objeto PrintWriter para escrita no arquivo de saída
 *
 * Saída:
 * - Mensagem no formato:
 *   "Linha X: erro sintatico proximo a Y"
 */
package br.ufscar.dc.compiladores.alguma.sintatico;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

import java.io.PrintWriter;
import java.util.BitSet;

public class MyCustomErrorListener implements ANTLRErrorListener {
    // Escritor utilizado para registrar os erros no arquivo de saída
    private PrintWriter writer;

    // Variável de controle para garantir que apenas o primeiro erro seja exibido
    private boolean error = false;

    public MyCustomErrorListener(PrintWriter writer) {
        this.writer = writer;
    }

    /**
     * Método chamado automaticamente pelo ANTLR ao encontrar um erro sintático
     *
     * @param recognizer parser que detectou o erro
     * @param o símbolo ofensivo (token onde ocorreu o erro)
     * @param i linha do erro
     * @param i1 coluna do erro
     * @param s mensagem padrão do ANTLR
     * @param e exceção de reconhecimento (pode ser nula)
     */
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object o, int i, int i1, String s, RecognitionException e) {
        if (!error) {
            error = true;
            // Converte o objeto para Token
            Token t = (Token) o;

            // Obtém o texto do token onde ocorreu o erro
            String texto = t.getText();

            // Caso o erro ocorra no fim do arquivo, ajusta a saída para passar nos testes automáticos
            if (t.getType() == Token.EOF) {
                texto = "EOF";
            }

            // Escreve a mensagem de erro no formato especificado
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
