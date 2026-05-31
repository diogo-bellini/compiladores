package br.ufscar.dc.compiladores.alguma.gerador;

public class TipoUtils {
    // Obtém o tipo a partir de um contexto de tipo, tratando registros inline
    public static Tipos obterTipo(Escopos escopos, LinguagemAlgoritmicaParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return Tipos.REGISTRO;
        }
        return obterTipo(escopos, ctx.tipo_estendido());
    }

    // Identifica se o tipo é um ponteiro e resolve o tipo base
    public static Tipos obterTipo(Escopos escopos, LinguagemAlgoritmicaParser.Tipo_estendidoContext ctx) {
        if (ctx.PONTEIRO() != null) {
            return Tipos.PONTEIRO;
        }
        return obterTipo(escopos, ctx.tipo_basico_ident());
    }

    // Resolve tipo básico ou busca identificadores de tipos customizados na tabela
    public static Tipos obterTipo(Escopos escopos, LinguagemAlgoritmicaParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null) {
            return obterTipo(ctx.tipo_basico());
        }

        String nome = ctx.IDENT().getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);

        // Validação: o identificador usado como tipo deve existir e ser da categoria TIPO
        if (e == null || e.categoria != Categoria.TIPO) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.TIPO_NAO_DECLARADO);
            return Tipos.INVALIDO;
        }
        return e.tipo;
    }

    // Mapeia os tokens de tipos básicos da gramática para o enum Tipos
    public static Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_basicoContext ctx) {
        if (ctx.INTEIRO() != null) return Tipos.INTEIRO;
        if (ctx.REAL() != null) return Tipos.REAL;
        if (ctx.LOGICO() != null) return Tipos.LOGICO;
        if (ctx.LITERAL() != null) return Tipos.LITERAL;
        return Tipos.INVALIDO;
    }

    // Resolve o tipo de constantes literais para validação de atribuições em tempo de declaração
    public static Tipos obterTipo(LinguagemAlgoritmicaParser.Valor_constanteContext ctx) {
        if (ctx.NUM_INT() != null) return Tipos.INTEIRO;
        if (ctx.NUM_REAL() != null) return Tipos.REAL;
        if (ctx.CADEIA() != null) return Tipos.LITERAL;
        if (ctx.VERDADEIRO() != null || ctx.FALSO() != null) return Tipos.LOGICO;
        return Tipos.INVALIDO;
    }
}
