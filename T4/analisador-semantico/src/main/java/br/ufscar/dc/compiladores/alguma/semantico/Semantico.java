package br.ufscar.dc.compiladores.alguma.semantico;

// Classe responsável pela análise semântica utilizando o padrão Visitor
public class Semantico extends LinguagemAlgoritmicaBaseVisitor<Void>{

    Escopos escopos = new Escopos(); // controla os escopos ativos
    Tipos tipoRetornoAtual = null;   // usado para validar "retorne" em funções

    // Obtém o tipo a partir de um contexto de tipo
    private Tipos obterTipo(LinguagemAlgoritmicaParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return Tipos.REGISTRO;
        }
        return obterTipo(ctx.tipo_estendido());
    }

    // Trata ponteiros e delega para tipo básico
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_estendidoContext ctx) {
        if (ctx.PONTEIRO() != null) {
            return Tipos.PONTEIRO;
        }
        return obterTipo(ctx.tipo_basico_ident());
    }

    // Resolve tipo básico ou identificador de tipo definido pelo usuário
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_basico_identContext ctx) {
        if (ctx.tipo_basico() != null) {
            return obterTipo(ctx.tipo_basico());
        }

        String nome = ctx.IDENT().getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);

        // Tipo não declarado
        if (e == null || e.categoria != Categoria.TIPO) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.TIPO_NAO_DECLARADO);
            return Tipos.INVALIDO;
        }
        return e.tipo;
    }

    // Tipos básicos da linguagem
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_basicoContext ctx) {
        if (ctx.INTEIRO() != null) return Tipos.INTEIRO;
        if (ctx.REAL() != null) return Tipos.REAL;
        if (ctx.LOGICO() != null) return Tipos.LOGICO;
        if (ctx.LITERAL() != null) return Tipos.LITERAL;
        return Tipos.INVALIDO;
    }

    // Tipo de constantes literais
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Valor_constanteContext ctx){
        if (ctx.NUM_INT() != null) return Tipos.INTEIRO;
        if (ctx.NUM_REAL() != null) return Tipos.REAL;
        if (ctx.CADEIA() != null) return Tipos.LITERAL;
        if (ctx.VERDADEIRO() != null || ctx.FALSO() != null) return Tipos.LOGICO;
        return Tipos.INVALIDO;
    }

    // Ponto de entrada da análise semântica
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

    // Decide entre declaração local ou global
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

    // Trata funções e procedimentos
    @Override
    public Void visitDeclaracao_global(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        String nome = ctx.IDENT().getText();

        // Verifica duplicidade no escopo atual
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
                tipoRetornoAtual = tipoRetorno; // usado para validar "retorne"
            }
        }

        escopos.criarEscopo(); // novo escopo para parâmetros/corpo

        if (ctx.parametros() != null) {
            visitParametros(ctx.parametros());
        }

        super.visitDeclaracao_global(ctx);

        escopos.deletarEscopoAtual(); // sai do escopo
        tipoRetornoAtual = null;
        return null;
    }

    // Insere parâmetros no escopo
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

    // Declarações locais (variável, constante, tipo)
    @Override
    public Void visitDeclaracao_local(LinguagemAlgoritmicaParser.Declaracao_localContext ctx) {
        // Declaração de constante com verificação de tipo
        if (ctx.CONSTANTE() != null) {
            TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
            String nome = ctx.IDENT().getText();

            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
                return null;
            }

            Tipos tipoDeclarado = obterTipo(ctx.tipo_basico());
            Tipos tipoValor = obterTipo(ctx.valor_constante());

            // Verifica compatibilidade entre tipo e valor
            if (!SemanticoUtils.tiposCompativeis(tipoDeclarado, tipoValor)) {
                SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            }

            escopoAtual.inserir(nome, tipoDeclarado, Categoria.CONSTANTE);
            return null;
        }

        return super.visitDeclaracao_local(ctx);
    }

    // Verificação de atribuição
    @Override
    public Void visitCmdAtribuicao(LinguagemAlgoritmicaParser.CmdAtribuicaoContext ctx) {
        String nome = ctx.identificador().IDENT(0).getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);

        // Variável não declarada
        if (e == null) {
            SemanticoUtils.adicionarErro(ctx.identificador().start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
            return null;
        }

        Tipos tipoVar = e.tipo;
        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos, ctx.expressao());

        // Expressão inválida
        if (tipoExpr == Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
            return null;
        }

        // Regras específicas para ponteiros
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

        // Tipos numéricos são compatíveis entre si
        if (SemanticoUtils.tiposNumericos(tipoVar, tipoExpr)) {
            return null;
        }

        if (tipoVar == tipoExpr) {
            return null;
        }

        // Caso geral de erro
        SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        return null;
    }
}