package br.ufscar.dc.compiladores.alguma.gerador;

/**
 * GeradorC — converte a árvore sintática da Linguagem Algorítmica em código C.
 *
 * Decisões gerais:
 *  - A tabela de símbolos (escopos) é populada durante o próprio walk do gerador,
 *    pois sem ela não é possível resolver tipos para scanf/printf.
 *  - Cada regra da gramática tem um visitor correspondente ou um helper privado.
 *  - Nenhum visitor silencia erros com try/catch sem tratamento.
 */
public class GeradorC extends LinguagemAlgoritmicaBaseVisitor<Void> {

    Escopos escopos = new Escopos();
    StringBuilder saida = new StringBuilder();

    public String getCodigo() {
        return saida.toString();
    }

    // =========================================================
    // Helpers: geração de expressões (sem efeito colateral)
    // =========================================================

    private String gerarExpressao(LinguagemAlgoritmicaParser.ExpressaoContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(gerarTermoLogico(ctx.termo_logico(0)));
        for (int i = 1; i < ctx.termo_logico().size(); i++) {
            sb.append(" ")
                    .append(CUtils.operadorC(ctx.op_logico_1(i - 1).getText()))
                    .append(" ")
                    .append(gerarTermoLogico(ctx.termo_logico(i)));
        }
        return sb.toString();
    }

    private String gerarTermoLogico(LinguagemAlgoritmicaParser.Termo_logicoContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(gerarFatorLogico(ctx.fator_logico(0)));
        for (int i = 1; i < ctx.fator_logico().size(); i++) {
            sb.append(" ")
                    .append(CUtils.operadorC(ctx.op_logico_2(i - 1).getText()))
                    .append(" ")
                    .append(gerarFatorLogico(ctx.fator_logico(i)));
        }
        return sb.toString();
    }

    private String gerarFatorLogico(LinguagemAlgoritmicaParser.Fator_logicoContext ctx) {
        String parcela = gerarParcelaLogica(ctx.parcela_logica());
        return ctx.NAO() != null ? "!" + parcela : parcela;
    }

    private String gerarParcelaLogica(LinguagemAlgoritmicaParser.Parcela_logicaContext ctx) {
        if (ctx.VERDADEIRO() != null) return "1";
        if (ctx.FALSO()     != null) return "0";
        return gerarExpRelacional(ctx.exp_relacional());
    }

    private String gerarExpRelacional(LinguagemAlgoritmicaParser.Exp_relacionalContext ctx) {
        String esquerda = gerarExpAritmetica(ctx.exp_aritmetica(0));
        if (ctx.op_relacional() == null) return esquerda;
        String op      = CUtils.operadorC(ctx.op_relacional().getText());
        String direita = gerarExpAritmetica(ctx.exp_aritmetica(1));
        return esquerda + " " + op + " " + direita;
    }

    private String gerarExpAritmetica(LinguagemAlgoritmicaParser.Exp_aritmeticaContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(gerarTermo(ctx.termo(0)));
        for (int i = 1; i < ctx.termo().size(); i++) {
            sb.append(" ").append(ctx.op1(i - 1).getText()).append(" ")
                    .append(gerarTermo(ctx.termo(i)));
        }
        return sb.toString();
    }

    private String gerarTermo(LinguagemAlgoritmicaParser.TermoContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(gerarFator(ctx.fator(0)));
        for (int i = 1; i < ctx.fator().size(); i++) {
            sb.append(" ").append(ctx.op2(i - 1).getText()).append(" ")
                    .append(gerarFator(ctx.fator(i)));
        }
        return sb.toString();
    }

    private String gerarFator(LinguagemAlgoritmicaParser.FatorContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(gerarParcela(ctx.parcela(0)));
        for (int i = 1; i < ctx.parcela().size(); i++) {
            sb.append(" ").append(ctx.op3(i - 1).getText()).append(" ")
                    .append(gerarParcela(ctx.parcela(i)));
        }
        return sb.toString();
    }

