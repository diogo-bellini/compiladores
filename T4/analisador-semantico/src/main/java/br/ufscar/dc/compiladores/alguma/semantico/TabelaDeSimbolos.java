package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.HashMap;

// Estrutura que armazena identificadores de um escopo
public class TabelaDeSimbolos {
    private final HashMap<String, EntradaTabelaDeSimbolos> tabelaDeSimbolos;

    public TabelaDeSimbolos() {
        tabelaDeSimbolos = new HashMap<>();
    }

    // inserção simples
    public void inserir(String nome, Tipos tipo, Categoria categoria) {

        EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
        entrada.nome = nome;
        entrada.tipo = tipo;
        entrada.categoria = categoria;
        tabelaDeSimbolos.put(nome, entrada);
    }

    // inserção completa
    public void inserir(EntradaTabelaDeSimbolos entrada) {
        tabelaDeSimbolos.put(entrada.nome, entrada);
    }

    // busca
    public EntradaTabelaDeSimbolos buscar(String nome) {
        return tabelaDeSimbolos.get(nome);
    }

    // existe no escopo
    public boolean existe(String nome) {
        return tabelaDeSimbolos.containsKey(nome);
    }

    // retorna tipo
    public Tipos verificarTipo(String nome) {
        return tabelaDeSimbolos.get(nome).tipo;
    }
}