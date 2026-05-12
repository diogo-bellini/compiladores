package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.ArrayList;
import java.util.List;

// Representa uma entrada na tabela de símbolos, contendo metadados cruciais para a análise semântica
public class EntradaTabelaDeSimbolos {
    String nome;           // Nome do identificador
    Tipos tipo;            // Tipo base (INTEIRO, REAL, REGISTRO, etc)
    Categoria categoria;   // Classificação (VARIAVEL, FUNCAO, TIPO, etc)
    String nomeTipo;       // Nome do tipo customizado (útil para registros definidos por 'tipo')
    
    // Atributos para manipulação de ponteiros
    boolean ehPonteiro = false;
    Tipos tipoApontado;
    
    // Atributos para manipulação de registros (structs)
    boolean ehRegistro = false;
    TabelaDeSimbolos camposRegistro; // Tabela própria para os campos internos do registro
    
    // Lista de tipos para assinatura de funções e procedimentos
    List<Tipos> tiposParametros = new ArrayList<>();
}