    private String gerarParcela(LinguagemAlgoritmicaParser.ParcelaContext ctx) {
        // parcela: op_unario? parcela_unario | parcela_nao_unario
        if (ctx.parcela_unario() != null) {
            String pu = gerarParcelaUnario(ctx.parcela_unario());
            return ctx.op_unario() != null ? "-" + pu : pu;
        }
        return gerarParcelaNaoUnario(ctx.parcela_nao_unario());
    }

    private String gerarParcelaUnario(LinguagemAlgoritmicaParser.Parcela_unarioContext ctx) {
        // PONTEIRO? identificador
        if (ctx.identificador() != null) {
            String ident = gerarIdentificador(ctx.identificador());
            return ctx.PONTEIRO() != null ? "*" + ident : ident;
        }
        // IDENT '(' expressao (',' expressao)* ')'  → chamada de função
        if (ctx.IDENT() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(ctx.IDENT().getText()).append("(");
            for (int i = 0; i < ctx.expressao().size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(gerarExpressao(ctx.expressao(i)));
            }
            sb.append(")");
            return sb.toString();
        }
        if (ctx.NUM_INT()  != null) return ctx.NUM_INT().getText();
        if (ctx.NUM_REAL() != null) return ctx.NUM_REAL().getText();
        // '(' expressao ')'
        if (!ctx.expressao().isEmpty())
            return "(" + gerarExpressao(ctx.expressao(0)) + ")";
        return "";
    }

    private String gerarParcelaNaoUnario(LinguagemAlgoritmicaParser.Parcela_nao_unarioContext ctx) {
        // ENDERECO identificador
        if (ctx.identificador() != null) return "&" + gerarIdentificador(ctx.identificador());
        // CADEIA
        if (ctx.CADEIA() != null) return ctx.CADEIA().getText();
        return "";
    }

