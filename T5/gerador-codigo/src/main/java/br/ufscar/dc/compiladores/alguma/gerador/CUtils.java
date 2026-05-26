package br.ufscar.dc.compiladores.alguma.gerador;

public class CUtils {
    public static String tipoC(Tipos tipo) {
        switch (tipo) {
            case INTEIRO:
                return "int";
            case REAL:
                return "float";
            case LITERAL:
                return "char";
            case LOGICO:
                return "int";
            default:
                return "void";
        }
    }

    public static String operadorC(String op) {
        switch (op) {
            case "=":
                return "==";
            case "<>":
                return "!=";
            case "ou":
                return "||";
            case "e":
                return "&&";
            default:
                return op;
        }
    }
}
