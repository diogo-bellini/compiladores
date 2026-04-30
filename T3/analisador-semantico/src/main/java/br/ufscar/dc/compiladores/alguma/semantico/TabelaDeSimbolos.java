package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.HashMap;

// Estrutura que armazena identificadores de um escopo
public class TabelaDeSimbolos {
    private final HashMap<String, EntradaTabelaDeSimbolos> tabelaDeSimbolos;

    public TabelaDeSimbolos() {
        tabelaDeSimbolos = new HashMap<>();
    }

    // Insere um novo identificador na tabela
    public void inserir(String nome, Tipos tipo, Categoria categoria) {
        EntradaTabelaDeSimbolos instancia = new EntradaTabelaDeSimbolos();
        instancia.nome = nome;
        instancia.tipo = tipo;
        instancia.categoria = categoria;
        tabelaDeSimbolos.put(nome, instancia);
    }

    // Busca um identificador
    public EntradaTabelaDeSimbolos buscar(String nome) {
        return tabelaDeSimbolos.get(nome);
    }

    // Verifica se já existe no escopo
    public boolean existe(String nome) {
        return tabelaDeSimbolos.containsKey(nome);
    }

    // Retorna o tipo associado ao identificador
    public Tipos verificarTipo(String nome) {
        return tabelaDeSimbolos.get(nome).tipo;
    }
}
