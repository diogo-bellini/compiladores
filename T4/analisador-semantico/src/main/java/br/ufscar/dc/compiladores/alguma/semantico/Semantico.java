package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.ArrayList;
import java.util.List;

// Classe responsável pela análise semântica utilizando o padrão Visitor
public class Semantico extends LinguagemAlgoritmicaBaseVisitor<Void> {

    Escopos escopos = new Escopos(); // controla os escopos ativos
    Tipos tipoRetornoAtual = null;   // usado para validar "retorne" em funções
    boolean dentroFuncao = false;    // controla se estamos dentro de uma função (não procedimento)

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
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Valor_constanteContext ctx) {
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
        if (escopoAtual.existe(nome)) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
            return null;
        }

        EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
        entrada.nome = nome;

        // procedimento
        if (ctx.PROCEDIMENTO() != null) {
            entrada.tipo = Tipos.INVALIDO;
            entrada.categoria = Categoria.PROCEDIMENTO;
        }

        // função
        if (ctx.FUNCAO() != null) {
            Tipos tipoRetorno = obterTipo(ctx.tipo_estendido());
            entrada.tipo = tipoRetorno;
            entrada.categoria = Categoria.FUNCAO;
            tipoRetornoAtual = tipoRetorno;
            dentroFuncao = true;
        }

        // parâmetros
        entrada.tiposParametros = obterTiposParametros(ctx);
        escopoAtual.inserir(entrada);

        // cria escopo interno
        escopos.criarEscopo();

        // parâmetros entram no escopo
        if (ctx.parametros() != null) {
            visitParametros(ctx.parametros());
        }

        // declarações locais
        for (var declLocal : ctx.declaracao_local()) {
            visitDeclaracao_local(declLocal);
        }

        // comandos
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }

        escopos.deletarEscopoAtual();

        tipoRetornoAtual = null;
        dentroFuncao = false;

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

        // Declaração de tipo definido pelo usuário
        if (ctx.TIPO() != null) {
            TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
            String nome = ctx.IDENT().getText();

            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
                return null;
            }

            Tipos tipo = obterTipo(ctx.tipo());
            escopoAtual.inserir(nome, tipo, Categoria.TIPO);
            return null;
        }

        // Caso DECLARE variavel — delega explicitamente
        if (ctx.variavel() != null) {
            visitVariavel(ctx.variavel());
        }

        return null;
    }

    @Override
    public Void visitVariavel(LinguagemAlgoritmicaParser.VariavelContext ctx) {

        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        Tipos tipo = obterTipo(ctx.tipo());

        for (var ident : ctx.identificador()) {
            String nome = ident.IDENT(0).getText();

            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ident.start, TipoErro.IDENTIFICADOR_REPETIDO);
                continue;
            }

            EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
            entrada.nome = nome;
            entrada.tipo = tipo;
            entrada.categoria = Categoria.VARIAVEL;

            // registro inline
            if (ctx.tipo().registro() != null) {
                entrada.ehRegistro = true;
                entrada.camposRegistro = criarCamposRegistro(ctx.tipo().registro());
            }

            // ponteiro
            if (ctx.tipo().tipo_estendido() != null && ctx.tipo().tipo_estendido().PONTEIRO() != null) {
                entrada.ehPonteiro = true;
                entrada.tipoApontado = obterTipo(ctx.tipo().tipo_estendido().tipo_basico_ident());
            }
            escopoAtual.inserir(entrada);
        }
        return null;
    }

    private TabelaDeSimbolos criarCamposRegistro(LinguagemAlgoritmicaParser.RegistroContext reg) {
        TabelaDeSimbolos campos = new TabelaDeSimbolos();
        for (var variavel : reg.variavel()) {
            Tipos tipoCampo = obterTipo(variavel.tipo());
            for (var identCampo : variavel.identificador()) {

                EntradaTabelaDeSimbolos campo = new EntradaTabelaDeSimbolos();
                campo.nome = identCampo.IDENT(0).getText();
                campo.tipo = tipoCampo;
                campo.categoria = Categoria.VARIAVEL;
                campos.inserir(campo);
            }
        }
        return campos;
    }

    // Visita o corpo do algoritmo: declarações locais e comandos
    @Override
    public Void visitCorpo(LinguagemAlgoritmicaParser.CorpoContext ctx) {
        for (var declLocal : ctx.declaracao_local()) {
            visitDeclaracao_local(declLocal);
        }
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    // Despacha para o visitor correto conforme o tipo de comando
    @Override
    public Void visitCmd(LinguagemAlgoritmicaParser.CmdContext ctx) {
        if (ctx.cmdAtribuicao() != null) return visitCmdAtribuicao(ctx.cmdAtribuicao());
        if (ctx.cmdLeia() != null)       return visitCmdLeia(ctx.cmdLeia());
        if (ctx.cmdEscreva() != null)    return visitCmdEscreva(ctx.cmdEscreva());
        if (ctx.cmdSe() != null)         return visitCmdSe(ctx.cmdSe());
        if (ctx.cmdCaso() != null)       return visitCmdCaso(ctx.cmdCaso());
        if (ctx.cmdPara() != null)       return visitCmdPara(ctx.cmdPara());
        if (ctx.cmdEnquanto() != null)   return visitCmdEnquanto(ctx.cmdEnquanto());
        if (ctx.cmdChamada() != null)    return visitCmdChamada(ctx.cmdChamada());
        if (ctx.cmdRetorne() != null)    return visitCmdRetorne(ctx.cmdRetorne());
        return null;
    }

    // Verifica se os identificadores lidos estão declarados
    @Override
    public Void visitCmdLeia(LinguagemAlgoritmicaParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            String nome = ident.IDENT(0).getText();
            if (escopos.buscar(nome) == null) {
                SemanticoUtils.adicionarErro(ident.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
            }
        }
        return null;
    }

    // Verifica tipos das expressões escritas
    @Override
    public Void visitCmdEscreva(LinguagemAlgoritmicaParser.CmdEscrevaContext ctx) {
        for (var expr : ctx.expressao()) {
            SemanticoUtils.verificarTipo(escopos, expr);
        }
        return null;
    }

    // Verifica condição e visita ramos do se/senao
    @Override
    public Void visitCmdSe(LinguagemAlgoritmicaParser.CmdSeContext ctx) {
        SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    // Verifica expressão do caso e visita seleções e senao
    @Override
    public Void visitCmdCaso(LinguagemAlgoritmicaParser.CmdCasoContext ctx) {
        SemanticoUtils.verificarTipo(escopos, ctx.exp_aritmetica());
        if (ctx.selecao() != null) {
            visitSelecao(ctx.selecao());
        }
        // Comandos do senao do caso
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    // Visita cada item da seleção e seus comandos
    @Override
    public Void visitSelecao(LinguagemAlgoritmicaParser.SelecaoContext ctx) {
        for (var item : ctx.item_selecao()) {
            for (var cmd : item.cmd()) {
                visitCmd(cmd);
            }
        }
        return null;
    }

    // Verifica variável de controle e limites do para
    @Override
    public Void visitCmdPara(LinguagemAlgoritmicaParser.CmdParaContext ctx) {
        String nome = ctx.IDENT().getText();
        if (escopos.buscar(nome) == null) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_NAO_DECLARADO);
        }
        SemanticoUtils.verificarTipo(escopos, ctx.exp_aritmetica(0));
        SemanticoUtils.verificarTipo(escopos, ctx.exp_aritmetica(1));
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    // Verifica condição do enquanto e seus comandos
    @Override
    public Void visitCmdEnquanto(LinguagemAlgoritmicaParser.CmdEnquantoContext ctx) {
        SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    // Verifica se o procedimento/função chamada está declarada e a compatibilidade dos parâmetros
    @Override
    public Void visitCmdChamada(LinguagemAlgoritmicaParser.CmdChamadaContext ctx) {
        String nome = ctx.IDENT().getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);
        if (e == null) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_NAO_DECLARADO);
            return null;
        }

        List<Tipos> tiposPassados = new ArrayList<>();

        for (var expr : ctx.expressao()) {
            tiposPassados.add(SemanticoUtils.verificarTipo(escopos, expr));
        }

        List<Tipos> tiposEsperados = e.tiposParametros;

        if (tiposEsperados.size() != tiposPassados.size()) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.INCOMPATIBILIDADE_PARAMETROS);
            return null;
        }

        for (int i = 0; i < tiposEsperados.size(); i++) {

            if (!SemanticoUtils.tiposCompativeis(
                    tiposEsperados.get(i),
                    tiposPassados.get(i)
            )) {

                SemanticoUtils.adicionarErro(
                        ctx.IDENT().getSymbol(),
                        TipoErro.INCOMPATIBILIDADE_PARAMETROS
                );

                break;
            }
        }

        return null;
    }

    // Sobe na árvore até o nó programa e busca a declaração global pelo nome
    private LinguagemAlgoritmicaParser.Declaracao_globalContext buscarDeclaracaoGlobal(LinguagemAlgoritmicaParser.CmdChamadaContext ctx, String nome) {
        org.antlr.v4.runtime.tree.ParseTree node = ctx;
        while (node != null && !(node instanceof LinguagemAlgoritmicaParser.ProgramaContext)) {
            node = node.getParent();
        }
        if (node == null) return null;

        LinguagemAlgoritmicaParser.ProgramaContext programa = (LinguagemAlgoritmicaParser.ProgramaContext) node;
        for (var declLocalGlobal : programa.declaracoes().decl_local_global()) {
            if (declLocalGlobal.declaracao_global() != null) {
                LinguagemAlgoritmicaParser.Declaracao_globalContext dg = declLocalGlobal.declaracao_global();
                if (dg.IDENT().getText().equals(nome)) {
                    return dg;
                }
            }
        }
        return null;
    }

    // Extrai a lista ordenada de tipos dos parâmetros de uma declaração global
    private List<Tipos> obterTiposParametros(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {
        List<Tipos> tipos = new ArrayList<>();
        if (ctx.parametros() == null) return tipos;

        for (var parametro : ctx.parametros().parametro()) {
            Tipos tipo = obterTipo(parametro.tipo_estendido());
            // Cada identificador no parâmetro conta como um argumento esperado
            for (var ident : parametro.identificador()) {
                tipos.add(tipo);
            }
        }
        return tipos;
    }

    // Verifica tipo do retorne contra o tipo de retorno da função atual
    // Retorne fora de função (inclusive em procedimento) é erro semântico
    @Override
    public Void visitCmdRetorne(LinguagemAlgoritmicaParser.CmdRetorneContext ctx) {
        if (!dentroFuncao) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.RETORNE_FORA_FUNCAO);
            return null;
        }
        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        if (tipoRetornoAtual != null && !SemanticoUtils.tiposCompativeis(tipoRetornoAtual, tipoExpr)) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL);
        }
        return null;
    }

    // Verificação de atribuição
    // Trata identificadores simples, acesso a campos de registro e vetores
    @Override
    public Void visitCmdAtribuicao(LinguagemAlgoritmicaParser.CmdAtribuicaoContext ctx) {
        LinguagemAlgoritmicaParser.IdentificadorContext identCtx = ctx.identificador();
        String nomeBase = identCtx.IDENT(0).getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nomeBase);

        if (e == null) {
            SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
            return null;
        }
        Tipos tipoVar = e.tipo;

        // campo de registro
        if (identCtx.IDENT().size() > 1) {
            if (!e.ehRegistro) {
                SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
                return null;
            }

            String campoNome = identCtx.IDENT(1).getText();
            EntradaTabelaDeSimbolos campo = e.camposRegistro.buscar(campoNome);

            if (campo == null) {
                SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
                return null;
            }
            tipoVar = campo.tipo;
        }

        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos,ctx.expressao());
        String nome;
        if (ctx.PONTEIRO() != null) {
            nome = "^" + identCtx.getText();
        } else {
            nome = identCtx.getText();
        }

        if (tipoExpr == Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
            return null;
        }

        // ^ponteiro <- expr
        if (ctx.PONTEIRO() != null) {
            if (!e.ehPonteiro) {
                SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
                return null;
            }

            if (!SemanticoUtils.tiposCompativeis(e.tipoApontado, tipoExpr)) {
                SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
            }
            return null;
        }

        // ponteiro <- &x
        if (e.ehPonteiro) {
            if (tipoExpr != Tipos.ENDERECO) {
                SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
            }
            return null;
        }

        // endereço para não ponteiro
        if (tipoExpr == Tipos.ENDERECO) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
            return null;
        }

        if (!SemanticoUtils.tiposCompativeis(tipoVar, tipoExpr)) {
            SemanticoUtils.adicionarErro(ctx.start,TipoErro.ATRIBUICAO_NAO_COMPATIVEL,nome);
        }
        return null;
    }
}