    private String gerarIdentificador(LinguagemAlgoritmicaParser.IdentificadorContext ctx) {
        // identificador: IDENT ('.' IDENT)* dimensao
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.IDENT(0).getText());
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            sb.append(".").append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao() != null) sb.append(gerarDimensao(ctx.dimensao()));
        return sb.toString();
    }

    private String gerarDimensao(LinguagemAlgoritmicaParser.DimensaoContext ctx) {
        // dimensao: ('[' exp_aritmetica ']')*
        StringBuilder sb = new StringBuilder();
        for (var exp : ctx.exp_aritmetica()) {
            sb.append("[").append(gerarExpAritmetica(exp)).append("]");
        }
        return sb.toString();
    }

    // =========================================================
    // Helpers: tipos e formatos scanf/printf
    // =========================================================

    /**
     * Retorna o especificador de formato C para um tipo.
     * Usado tanto em scanf quanto em printf.
     */
    private String especificador(Tipos t) {
        switch (t) {
            case REAL:    return "%f";
            case LITERAL: return "%s";
            default:      return "%d";   // INTEIRO, LOGICO, PONTEIRO, INVALIDO
        }
    }

    /**
     * Resolve o tipo de um identificador consultando todos os escopos ativos.
     * Retorna INVALIDO quando não encontrado.
     */
    private Tipos resolverTipo(String nome) {
        EntradaTabelaDeSimbolos e = escopos.buscar(nome);
        return e != null ? e.tipo : Tipos.INVALIDO;
    }

    /**
     * Insere um identificador com seu tipo e categoria no escopo atual.
     * Evita duplicatas (permite redeclaração silenciosa, como a fase semântica já validou).
     */
    private void inserirSimbolos(LinguagemAlgoritmicaParser.VariavelContext ctx, Tipos tipo, Categoria categoria) {
        for (var ident : ctx.identificador()) {
            String nome = ident.IDENT(0).getText();
            escopos.obterEscopoAtual().inserir(nome, tipo, categoria);
        }
    }

    /**
     * Infere o especificador de formato de uma expressão para printf.
     *
     * Estratégia: desce até a parcela mais interna da primeira subexpressão.
     *  - CADEIA        → %s
     *  - NUM_REAL      → %f
     *  - identificador → consulta tabela de símbolos
     *  - expressão com op_relacional ou op_logico → resultado é inteiro (0/1) → %d
     *  - NUM_INT       → %d
     *  - caso contrário → %d
     */
    private String inferirFormato(LinguagemAlgoritmicaParser.ExpressaoContext ctx) {
        // Se há operador lógico, resultado é booleano (int em C)
        if (ctx.termo_logico().size() > 1) return "%d";

        var tl = ctx.termo_logico(0);
        if (tl.fator_logico().size() > 1) return "%d";

        var fl = tl.fator_logico(0);
        // NAO → resultado lógico
        if (fl.NAO() != null) return "%d";

        var pl = fl.parcela_logica();
        if (pl.VERDADEIRO() != null || pl.FALSO() != null) return "%d";

        var er = pl.exp_relacional();
        // Se há operador relacional, resultado é booleano
        if (er.op_relacional() != null) return "%d";

        var ea = er.exp_aritmetica(0);
        // Se há soma/subtração entre termos, pode ser real ou int; usamos o tipo do primeiro operando
        // (simplificação razoável para a linguagem)

        var t0 = ea.termo(0);
        var f0 = t0.fator(0);
        var p0 = f0.parcela(0);

        if (p0.parcela_nao_unario() != null) {
            var pnu = p0.parcela_nao_unario();
            if (pnu.CADEIA() != null) return "%s";
            // &identificador → endereço → %p seria correto, mas %d é tolerável
            return "%d";
        }

        if (p0.parcela_unario() != null) {
            var pu = p0.parcela_unario();
            if (pu.NUM_REAL() != null) return "%f";
            if (pu.NUM_INT()  != null) return "%d";
            if (pu.identificador() != null) {
                String raiz = pu.identificador().IDENT(0).getText();
                return especificador(resolverTipo(raiz));
            }
            // chamada de função ou subexpressão entre parênteses → %d como fallback
        }
        return "%d";
    }

    // =========================================================
    // Programa
    // =========================================================

    @Override
    public Void visitPrograma(LinguagemAlgoritmicaParser.ProgramaContext ctx) {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n");
        saida.append("#include <string.h>\n\n");

        // Declarações globais (funções e procedimentos) vêm antes do main
        visitDeclaracoes(ctx.declaracoes());

        saida.append("int main() {\n");
        visitCorpo(ctx.corpo());
        saida.append("return 0;\n}\n");
        return null;
    }

    // =========================================================
    // Declarações globais
    // =========================================================

    @Override
    public Void visitDeclaracoes(LinguagemAlgoritmicaParser.DeclaracoesContext ctx) {
        for (var dlg : ctx.decl_local_global()) {
            visitDecl_local_global(dlg);
        }
        return null;
    }

    @Override
    public Void visitDecl_local_global(LinguagemAlgoritmicaParser.Decl_local_globalContext ctx) {
        if (ctx.declaracao_global() != null) {
            visitDeclaracao_global(ctx.declaracao_global());
        } else {
            // declaracao_local no escopo global: processa para popular escopos
            // mas não gera código fora de funções (tipos e constantes globais)
            visitDeclaracao_local(ctx.declaracao_local());
        }
        return null;
    }

    /**
     * Gera funções e procedimentos em C.
     *
     * - PROCEDIMENTO → void em C
     * - FUNCAO       → tipo de retorno correspondente
     * - Parâmetro VAR → passagem por ponteiro (tipo *nome)
     * - Parâmetro sem VAR → passagem por valor
     * - LITERAL sempre vira char* (array já é ponteiro em C)
     *
     * Um novo escopo é criado para os parâmetros e o corpo da função.
     */
    @Override
    public Void visitDeclaracao_global(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {
        escopos.criarEscopo();

        boolean ehFuncao = ctx.FUNCAO() != null;

        // Tipo de retorno
        if (ehFuncao) {
            Tipos tipoRet = TipoUtils.obterTipo(escopos, ctx.tipo_estendido());
            saida.append(CUtils.tipoC(tipoRet));
        } else {
            saida.append("void");
        }
        saida.append(" ").append(ctx.IDENT().getText()).append("(");

        // Parâmetros
        if (ctx.parametros() != null) {
            boolean primeiro = true;
            for (var param : ctx.parametros().parametro()) {
                boolean porRef  = param.VAR() != null;
                Tipos tipoParam = TipoUtils.obterTipo(escopos, param.tipo_estendido());

                for (var ident : param.identificador()) {
                    if (!primeiro) saida.append(", ");
                    primeiro = false;

                    String nome = gerarIdentificador(ident);
                    // Insere no escopo para resolverTipo funcionar dentro da função
                    escopos.obterEscopoAtual().inserir(ident.IDENT(0).getText(), tipoParam, Categoria.VARIAVEL);

                    if (tipoParam == Tipos.LITERAL) {
                        // char[] decai para char* na passagem
                        saida.append("char *").append(nome);
                    } else {
                        saida.append(CUtils.tipoC(tipoParam));
                        saida.append(porRef ? " *" : " ");
                        saida.append(nome);
                    }
                }
            }
        }
        saida.append(") {\n");

        // Declarações locais da função
        for (var declLocal : ctx.declaracao_local()) {
            visitDeclaracao_local(declLocal);
        }
        // Comandos da função
        for (var cmd : ctx.cmd()) {
            visitCmd(cmd);
        }

        saida.append("}\n\n");
        escopos.deletarEscopoAtual();
        return null;
    }

    // =========================================================
    // Corpo e declarações locais
    // =========================================================

    @Override
    public Void visitCorpo(LinguagemAlgoritmicaParser.CorpoContext ctx) {
        for (var decl : ctx.declaracao_local()) visitDeclaracao_local(decl);
        for (var cmd  : ctx.cmd())              visitCmd(cmd);
        return null;
    }

    /**
     * declaracao_local: DECLARE variavel
     *                 | CONSTANTE IDENT ':' tipo_basico '=' valor_constante
     *                 | TIPO IDENT ':' tipo
     *
     * CONSTANTE → gera #define antes do bloco (ou const em C).
     *   Aqui optamos por gerar como variável const local para manter escopo.
     * TIPO → gera typedef correspondente.
     */
    @Override
    public Void visitDeclaracao_local(LinguagemAlgoritmicaParser.Declaracao_localContext ctx) {
        if (ctx.variavel() != null) {
            visitVariavel(ctx.variavel());
            return null;
        }

        // CONSTANTE IDENT ':' tipo_basico '=' valor_constante
        if (ctx.CONSTANTE() != null) {
            String nome       = ctx.IDENT().getText();
            Tipos tipo        = TipoUtils.obterTipo(ctx.tipo_basico());
            String valorTexto = ctx.valor_constante().getText();

            // Insere no escopo como constante
            escopos.obterEscopoAtual().inserir(nome, tipo, Categoria.CONSTANTE);

            saida.append("const ").append(CUtils.tipoC(tipo)).append(" ")
                    .append(nome).append(" = ");

            if (tipo == Tipos.LITERAL) {
                // valor_constante de literal é uma CADEIA → atribuição direta não funciona para char[]
                // usamos char* const para constante de string
                // (reescreve a linha já emitida — fazemos mais simples: char* const nome = "...")
                // Desfaz o que foi emitido acima e reescreve
                int pos = saida.lastIndexOf("const ");
                saida.delete(pos, saida.length());
                saida.append("char * const ").append(nome).append(" = ").append(valorTexto).append(";\n");
            } else {
                saida.append(valorTexto).append(";\n");
            }
            return null;
        }

        // TIPO IDENT ':' tipo
        if (ctx.TIPO() != null) {
            String nome = ctx.IDENT().getText();
            Tipos tipo  = TipoUtils.obterTipo(escopos, ctx.tipo());

            // Insere no escopo como tipo para que outros possam referenciá-lo
            escopos.obterEscopoAtual().inserir(nome, tipo, Categoria.TIPO);

            if (tipo == Tipos.REGISTRO) {
                // Gera typedef struct
                saida.append("typedef struct {\n");
                for (var campo : ctx.tipo().registro().variavel()) {
                    gerarCamposRegistro(campo);
                }
                saida.append("} ").append(nome).append(";\n");
            } else {
                saida.append("typedef ").append(CUtils.tipoC(tipo))
                        .append(" ").append(nome).append(";\n");
            }
            return null;
        }

        return null;
    }

    /**
     * Gera declaração de variáveis em C e popula o escopo.
     *
     * Casos:
     *  - LITERAL       → char nome[80]
     *  - REGISTRO      → struct anônima inline
     *  - PONTEIRO      → tipo_base *nome
     *  - tipo definido pelo usuário (IDENT) → usa o nome do typedef
     *  - demais        → mapeamento direto via CUtils.tipoC
     *
     * Vetores/matrizes: as dimensões vêm no identificador (gramática).
     * Ex: v[10] → identificador já carrega "[10]" via gerarDimensao.
     */
    @Override
    public Void visitVariavel(LinguagemAlgoritmicaParser.VariavelContext ctx) {
        Tipos tipo = TipoUtils.obterTipo(escopos, ctx.tipo());

        // Popula o escopo para todos os identificadores desta declaração
        inserirSimbolos(ctx, tipo, Categoria.VARIAVEL);

        if (tipo == Tipos.REGISTRO) {
            for (var ident : ctx.identificador()) {
                saida.append("struct {\n");
                for (var campo : ctx.tipo().registro().variavel()) {
                    gerarCamposRegistro(campo);
                }
                saida.append("} ").append(gerarIdentificador(ident)).append(";\n");
            }
            return null;
        }

        if (tipo == Tipos.PONTEIRO) {
            // Descobre o tipo base (o que está sendo apontado)
            Tipos tipoBase = TipoUtils.obterTipo(escopos, ctx.tipo().tipo_estendido().tipo_basico_ident());
            for (var ident : ctx.identificador()) {
                saida.append(CUtils.tipoC(tipoBase)).append(" *")
                        .append(gerarIdentificador(ident)).append(";\n");
            }
            return null;
        }

        // Tipo definido pelo usuário via TIPO IDENT (está na tabela como Categoria.TIPO)
        if (ctx.tipo().tipo_estendido() != null
                && ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT() != null) {
            String nomeDoTipo = ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT().getText();
            EntradaTabelaDeSimbolos entrada = escopos.buscar(nomeDoTipo);
            if (entrada != null && entrada.categoria == Categoria.TIPO) {
                for (var ident : ctx.identificador()) {
                    saida.append(nomeDoTipo).append(" ")
                            .append(gerarIdentificador(ident)).append(";\n");
                }
                return null;
            }
        }

        // Tipos básicos: INTEIRO, REAL, LOGICO, LITERAL
        for (var ident : ctx.identificador()) {
            String nome = gerarIdentificador(ident);
            if (tipo == Tipos.LITERAL) {
                saida.append("char ").append(nome).append("[80];\n");
            } else {
                saida.append(CUtils.tipoC(tipo)).append(" ").append(nome).append(";\n");
            }
        }
        return null;
    }

    /**
     * Gera os campos de um registro (usada tanto em struct anônima quanto em typedef struct).
     * Reutiliza visitVariavel internamente, mas emite direto na saída.
     */
    private void gerarCamposRegistro(LinguagemAlgoritmicaParser.VariavelContext ctx) {
        Tipos tipo = TipoUtils.obterTipo(escopos, ctx.tipo());
        for (var ident : ctx.identificador()) {
            String nome = gerarIdentificador(ident);
            if (tipo == Tipos.LITERAL) {
                saida.append("    char ").append(nome).append("[80];\n");
            } else if (tipo == Tipos.PONTEIRO) {
                Tipos tipoBase = TipoUtils.obterTipo(escopos, ctx.tipo().tipo_estendido().tipo_basico_ident());
                saida.append("    ").append(CUtils.tipoC(tipoBase)).append(" *").append(nome).append(";\n");
            } else {
                saida.append("    ").append(CUtils.tipoC(tipo)).append(" ").append(nome).append(";\n");
            }
        }
    }

    // =========================================================
    // Comandos
    // =========================================================

    /**
     * Gera atribuição em C.
     * PONTEIRO? identificador '<-' expressao
     * O PONTEIRO antes do identificador indica desreferência (*x <- expr).
     */
    @Override
    public Void visitCmdAtribuicao(LinguagemAlgoritmicaParser.CmdAtribuicaoContext ctx) {
        if (ctx.PONTEIRO() != null) saida.append("*");
        saida.append(gerarIdentificador(ctx.identificador()));
        saida.append(" = ").append(gerarExpressao(ctx.expressao())).append(";\n");
        return null;
    }

    /**
     * Gera scanf para cada identificador lido.
     *
     * Regras:
     *  - O tipo é buscado na tabela de símbolos pelo nome raiz do identificador.
     *  - LITERAL (char[]) não leva & porque array já é ponteiro.
     *  - O PONTEIRO opcional antes do identificador na gramática indica leitura
     *    via ponteiro: scanf("%d", x) em vez de scanf("%d", &x).
     */
    @Override
    public Void visitCmdLeia(LinguagemAlgoritmicaParser.CmdLeiaContext ctx) {
        // cmdLeia: LEIA '(' PONTEIRO? identificador (',' PONTEIRO? identificador)* ')'
        // A gramática alterna tokens PONTEIRO e identificador na mesma lista ctx.identificador().
        // Precisamos saber se cada identificador veio precedido de PONTEIRO.
        // Como ctx.PONTEIRO() retorna todos os tokens PONTEIRO, usamos token index para parear.

        var idents   = ctx.identificador();
        var ponteiros = ctx.PONTEIRO(); // lista de tokens '^' presentes

        for (int i = 0; i < idents.size(); i++) {
            var ident = idents.get(i);
            String nome = gerarIdentificador(ident);
            String raiz = ident.IDENT(0).getText();
            Tipos tipo  = resolverTipo(raiz);
            String fmt  = especificador(tipo);

            // Verifica se há um token PONTEIRO imediatamente antes deste identificador
            boolean temPonteiro = false;
            for (var pt : ponteiros) {
                // O token PONTEIRO vem antes do identificador no fluxo de tokens
                if (pt.getSymbol().getTokenIndex() < ident.getStart().getTokenIndex()) {
                    // Só conta se não houver outro identificador entre eles
                    boolean temIdentEntre = false;
                    for (int j = 0; j < i; j++) {
                        if (idents.get(j).getStop().getTokenIndex() > pt.getSymbol().getTokenIndex()) {
                            temIdentEntre = true;
                            break;
                        }
                    }
                    if (!temIdentEntre) temPonteiro = true;
                }
            }

            saida.append("scanf(\"").append(fmt).append("\", ");
            if (temPonteiro || tipo == Tipos.LITERAL) {
                // com ponteiro: variável já é ponteiro, não precisa de &
                // literal: char[] já é ponteiro
                saida.append(nome);
            } else {
                saida.append("&").append(nome);
            }
            saida.append(");\n");
        }
        return null;
    }

    /**
     * Gera printf para cada expressão escrita.
     * O formato é inferido pela estrutura da expressão + tabela de símbolos.
     */
    @Override
    public Void visitCmdEscreva(LinguagemAlgoritmicaParser.CmdEscrevaContext ctx) {
        for (var expr : ctx.expressao()) {
            String fmt  = inferirFormato(expr);
            String val  = gerarExpressao(expr);
            saida.append("printf(\"").append(fmt).append("\", ").append(val).append(");\n");
        }
        return null;
    }

    /**
     * Gera if/else em C.
     *
     * Problema: ctx.cmd() retorna TODOS os comandos (then + else) em uma lista única.
     * Solução: comparamos o token index de cada cmd com o token index do token SENAO.
     * Comandos com start index > senaoIndex pertencem ao bloco else.
     */
    @Override
    public Void visitCmdSe(LinguagemAlgoritmicaParser.CmdSeContext ctx) {
        saida.append("if (").append(gerarExpressao(ctx.expressao())).append(") {\n");

        int senaoIndex = ctx.SENAO() != null
                ? ctx.SENAO().getSymbol().getTokenIndex()
                : Integer.MAX_VALUE;

        boolean elseAberto = false;
        for (var cmd : ctx.cmd()) {
            if (cmd.getStart().getTokenIndex() > senaoIndex && !elseAberto) {
                saida.append("} else {\n");
                elseAberto = true;
            }
            visitCmd(cmd);
        }

        saida.append("}\n");
        return null;
    }

    /**
     * Gera switch/case em C a partir do cmdCaso.
     *
     * cmdCaso: CASO exp_aritmetica SEJA selecao (SENAO cmd*)? FIM_CASO
     *
     * O bloco SENAO vira default. Os cmds do senao são separados dos cmds
     * da selecao da mesma forma que no cmdSe: via token index.
     * Na prática, ctx.cmd() aqui contém APENAS os cmds do SENAO porque
     * os cmds da selecao são filhos de item_selecao, não de cmdCaso.
     */
    @Override
    public Void visitCmdCaso(LinguagemAlgoritmicaParser.CmdCasoContext ctx) {
        saida.append("switch (").append(gerarExpAritmetica(ctx.exp_aritmetica())).append(") {\n");

        visitSelecao(ctx.selecao());

        if (ctx.SENAO() != null) {
            saida.append("default:\n");
            for (var cmd : ctx.cmd()) visitCmd(cmd);
            saida.append("break;\n");
        }

        saida.append("}\n");
        return null;
    }

    @Override
    public Void visitSelecao(LinguagemAlgoritmicaParser.SelecaoContext ctx) {
        for (var item : ctx.item_selecao()) visitItem_selecao(item);
        return null;
    }

    /**
     * Gera os cases de um item_selecao.
     *
     * item_selecao: constantes ':' cmd*
     * constantes:   numero_intervalo (',' numero_intervalo)*
     * numero_intervalo: op_unario? NUM_INT ('..' op_unario? NUM_INT)?
     *
     * Intervalos são expandidos em cases individuais.
     * Sinais negativos (op_unario) são tratados individualmente para cada extremo.
     */
    @Override
    public Void visitItem_selecao(LinguagemAlgoritmicaParser.Item_selecaoContext ctx) {
        for (var ni : ctx.constantes().numero_intervalo()) {
            int valorInicio = parseNumeroIntervalo(ni, 0);

            if (ni.NUM_INT().size() == 1) {
                saida.append("case ").append(valorInicio).append(":\n");
            } else {
                int valorFim = parseNumeroIntervalo(ni, 1);
                for (int v = valorInicio; v <= valorFim; v++) {
                    saida.append("case ").append(v).append(":\n");
                }
            }
        }

        for (var cmd : ctx.cmd()) visitCmd(cmd);
        saida.append("break;\n");
        return null;
    }

    /**
     * Extrai o valor inteiro (com sinal) do n-ésimo NUM_INT de um numero_intervalo.
     *
     * numero_intervalo: op_unario? NUM_INT ('..' op_unario? NUM_INT)?
     *
     * A gramática coloca os op_unario como lista; o primeiro (índice 0) pertence
     * ao primeiro NUM_INT e o segundo (índice 1, se existir) ao segundo NUM_INT.
     * O mapeamento é feito por posição relativa dos tokens.
     */
    private int parseNumeroIntervalo(LinguagemAlgoritmicaParser.Numero_intervaloContext ni, int posNum) {
        int valor = Integer.parseInt(ni.NUM_INT(posNum).getText());

        // Verifica se há um op_unario correspondente a este NUM_INT
        // A gramática garante que op_unario[i] precede NUM_INT[i]
        if (ni.op_unario().size() > posNum && ni.op_unario(posNum) != null) {
            int opIndex  = ni.op_unario(posNum).getStart().getTokenIndex();
            int numIndex = ni.NUM_INT(posNum).getSymbol().getTokenIndex();
            // Confirma que o op_unario é imediatamente anterior ao número
            if (opIndex < numIndex) valor = -valor;
        }
        return valor;
    }

    /**
     * Gera for em C.
     * PARA ident '<-' inicio ATE fim FACA cmd* FIM_PARA
     * → for (ident = inicio; ident <= fim; ident++) { }
     */
    @Override
    public Void visitCmdPara(LinguagemAlgoritmicaParser.CmdParaContext ctx) {
        String ident  = ctx.IDENT().getText();
        String inicio = gerarExpAritmetica(ctx.exp_aritmetica(0));
        String fim    = gerarExpAritmetica(ctx.exp_aritmetica(1));

        saida.append("for (")
                .append(ident).append(" = ").append(inicio).append("; ")
                .append(ident).append(" <= ").append(fim).append("; ")
                .append(ident).append("++) {\n");

        for (var cmd : ctx.cmd()) visitCmd(cmd);
        saida.append("}\n");
        return null;
    }

    /**
     * Gera while em C.
     * ENQUANTO expressao FACA cmd* FIM_ENQUANTO → while (expr) { }
     */
    @Override
    public Void visitCmdEnquanto(LinguagemAlgoritmicaParser.CmdEnquantoContext ctx) {
        saida.append("while (").append(gerarExpressao(ctx.expressao())).append(") {\n");
        for (var cmd : ctx.cmd()) visitCmd(cmd);
        saida.append("}\n");
        return null;
    }

    /**
     * Gera do-while em C.
     * FACA cmd* ATE expressao
     *
     * Semântica: executa ATÉ a condição ser verdadeira (como repeat-until do Pascal).
     * Equivale a do { } while (!(cond)) em C.
     */
    @Override
    public Void visitCmdFaca(LinguagemAlgoritmicaParser.CmdFacaContext ctx) {
        saida.append("do {\n");
        for (var cmd : ctx.cmd()) visitCmd(cmd);
        saida.append("} while (!(").append(gerarExpressao(ctx.expressao())).append("));\n");
        return null;
    }

    /**
     * Gera chamada de procedimento/função como statement isolado.
     */
    @Override
    public Void visitCmdChamada(LinguagemAlgoritmicaParser.CmdChamadaContext ctx) {
        saida.append(ctx.IDENT().getText()).append("(");
        for (int i = 0; i < ctx.expressao().size(); i++) {
            if (i > 0) saida.append(", ");
            saida.append(gerarExpressao(ctx.expressao(i)));
        }
        saida.append(");\n");
        return null;
    }

    /**
     * Gera return em C.
     */
    @Override
    public Void visitCmdRetorne(LinguagemAlgoritmicaParser.CmdRetorneContext ctx) {
        saida.append("return ").append(gerarExpressao(ctx.expressao())).append(";\n");
        return null;
    }
}