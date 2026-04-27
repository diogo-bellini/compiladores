package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.LinkedList;
import java.util.List;

public class Escopos {
    private LinkedList<TabelaDeSimbolos> escopos;

    public Escopos(){
        escopos = new LinkedList<>();
        criarEscopo();
    }

    public void criarEscopo(){
        escopos.push(new TabelaDeSimbolos());
    }

    public TabelaDeSimbolos obterEscopoAtual(){
        return escopos.peek();
    }

    public List<TabelaDeSimbolos> obterTodosEscopos(){
        return escopos;
    }

    public void deletarEscopoAtual(){
        escopos.pop();
    }

    public EntradaTabelaDeSimbolos buscar(String nome) {
        for (TabelaDeSimbolos escopo : escopos) {
            if (escopo.existe(nome)) {
                return escopo.buscar(nome);
            }
        }
        return null;
    }
}
