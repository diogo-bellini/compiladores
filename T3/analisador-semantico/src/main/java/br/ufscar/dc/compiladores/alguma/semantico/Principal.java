package br.ufscar.dc.compiladores.alguma.semantico;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.PrintWriter;

// Classe principal: executa análise léxica, sintática e semântica
public class Principal {
    public static void main(String[] args) throws IOException {
        // Leitura do arquivo de entrada
        CharStream input = CharStreams.fromFileName(args[0]);

        // Análise léxica
        LinguagemAlgoritmicaLexer lexer = new LinguagemAlgoritmicaLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // Análise sintática
        LinguagemAlgoritmicaParser parser = new LinguagemAlgoritmicaParser(tokens);
        LinguagemAlgoritmicaParser.ProgramaContext arvore = parser.programa();

        // Análise semântica
        Semantico semantico = new Semantico();
        semantico.visitPrograma(arvore);

        // Escrita dos erros
        PrintWriter writer = new PrintWriter(args[1]);
        for (var erro : SemanticoUtils.errosSemanticos){
            writer.println(erro);
        }
        writer.println("Fim da compilacao");
        writer.close();
    }
}
