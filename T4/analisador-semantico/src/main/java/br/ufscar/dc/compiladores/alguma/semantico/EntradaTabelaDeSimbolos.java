package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.ArrayList;
import java.util.List;

// Representa uma entrada na tabela de símbolos
public class EntradaTabelaDeSimbolos {
    String nome;
    Tipos tipo;
    Categoria categoria;
    // nome do tipo declarado
    // útil para registros
    String nomeTipo;
    // ponteiros
    boolean ehPonteiro = false;
    Tipos tipoApontado;
    // registros
    boolean ehRegistro = false;
    TabelaDeSimbolos camposRegistro;
    // funções/procedimentos
    List<Tipos> tiposParametros = new ArrayList<>();
}