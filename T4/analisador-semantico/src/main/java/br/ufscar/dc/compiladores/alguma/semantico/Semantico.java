package br.ufscar.dc.compiladores.alguma.semantico;

import java.util.ArrayList;
import java.util.List;

// Classe responsável pela análise semântica utilizando o padrão Visitor
public class Semantico extends LinguagemAlgoritmicaBaseVisitor<Void> {

    Escopos escopos = new Escopos(); // Gerencia a pilha de escopos (locais e globais)
    Tipos tipoRetornoAtual = null;   // Armazena o tipo esperado de retorno da função que está sendo visitada
    boolean dentroFuncao = false;    // Flag para validar se o comando 'retorne' está em local permitido

    // Obtém o tipo a partir de um contexto de tipo, tratando registros inline
    private Tipos obterTipo(LinguagemAlgoritmicaParser.TipoContext ctx) {
        if (ctx.registro() != null) {
            return Tipos.REGISTRO;
        }
        return obterTipo(ctx.tipo_estendido());
    }

    // Identifica se o tipo é um ponteiro e resolve o tipo base
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_estendidoContext ctx) {
        if (ctx.PONTEIRO() != null) {
            return Tipos.PONTEIRO;
        }
        return obterTipo(ctx.tipo_basico_ident());
    }

    // Resolve tipo básico ou busca identificadores de tipos customizados na tabela
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_basico_identContext ctx) {
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
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Tipo_basicoContext ctx) {
        if (ctx.INTEIRO() != null) return Tipos.INTEIRO;
        if (ctx.REAL() != null) return Tipos.REAL;
        if (ctx.LOGICO() != null) return Tipos.LOGICO;
        if (ctx.LITERAL() != null) return Tipos.LITERAL;
        return Tipos.INVALIDO;
    }

    // Resolve o tipo de constantes literais para validação de atribuições em tempo de declaração
    private Tipos obterTipo(LinguagemAlgoritmicaParser.Valor_constanteContext ctx) {
        if (ctx.NUM_INT() != null) return Tipos.INTEIRO;
        if (ctx.NUM_REAL() != null) return Tipos.REAL;
        if (ctx.CADEIA() != null) return Tipos.LITERAL;
        if (ctx.VERDADEIRO() != null || ctx.FALSO() != null) return Tipos.LOGICO;
        return Tipos.INVALIDO;
    }

    // Ponto de entrada da análise semântica: percorre declarações e o corpo principal
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

    // Decide entre declaração local (variáveis/tipos) ou global (funções/procedimentos)
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

    // Trata a definição de funções e procedimentos, gerindo a abertura e fechamento de escopos
    @Override
    public Void visitDeclaracao_global(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        String nome = ctx.IDENT().getText();
        
        // Verifica se o nome da função já foi utilizado no escopo global
        if (escopoAtual.existe(nome)) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
            return null;
        }

        EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
        entrada.nome = nome;

        // Configura entrada como Procedimento (sem tipo de retorno)
        if (ctx.PROCEDIMENTO() != null) {
            entrada.tipo = Tipos.INVALIDO;
            entrada.categoria = Categoria.PROCEDIMENTO;
        }

        // Configura entrada como Função e define o contexto de retorno para validação interna
        if (ctx.FUNCAO() != null) {
            Tipos tipoRetorno = obterTipo(ctx.tipo_estendido());
            entrada.tipo = tipoRetorno;
            entrada.categoria = Categoria.FUNCAO;
            tipoRetornoAtual = tipoRetorno;
            dentroFuncao = true;
        }

        // Registra os tipos dos parâmetros para validar futuras chamadas
        entrada.tiposParametros = obterTiposParametros(ctx);
        escopoAtual.inserir(entrada);

        // Cria um novo escopo para o corpo da função/procedimento
        escopos.criarEscopo();

        // Insere os parâmetros formais como variáveis dentro do novo escopo
        if (ctx.parametros() != null) {
            visitParametros(ctx.parametros());
        }

        // Visita as declarações locais e comandos internos
        for (var declLocal : ctx.declaracao_local()) {
            visitDeclaracao_local(declLocal);
        }

        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }

        // Remove o escopo ao finalizar a visita da função
        escopos.deletarEscopoAtual();

        // Limpa o contexto de função
        tipoRetornoAtual = null;
        dentroFuncao = false;

        return null;
    }

    // Insere parâmetros no escopo atual, tratando tipos complexos e ponteiros
    @Override
    public Void visitParametro(LinguagemAlgoritmicaParser.ParametroContext ctx) {
        TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
        Tipos tipo = obterTipo(ctx.tipo_estendido());
        for (var ident : ctx.identificador()) {
            String nome = ident.IDENT(0).getText();
            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ident.start, TipoErro.IDENTIFICADOR_REPETIDO);
                continue;
            }

            EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
            entrada.nome = nome;
            entrada.tipo = tipo;
            entrada.categoria = Categoria.PARAMETRO;

            // Tratamento especial para parâmetros que são ponteiros
            if (ctx.tipo_estendido().PONTEIRO() != null) {
                entrada.ehPonteiro = true;
                entrada.tipoApontado = obterTipo(ctx.tipo_estendido().tipo_basico_ident());
            }

            // Tratamento para parâmetros que utilizam tipos (registros) pré-definidos
            if (ctx.tipo_estendido().tipo_basico_ident().IDENT() != null) {
                String nomeTipo = ctx.tipo_estendido().tipo_basico_ident().IDENT().getText();
                EntradaTabelaDeSimbolos tipoDecl = escopos.buscar(nomeTipo);

                if (tipoDecl != null && tipoDecl.ehRegistro) {
                    entrada.ehRegistro = true;
                    entrada.camposRegistro = tipoDecl.camposRegistro;
                    entrada.nomeTipo = nomeTipo;
                }
            }
            escopoAtual.inserir(entrada);
        }
        return null;
    }

    // Declarações locais: trata a criação de constantes, novos tipos e variáveis
    @Override
    public Void visitDeclaracao_local(LinguagemAlgoritmicaParser.Declaracao_localContext ctx) {
        // Registro de constantes com verificação de compatibilidade de valor inicial
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

        // Registro de novos tipos (aliases ou registros/structs)
        if (ctx.TIPO() != null) {
            TabelaDeSimbolos escopoAtual = escopos.obterEscopoAtual();
            String nome = ctx.IDENT().getText();

            if (escopoAtual.existe(nome)) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_REPETIDO);
                return null;
            }

            EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
            entrada.nome = nome;
            entrada.tipo = obterTipo(ctx.tipo());
            entrada.categoria = Categoria.TIPO;

            // Se for um registro, cria uma sub-tabela para seus campos
            if (ctx.tipo().registro() != null) {
                entrada.ehRegistro = true;
                entrada.camposRegistro = criarCamposRegistro(ctx.tipo().registro());
            }
            escopoAtual.inserir(entrada);
            return null;
        }

        // Delegação para visita de declaração de variáveis comuns
        if (ctx.variavel() != null) {
            visitVariavel(ctx.variavel());
        }

        return null;
    }

    // Processa a declaração de variáveis, tratando ponteiros e registros inline
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

            // Caso de registro declarado diretamente na variável (inline)
            if (ctx.tipo().registro() != null) {
                entrada.ehRegistro = true;
                entrada.camposRegistro = criarCamposRegistro(ctx.tipo().registro());
            }

            // Identifica se a variável é um ponteiro para um tipo básico
            if (ctx.tipo().tipo_estendido() != null && ctx.tipo().tipo_estendido().PONTEIRO() != null) {
                entrada.ehPonteiro = true;
                entrada.tipoApontado = obterTipo(ctx.tipo().tipo_estendido().tipo_basico_ident());
            }

            // Associa a estrutura de um tipo customizado já declarado à variável
            if (ctx.tipo().tipo_estendido() != null && ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT() != null) {
                String nomeTipo = ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT().getText();
                EntradaTabelaDeSimbolos tipoDeclarado = escopos.buscar(nomeTipo);

                if (tipoDeclarado != null && tipoDeclarado.ehRegistro) {
                    entrada.ehRegistro = true;
                    entrada.camposRegistro = tipoDeclarado.camposRegistro;
                    entrada.nomeTipo = nomeTipo;
                }
            }
            escopoAtual.inserir(entrada);
        }
        return null;
    }

    // Método auxiliar que constrói a tabela de símbolos interna de um registro (escopo aninhado)
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

    // Visita o corpo do algoritmo, processando comandos sequencialmente
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

    // Despachante principal de comandos (Estrutura de Controle de Visitação)
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
        if (ctx.cmdFaca() != null)       return visitCmdFaca(ctx.cmdFaca());
        return null;
    }

    // Valida se as variáveis passadas ao comando 'leia' existem nos escopos
    @Override
    public Void visitCmdLeia(LinguagemAlgoritmicaParser.CmdLeiaContext ctx) {
        for (var ident : ctx.identificador()) {
            SemanticoUtils.verificarTipo(escopos, ident);
        }
        return null;
    }

    // Dispara a verificação de tipo para cada expressão contida no comando 'escreva'
    @Override
    public Void visitCmdEscreva(LinguagemAlgoritmicaParser.CmdEscrevaContext ctx) {
        for (var expr : ctx.expressao()) {
            SemanticoUtils.verificarTipo(escopos, expr);
        }
        return null;
    }

    // Valida a condição booleana do comando 'se' e visita seus blocos de comandos
    @Override
    public Void visitCmdSe(LinguagemAlgoritmicaParser.CmdSeContext ctx) {
        SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    // Valida a expressão seletora do comando 'caso' e as seleções internas
    @Override
    public Void visitCmdCaso(LinguagemAlgoritmicaParser.CmdCasoContext ctx) {
        SemanticoUtils.verificarTipo(escopos, ctx.exp_aritmetica());
        if (ctx.selecao() != null) {
            visitSelecao(ctx.selecao());
        }
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    @Override
    public Void visitSelecao(LinguagemAlgoritmicaParser.SelecaoContext ctx) {
        for (var item : ctx.item_selecao()) {
            for (var cmd : item.cmd()) {
                visitCmd(cmd);
            }
        }
        return null;
    }

    // Valida limites do laço 'para' e garante que a variável de controle existe
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

    // Valida a expressão condicional do laço 'enquanto'
    @Override
    public Void visitCmdEnquanto(LinguagemAlgoritmicaParser.CmdEnquantoContext ctx) {
        SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        return null;
    }

    // Valida comandos e condição do laço 'faca-enquanto'
    @Override
    public Void visitCmdFaca(LinguagemAlgoritmicaParser.CmdFacaContext ctx) {
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }
        SemanticoUtils.verificarTipo(escopos, ctx.expressao());
        return null;
    }

    // Valida chamadas de sub-rotinas: existência da rotina e compatibilidade da lista de argumentos
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

        // Erro se a quantidade de argumentos for diferente do esperado
        if (tiposEsperados.size() != tiposPassados.size()) {
            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.INCOMPATIBILIDADE_PARAMETROS);
            return null;
        }

        // Valida um a um a compatibilidade dos tipos passados
        for (int i = 0; i < tiposEsperados.size(); i++) {
            if (!SemanticoUtils.tiposCompativeis(tiposEsperados.get(i), tiposPassados.get(i))) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.INCOMPATIBILIDADE_PARAMETROS);
                break;
            }
        }
        return null;
    }

    // Auxiliar para extrair a assinatura (tipos) dos parâmetros de uma função/procedimento
    private List<Tipos> obterTiposParametros(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {
        List<Tipos> tipos = new ArrayList<>();
        if (ctx.parametros() == null) return tipos;

        for (var parametro : ctx.parametros().parametro()) {
            Tipos tipo = obterTipo(parametro.tipo_estendido());
            for (var ident : parametro.identificador()) {
                tipos.add(tipo);
            }
        }
        return tipos;
    }

    // Valida o comando 'retorne': deve estar dentro de uma função e o tipo deve ser compatível
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

    // Lógica complexa de atribuição: valida tipos de variáveis simples, campos de registro e ponteiros
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

        // Lógica de acesso a campo de registro (ex: p.nome)
        if (identCtx.IDENT().size() > 1) {
            if (!e.ehRegistro || e.camposRegistro == null) {
                SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, identCtx.getText());
                return null;
            }

            String campoNome = identCtx.IDENT(1).getText();
            EntradaTabelaDeSimbolos campo = e.camposRegistro.buscar(campoNome);

            if (campo == null) {
                SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, identCtx.getText());
                return null;
            }
            tipoVar = campo.tipo;
        }

        Tipos tipoExpr = SemanticoUtils.verificarTipo(escopos,ctx.expressao());
        String nome = ctx.PONTEIRO() != null ? "^" + identCtx.getText() : identCtx.getText();

        if (tipoExpr == Tipos.INVALIDO) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
            return null;
        }

        // Validação de desreferenciamento de ponteiro (^ponteiro <- valor)
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

        // Validação de atribuição de endereço a um ponteiro (ponteiro <- &var)
        if (e.ehPonteiro) {
            if (tipoExpr != Tipos.ENDERECO) {
                SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
            }
            return null;
        }

        // Impede atribuição de endereços a variáveis comuns
        if (tipoExpr == Tipos.ENDERECO) {
            SemanticoUtils.adicionarErro(ctx.start, TipoErro.ATRIBUICAO_NAO_COMPATIVEL, nome);
            return null;
        }

        // Validação de compatibilidade de tipos padrão
        if (!SemanticoUtils.tiposCompativeis(tipoVar, tipoExpr)) {
            SemanticoUtils.adicionarErro(ctx.start,TipoErro.ATRIBUICAO_NAO_COMPATIVEL,nome);
        }
        return null;
    }
}