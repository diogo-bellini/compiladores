package br.ufscar.dc.compiladores.alguma.semantico;

public class Semantico extends LinguagemAlgoritmicaBaseVisitor<Void>{
    Escopos escopos = new Escopos();
    Tipos tipoRetornoAtual = null;

    private Tipos obterTipo(LinguagemAlgoritmicaParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return Tipos.REGISTRO;
        }
        return obterTipo(ctx.tipo_estendido());
    }

    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_estendidoContext ctx) {
        if (ctx.PONTEIRO() != null) {
            return Tipos.PONTEIRO;
        }
        return obterTipo(ctx.tipo_basico_ident());
    }

    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null) {
            return obterTipo(ctx.tipo_basico());
        }
        String nome = ctx.IDENT().getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);

        if (e == null || e.categoria != Categoria.TIPO) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.TIPO_NAO_DECLARADO);
            return Tipos.INVALIDO;
        }
        return e.tipo;
    }

    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_basicoContext ctx) {
        if (ctx.INTEIRO() != null) return Tipos.INTEIRO;
        if (ctx.REAL() != null) return Tipos.REAL;
        if (ctx.LOGICO() != null) return Tipos.LOGICO;
        if (ctx.LITERAL() != null) return Tipos.LITERAL;
        return Tipos.INVALIDO;
    }

    private Tipos obterTipo(LinguagemAlgoritmicaParser.Valor_constanteContext ctx){
        if (ctx.NUM_INT() != null) return Tipos.INTEIRO;
        if (ctx.NUM_REAL() != null) return Tipos.REAL;
        if (ctx.CADEIA() != null) return Tipos.LITERAL;
        if (ctx.VERDADEIRO() != null || ctx.FALSO() != null) return Tipos.LOGICO;
        return Tipos.INVALIDO;
    }

    @Override
    public Void visitPrograma(LinguagemAlgoritmicaParser.ProgramaContext ctx) {
        if (ctx.declaracoes() != null) {
            visitDeclaracoes(ctx.declaracoes());
        }
        if (ctx.corpo() != null) {
            visitCorpo(ctx.corpo());
        }
        return null;
    }

    @Override
    public Void visitDeclaracoes(LinguagemAlgoritmicaParser.DeclaracoesContext ctx) {
        for (var decl : ctx.decl_local_global()) {
            visitDecl_local_global(decl);
        }
        return null;
    }

    @Override
    public Void visitDecl_local_global(LinguagemAlgoritmicaParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_local() != null) {
            visitDeclaracao_local(ctx.declaracao_local());
        }
        if (ctx.declaracao_global() != null) {
            visitDeclaracao_global(ctx.declaracao_global());
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_global(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        String nome = ctx.IDENT().getText();

        if (escopoAtual.existe(nome)) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
        }
        else {
            if (ctx.PROCEDIMENTO() != null) {
                escopoAtual.inserir(nome, Tipos.INVALIDO, Categoria.PROCEDIMENTO);
            }
            if (ctx.FUNCAO() != null) {
                Tipos tipoRetorno = obterTipo(ctx.tipo_estendido());
                escopoAtual.inserir(nome, tipoRetorno, Categoria.FUNCAO);
                tipoRetornoAtual = tipoRetorno;
            }
        }

        escopos.criarEscopo();

        if (ctx.parametros() != null) {
            visitParametros(ctx.parametros());
        }

        super.visitDeclaracao_global(ctx);
        escopos.deletarEscopoAtual();
        tipoRetornoAtual = null;
        return null;
    }

    @Override
    public Void visitParametros(LinguagemAlgoritmicaParser.ParametrosContext ctx) {
        for (var p : ctx.parametro()) {
            visitParametro(p);
        }
        return null;
    }

    @Override
    public Void visitParametro(LinguagemAlgoritmicaParser.ParametroContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        Tipos tipo = obterTipo(ctx.tipo_estendido());

        for (var ident : ctx.identificador()) {
            String nome = ident.getText();

            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ident.start, TipoErro.IDENTIFICADOR_REPETIDO);
            } else {
                escopoAtual.inserir(nome, tipo, Categoria.PARAMETRO);
            }
        }
        return null;
    }

    @Override
    public Void visitDeclaracao_local(LinguagemAlgoritmicaParser.Declaracao_localContext ctx) {
        if (ctx.variavel() != null) {
            return visitVariavel(ctx.variavel());
        }

        if (ctx.CONSTANTE() != null) {
            TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
            String nome = ctx.IDENT().getText();

            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
                return null;
            }

            Tipos tipoDeclarado = obterTipo(ctx.tipo_basico());
            Tipos tipoValor = obterTipo(ctx.valor_constante());

            if (!SemanticoUtils.tiposCompativeis(tipoDeclarado, tipoValor)) {
                SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            }

            escopoAtual.inserir(nome, tipoDeclarado, Categoria.CONSTANTE);
            return null;
        }

        if (ctx.TIPO() != null) {
            String nome = ctx.IDENT().getText();
            TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();

            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
            } else {
                Tipos tipo = obterTipo(ctx.tipo());
                escopoAtual.inserir(nome, tipo, Categoria.TIPO);
            }
        }
        return super.visitDeclaracao_local(ctx);
    }

    @Override
    public Void visitVariavel(LinguagemAlgoritmicaParser.VariavelContext ctx) {
        Tipos tipo = obterTipo(ctx.tipo());
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();

        for (var ident : ctx.identificador()) {
            String nome = ident.getText();
            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ident.start, TipoErro.IDENTIFICADOR_REPETIDO);
            } else {
                escopoAtual.inserir(nome, tipo, Categoria.VARIAVEL);
            }
        }
        return null;
    }

    @Override
    public Void visitCorpo(LinguagemAlgoritmicaParser.CorpoContext ctx) {
        for (var decl : ctx.declaracao_local()) {
            visitDeclaracao_local(decl);
        }
        for (var cmd : ctx.cmd()) {
            visit(cmd);
        }
        return null;
    }

    @Override
    public Void visitCmdLeia(LinguagemAlgoritmicaParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            String nome = ident.getText();
            EntradaTabelaDeSimbolos e = escopos.buscar(nome);
            if (e == null) {
                SemanticoUtils.adicionarErro(ident.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
            }
        }
        return null;
    }

    @Override
    public Void visitCmdEscreva(LinguagemAlgoritmicaParser.CmdEscrevaContext ctx) {
        for (var expr : ctx.expressao()) {
            SemanticoUtils.verificarTipo(escopos, expr);
        }
        return null;
    }

    @Override
    public Void visitCmdSe(LinguagemAlgoritmicaParser.CmdSeContext ctx) {
        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        if (tipoExpr != Tipos.LOGICO && tipoExpr != Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.expressao().start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }
        for (var cmd : ctx.cmd()) {
            visit(cmd);
        }
        return null;
    }

    @Override
    public Void visitCmdCaso(LinguagemAlgoritmicaParser.CmdCasoContext ctx) {
        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos, ctx.exp_aritmetica());
        if (!SemanticoUtils.tiposNumericos(tipoExpr, tipoExpr) && tipoExpr != Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.exp_aritmetica().start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }
        for (var cmd : ctx.cmd()) {
            visit(cmd);
        }
        return null;
    }

    @Override
    public Void visitCmdPara(LinguagemAlgoritmicaParser.CmdParaContext ctx) {
        String nome = ctx.IDENT().getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);

        if (e == null) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_NAO_DECLARADO);
        } else {
            if (!SemanticoUtils.tiposNumericos(e.tipo, e.tipo)) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            }
        }

        Tipos tipoInicio = SemanticoUtils.verificarTipo(escopos, ctx.exp_aritmetica(0));
        if (!SemanticoUtils.tiposNumericos(tipoInicio, tipoInicio) && tipoInicio != Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.exp_aritmetica(0).start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }

        Tipos tipoFim = SemanticoUtils.verificarTipo(escopos, ctx.exp_aritmetica(1));
        if (!SemanticoUtils.tiposNumericos(tipoFim, tipoFim) && tipoFim != Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.exp_aritmetica(1).start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }

        for (var cmd : ctx.cmd()) {
            visit(cmd);
        }
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LinguagemAlgoritmicaParser.CmdEnquantoContext ctx) {
        Tipos t = SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        if (t!= Tipos.LOGICO && t != Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.expressao().start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }
        for (var cmd : ctx.cmd()) {
            visit(cmd);
        }
        return null;
    }

    @Override
    public Void visitCmdFaca(LinguagemAlgoritmicaParser.CmdFacaContext ctx) {
        for (var cmd : ctx.cmd()) {
            visit(cmd);
        }
        Tipos t = SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        if (t!= Tipos.LOGICO && t != Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.expressao().start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }
        return null;
    }

    @Override
    public Void visitCmdAtribuicao(LinguagemAlgoritmicaParser.CmdAtribuicaoContext ctx) {
        String nome = ctx.identificador().IDENT(0).getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);

        if (e == null) {
            SemanticoUtils.adicionarErro(ctx.identificador().start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
            return null;
        }

        Tipos tipoVar = e.tipo;
        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos, ctx.expressao());

        if (tipoExpr == Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            return null;
        }

        if (tipoVar == Tipos.PONTEIRO) {
            if (tipoExpr != Tipos.ENDERECO) {
                SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            }
            return null;
        }

        if (tipoExpr == Tipos.ENDERECO){
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            return null;
        }

        if (SemanticoUtils.tiposNumericos(tipoVar, tipoExpr)) {
            return null;
        }

        if (tipoVar == tipoExpr) {
            return null;
        }

        SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        return null;
    }

    @Override
    public Void visitCmdChamada(LinguagemAlgoritmicaParser.CmdChamadaContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        String nome = ctx.IDENT().getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);

        if (e  == null) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_NAO_DECLARADO);
            return null;
        }

        if (e.categoria != Categoria.FUNCAO && e.categoria != Categoria.PROCEDIMENTO) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            return null;
        }

        for (var expr : ctx.expressao()) {
            SemanticoUtils.verificarTipo(escopos, expr);
        }
        return null;
    }

    @Override
    public Void visitCmdRetorne(LinguagemAlgoritmicaParser.CmdRetorneContext ctx) {
        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        if (tipoRetornoAtual == null){
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            return null;
        }
        if (!SemanticoUtils.tiposCompativeis(tipoRetornoAtual, tipoExpr)) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }
        return null;
    }
}
