package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.LinkedList;
import java.util.List;

// Gerencia a pilha de escopos (tabelas de símbolos)
public class Escopos {
    private LinkedList<TabelaDeSimbolos> escopos;

    public Escopos(){
        escopos = new LinkedList<>();
        criarEscopo(); // escopo global inicial
    }

    // Cria um novo escopo no topo da pilha
    public void criarEscopo(){
        escopos.push(new TabelaDeSimbolos());
    }

    // Retorna o escopo atual (topo da pilha)
    public TabelaDeSimbolos obterEscopoAtual(){
        return escopos.peek();
    }

    // Retorna todos os escopos (usado para busca)
    public List<TabelaDeSimbolos> obterTodosEscopos(){
        return escopos;
    }

    // Remove o escopo atual
    public void deletarEscopoAtual(){
        escopos.pop();
    }

    // Busca um identificador do escopo mais interno ao mais externo
    public EntradaTabelaDeSimbolos buscar(String nome) {
        for (TabelaDeSimbolos escopo : escopos) {
            if (escopo.existe(nome)) {
                return escopo.buscar(nome);
            }
        }
        return null;
    }
}
