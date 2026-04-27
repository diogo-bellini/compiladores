package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.HashMap;

public class TabelaDeSimbolos {
    private final HashMap<String, EntradaTabelaDeSimbolos> tabelaDeSimbolos;

    public TabelaDeSimbolos() {
        tabelaDeSimbolos = new HashMap<>();
    }

    public void inserir(String nome, Tipos tipo, Categoria categoria) {
        EntradaTabelaDeSimbolos instancia = new EntradaTabelaDeSimbolos();
        instancia.nome = nome;
        instancia.tipo = tipo;
        instancia.categoria = categoria;
        tabelaDeSimbolos.put(nome, instancia);
    }

    public EntradaTabelaDeSimbolos buscar(String nome) {
        return tabelaDeSimbolos.get(nome);
    }

    public boolean existe(String nome) {
        return tabelaDeSimbolos.containsKey(nome);
    }

    public Tipos verificarTipo(String nome) {
        return tabelaDeSimbolos.get(nome).tipo;
    }
}
