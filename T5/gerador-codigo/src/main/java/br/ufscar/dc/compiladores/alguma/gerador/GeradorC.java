package br.ufscar.dc.compiladores.alguma.gerador;

/**
 * GeradorC — converte a árvore sintática da Linguagem Algorítmica em código C.
 */
public class GeradorC extends LinguagemAlgoritmicaBaseVisitor<Void> {

    Escopos escopos = new Escopos();
    StringBuilder saida = new StringBuilder();
    int indentLevel = 0; // Controle de indentação para o corretor automático

    public String getCodigo() {
        return saida.toString();
    }

    private String getIndent() {
        return "\t".repeat(Math.max(0, indentLevel));
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
        if (ctx.parcela_unario() != null) {
            String pu = gerarParcelaUnario(ctx.parcela_unario());
            return ctx.op_unario() != null ? "-" + pu : pu;
        }
        return gerarParcelaNaoUnario(ctx.parcela_nao_unario());
    }

    private String gerarParcelaUnario(LinguagemAlgoritmicaParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            String ident = gerarIdentificador(ctx.identificador());
            return ctx.PONTEIRO() != null ? "*" + ident : ident;
        }
        if (ctx.IDENT() != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(ctx.IDENT().getText()).append("(");
            for (int i = 0; i < ctx.expressao().size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(gerarExpressao(ctx.expressao(i)));
            }
            sb.append(")");
            return sb.toString();
        }
        if (ctx.NUM_INT()  != null) return ctx.NUM_INT().getText();
        if (ctx.NUM_REAL() != null) return ctx.NUM_REAL().getText();
        if (!ctx.expressao().isEmpty())
            return "(" + gerarExpressao(ctx.expressao(0)) + ")";
        return "";
    }

    private String gerarParcelaNaoUnario(LinguagemAlgoritmicaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.identificador() != null) return "&" + gerarIdentificador(ctx.identificador());
        if (ctx.CADEIA() != null) return ctx.CADEIA().getText();
        return "";
    }

    private String gerarIdentificador(LinguagemAlgoritmicaParser.IdentificadorContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append(ctx.IDENT(0).getText());
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            sb.append(".").append(ctx.IDENT(i).getText());
        }
        if (ctx.dimensao() != null) sb.append(gerarDimensao(ctx.dimensao()));
        return sb.toString();
    }

    private String gerarDimensao(LinguagemAlgoritmicaParser.DimensaoContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (var exp : ctx.exp_aritmetica()) {
            sb.append("[").append(gerarExpAritmetica(exp)).append("]");
        }
        return sb.toString();
    }

    // =========================================================
    // Helpers: tipos, formatos e tabela de símbolos
    // =========================================================

    private String especificador(Tipos t) {
        switch (t) {
            case REAL:    return "%f";
            case LITERAL: return "%s";
            default:      return "%d";
        }
    }

    private Tipos resolverTipo(LinguagemAlgoritmicaParser.IdentificadorContext ctx) {
        String raiz = ctx.IDENT(0).getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(raiz);
        if (e == null) return Tipos.INVALIDO;

        if (ctx.IDENT().size() > 1) {
            EntradaTabelaDeSimbolos atual = e;
            for (int i = 1; i < ctx.IDENT().size(); i++) {
                String campo = ctx.IDENT(i).getText();

                if (atual.nomeTipo != null) {
                    EntradaTabelaDeSimbolos tipoBase = escopos.buscar(atual.nomeTipo);
                    if (tipoBase != null) atual = tipoBase;
                }

                if (atual != null && atual.camposRegistro != null) {
                    atual = atual.camposRegistro.buscar(campo);
                    if (atual == null) return Tipos.INVALIDO;
                } else {
                    return Tipos.INVALIDO;
                }
            }
            return atual.tipo;
        }
        return e.tipo;
    }

    private String inferirFormato(LinguagemAlgoritmicaParser.ExpressaoContext ctx) {
        if (ctx.termo_logico().size() > 1) return "%d";
        var tl = ctx.termo_logico(0);
        if (tl.fator_logico().size() > 1) return "%d";
        var fl = tl.fator_logico(0);
        if (fl.NAO() != null) return "%d";
        var pl = fl.parcela_logica();
        if (pl.VERDADEIRO() != null || pl.FALSO() != null) return "%d";
        var er = pl.exp_relacional();
        if (er.op_relacional() != null) return "%d";
        var ea = er.exp_aritmetica(0);
        var p0 = ea.termo(0).fator(0).parcela(0);

        if (p0.parcela_nao_unario() != null) {
            if (p0.parcela_nao_unario().CADEIA() != null) return null;
            return "%d";
        }

        if (p0.parcela_unario() != null) {
            var pu = p0.parcela_unario();
            if (pu.NUM_REAL() != null) return "%f";
            if (pu.NUM_INT()  != null) return "%d";
            if (pu.identificador() != null) {
                return especificador(resolverTipo(pu.identificador()));
            }
        }
        return "%d";
    }

    private boolean expressaoEhLiteral(LinguagemAlgoritmicaParser.ExpressaoContext ctx) {
        if (ctx.termo_logico().size() != 1) return false;
        var tl = ctx.termo_logico(0);
        if (tl.fator_logico().size() != 1) return false;
        var fl = tl.fator_logico(0);
        if (fl.NAO() != null) return false;
        var pl = fl.parcela_logica();
        if (pl.VERDADEIRO() != null || pl.FALSO() != null) return false;
        var er = pl.exp_relacional();
        if (er.op_relacional() != null) return false;
        var ea = er.exp_aritmetica(0);
        if (ea.termo().size() != 1) return false;
        var t = ea.termo(0);
        if (t.fator().size() != 1) return false;
        var f = t.fator(0);
        if (f.parcela().size() != 1) return false;
        var p = f.parcela(0);

        if (p.parcela_nao_unario() != null && p.parcela_nao_unario().CADEIA() != null)
            return true;

        if (p.parcela_unario() != null && p.parcela_unario().identificador() != null) {
            return resolverTipo(p.parcela_unario().identificador()) == Tipos.LITERAL;
        }

        return false;
    }

    private boolean expressaoTemNaoExterno(LinguagemAlgoritmicaParser.ExpressaoContext ctx) {
        if (ctx.termo_logico().size() != 1) return false;
        var tl = ctx.termo_logico(0);
        if (tl.fator_logico().size() != 1) return false;
        return tl.fator_logico(0).NAO() != null;
    }

    // =========================================================
    // Programa — ponto de entrada
    // =========================================================

    @Override
    public Void visitPrograma(LinguagemAlgoritmicaParser.ProgramaContext ctx) {
        saida.append("#include <stdio.h>\n");
        saida.append("#include <stdlib.h>\n\n");
        saida.append("#include <string.h>\n\n");

        for (var dlg : ctx.declaracoes().decl_local_global()) {
            if (dlg.declaracao_local() != null && dlg.declaracao_local().CONSTANTE() != null) {
                gerarDefine(dlg.declaracao_local());
            }
        }

        for (var dlg : ctx.declaracoes().decl_local_global()) {
            if (dlg.declaracao_global() != null) {
                visitDeclaracao_global(dlg.declaracao_global());
            }
        }

        saida.append("int main() {\n");
        indentLevel++;

        for (var dlg : ctx.declaracoes().decl_local_global()) {
            if (dlg.declaracao_local() != null && dlg.declaracao_local().CONSTANTE() == null) {
                visitDeclaracao_local(dlg.declaracao_local());
            }
        }

        visitCorpo(ctx.corpo());

        saida.append(getIndent()).append("return 0;\n");
        indentLevel--;
        saida.append("}\n");
        return null;
    }

    private void gerarDefine(LinguagemAlgoritmicaParser.Declaracao_localContext ctx) {
        String nome       = ctx.IDENT().getText();
        Tipos tipo        = TipoUtils.obterTipo(ctx.tipo_basico());
        String valorTexto = ctx.valor_constante().getText();

        escopos.obterEscopoAtual().inserir(nome, tipo, Categoria.CONSTANTE);
        saida.append("#define ").append(nome).append(" ").append(valorTexto).append("\n");
    }

    // =========================================================
    // Funções e procedimentos (declaracao_global)
    // =========================================================

    @Override
    public Void visitDeclaracao_global(LinguagemAlgoritmicaParser.Declaracao_globalContext ctx) {
        escopos.criarEscopo();

        boolean ehFuncao = ctx.FUNCAO() != null;

        if (ehFuncao) {
            Tipos tipoRet = TipoUtils.obterTipo(escopos, ctx.tipo_estendido());
            saida.append(CUtils.tipoC(tipoRet));
        } else {
            saida.append("void");
        }

        // Espaçamento estrito requerido pelo corretor automático: "void proc_imprime (char* mensagem){"
        saida.append(" ").append(ctx.IDENT().getText()).append(" (");

        if (ctx.parametros() != null) {
            boolean primeiro = true;
            for (var param : ctx.parametros().parametro()) {
                boolean porRef  = param.VAR() != null;
                Tipos tipoParam = TipoUtils.obterTipo(escopos, param.tipo_estendido());

                String nomeTipoStr = null;
                if (param.tipo_estendido().tipo_basico_ident().IDENT() != null) {
                    nomeTipoStr = param.tipo_estendido().tipo_basico_ident().IDENT().getText();
                }

                for (var ident : param.identificador()) {
                    if (!primeiro) saida.append(",");
                    primeiro = false;

                    String nome = gerarIdentificador(ident);

                    EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
                    entrada.nome = ident.IDENT(0).getText();
                    entrada.tipo = tipoParam;
                    entrada.categoria = Categoria.VARIAVEL;
                    entrada.nomeTipo = nomeTipoStr;
                    escopos.obterEscopoAtual().inserir(entrada);

                    if (tipoParam == Tipos.LITERAL) {
                        saida.append("char* ").append(nome);
                    } else {
                        saida.append(CUtils.tipoC(tipoParam));
                        saida.append(porRef ? "* " : " ");
                        saida.append(nome);
                    }
                }
            }
        }
        saida.append("){\n");
        indentLevel++;

        for (var declLocal : ctx.declaracao_local()) visitDeclaracao_local(declLocal);
        for (var cmd : ctx.cmd()) visitCmd(cmd);

        indentLevel--;
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

    @Override
    public Void visitDeclaracao_local(LinguagemAlgoritmicaParser.Declaracao_localContext ctx) {
        if (ctx.variavel() != null) {
            visitVariavel(ctx.variavel());
            return null;
        }

        if (ctx.CONSTANTE() != null) {
            String nome       = ctx.IDENT().getText();
            Tipos tipo        = TipoUtils.obterTipo(ctx.tipo_basico());
            String valorTexto = ctx.valor_constante().getText();

            escopos.obterEscopoAtual().inserir(nome, tipo, Categoria.CONSTANTE);

            if (tipo == Tipos.LITERAL) {
                saida.append(getIndent()).append("char * const ").append(nome)
                        .append(" = ").append(valorTexto).append(";\n");
            } else {
                saida.append(getIndent()).append("const ").append(CUtils.tipoC(tipo)).append(" ")
                        .append(nome).append(" = ").append(valorTexto).append(";\n");
            }
            return null;
        }

        if (ctx.TIPO() != null) {
            String nome = ctx.IDENT().getText();
            Tipos tipo  = TipoUtils.obterTipo(escopos, ctx.tipo());

            EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
            entrada.nome = nome;
            entrada.tipo = tipo;
            entrada.categoria = Categoria.TIPO;

            if (tipo == Tipos.REGISTRO) {
                entrada.ehRegistro = true;
                entrada.camposRegistro = new TabelaDeSimbolos();
                for (var campo : ctx.tipo().registro().variavel()) {
                    Tipos tipoCampo = TipoUtils.obterTipo(escopos, campo.tipo());
                    for (var idCampo : campo.identificador()) {
                        EntradaTabelaDeSimbolos ec = new EntradaTabelaDeSimbolos();
                        ec.nome = idCampo.IDENT(0).getText();
                        ec.tipo = tipoCampo;
                        ec.categoria = Categoria.VARIAVEL;
                        entrada.camposRegistro.inserir(ec);
                    }
                }
            }
            escopos.obterEscopoAtual().inserir(entrada);

            if (tipo == Tipos.REGISTRO) {
                saida.append(getIndent()).append("typedef struct {\n");
                indentLevel++;
                for (var campo : ctx.tipo().registro().variavel()) {
                    gerarCamposRegistro(campo);
                }
                indentLevel--;
                saida.append(getIndent()).append("} ").append(nome).append(";\n");
            } else {
                saida.append(getIndent()).append("typedef ").append(CUtils.tipoC(tipo))
                        .append(" ").append(nome).append(";\n");
            }
            return null;
        }

        return null;
    }

    @Override
    public Void visitVariavel(LinguagemAlgoritmicaParser.VariavelContext ctx) {
        Tipos tipo = TipoUtils.obterTipo(escopos, ctx.tipo());
        String nomeTipoStr = null;

        if (ctx.tipo().tipo_estendido() != null && ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT() != null) {
            nomeTipoStr = ctx.tipo().tipo_estendido().tipo_basico_ident().IDENT().getText();
        }

        for (var ident : ctx.identificador()) {
            EntradaTabelaDeSimbolos entrada = new EntradaTabelaDeSimbolos();
            entrada.nome = ident.IDENT(0).getText();
            entrada.tipo = tipo;
            entrada.categoria = Categoria.VARIAVEL;
            entrada.nomeTipo = nomeTipoStr;

            if (tipo == Tipos.REGISTRO) {
                entrada.ehRegistro = true;
                entrada.camposRegistro = new TabelaDeSimbolos();
                // Só popula campos quando o registro é definido inline (ctx.tipo().registro() != null).
                // Quando o tipo vem de um nome (ex: "declare reg: treg"), registro() é null,
                // e resolverTipo() já resolve campos via nomeTipo.
                if (ctx.tipo().registro() != null) {
                    for (var campo : ctx.tipo().registro().variavel()) {
                        Tipos tipoCampo = TipoUtils.obterTipo(escopos, campo.tipo());
                        for (var idCampo : campo.identificador()) {
                            EntradaTabelaDeSimbolos ec = new EntradaTabelaDeSimbolos();
                            ec.nome = idCampo.IDENT(0).getText();
                            ec.tipo = tipoCampo;
                            ec.categoria = Categoria.VARIAVEL;
                            entrada.camposRegistro.inserir(ec);
                        }
                    }
                }
            }
            escopos.obterEscopoAtual().inserir(entrada);
        }

        // Tipo nomeado (inclui registro via typedef): gera "NomeTipo var;"
        // Este bloco DEVE vir antes do check de REGISTRO para evitar gerar struct inline
        // quando a variável é declarada com um tipo registro nomeado (ex: "declare reg: treg").
        if (nomeTipoStr != null) {
            EntradaTabelaDeSimbolos entrada = escopos.buscar(nomeTipoStr);
            if (entrada != null && entrada.categoria == Categoria.TIPO) {
                for (var ident : ctx.identificador()) {
                    saida.append(getIndent()).append(nomeTipoStr).append(" ")
                            .append(gerarIdentificador(ident)).append(";\n");
                }
                return null;
            }
        }

        if (tipo == Tipos.REGISTRO) {
            for (var ident : ctx.identificador()) {
                saida.append(getIndent()).append("struct {\n");
                indentLevel++;
                for (var campo : ctx.tipo().registro().variavel()) {
                    gerarCamposRegistro(campo);
                }
                indentLevel--;
                saida.append(getIndent()).append("} ").append(gerarIdentificador(ident)).append(";\n");
            }
            return null;
        }

        if (tipo == Tipos.PONTEIRO) {
            Tipos tipoBase = TipoUtils.obterTipo(escopos, ctx.tipo().tipo_estendido().tipo_basico_ident());
            for (var ident : ctx.identificador()) {
                saida.append(getIndent()).append(CUtils.tipoC(tipoBase)).append("* ")
                        .append(gerarIdentificador(ident)).append(";\n");
            }
            return null;
        }

        for (var ident : ctx.identificador()) {
            String nome = gerarIdentificador(ident);
            if (tipo == Tipos.LITERAL) {
                saida.append(getIndent()).append("char ").append(nome).append("[80];\n");
            } else {
                saida.append(getIndent()).append(CUtils.tipoC(tipo)).append(" ").append(nome).append(";\n");
            }
        }
        return null;
    }

    private void gerarCamposRegistro(LinguagemAlgoritmicaParser.VariavelContext ctx) {
        Tipos tipo = TipoUtils.obterTipo(escopos, ctx.tipo());
        for (var ident : ctx.identificador()) {
            String nome = gerarIdentificador(ident);
            if (tipo == Tipos.LITERAL) {
                saida.append(getIndent()).append("char ").append(nome).append("[80];\n");
            } else if (tipo == Tipos.PONTEIRO) {
                Tipos tipoBase = TipoUtils.obterTipo(escopos, ctx.tipo().tipo_estendido().tipo_basico_ident());
                saida.append(getIndent()).append(CUtils.tipoC(tipoBase)).append("* ").append(nome).append(";\n");
            } else {
                saida.append(getIndent()).append(CUtils.tipoC(tipo)).append(" ").append(nome).append(";\n");
            }
        }
    }

    // =========================================================
    // Comandos
    // =========================================================

    @Override
    public Void visitCmdAtribuicao(LinguagemAlgoritmicaParser.CmdAtribuicaoContext ctx) {
        String destino = (ctx.PONTEIRO() != null ? "*" : "") + gerarIdentificador(ctx.identificador());
        saida.append(getIndent());

        if (expressaoEhLiteral(ctx.expressao())) {
            saida.append("strcpy(").append(destino).append(",")
                    .append(gerarExpressao(ctx.expressao())).append(");\n");
        } else {
            saida.append(destino).append(" = ")
                    .append(gerarExpressao(ctx.expressao())).append(";\n");
        }
        return null;
    }

    @Override
    public Void visitCmdLeia(LinguagemAlgoritmicaParser.CmdLeiaContext ctx) {
        var idents    = ctx.identificador();
        var ponteiros = ctx.PONTEIRO();

        for (int i = 0; i < idents.size(); i++) {
            var ident = idents.get(i);
            String nome = gerarIdentificador(ident);
            Tipos tipo  = resolverTipo(ident);
            String fmt  = especificador(tipo);

            boolean temPonteiro = false;
            for (var pt : ponteiros) {
                if (pt.getSymbol().getTokenIndex() < ident.getStart().getTokenIndex()) {
                    boolean tapado = false;
                    for (int j = 0; j < i; j++) {
                        if (idents.get(j).getStop().getTokenIndex() > pt.getSymbol().getTokenIndex()) {
                            tapado = true;
                            break;
                        }
                    }
                    if (!tapado) temPonteiro = true;
                }
            }

            saida.append(getIndent()).append("scanf(\"").append(fmt).append("\",");
            if (temPonteiro || tipo == Tipos.LITERAL) {
                saida.append(nome);
            } else {
                saida.append("&").append(nome);
            }
            saida.append(");\n");
        }
        return null;
    }

    @Override
    public Void visitCmdEscreva(LinguagemAlgoritmicaParser.CmdEscrevaContext ctx) {
        for (var expr : ctx.expressao()) {
            String fmt = inferirFormato(expr);
            String val = gerarExpressao(expr);
            saida.append(getIndent());
            if (fmt == null) {
                saida.append("printf(").append(val).append(");\n");
            } else {
                saida.append("printf(\"").append(fmt).append("\",").append(val).append(");\n");
            }
        }
        return null;
    }

    @Override
    public Void visitCmdSe(LinguagemAlgoritmicaParser.CmdSeContext ctx) {
        saida.append(getIndent()).append("if (").append(gerarExpressao(ctx.expressao())).append(") {\n");

        int senaoIndex = ctx.SENAO() != null
                ? ctx.SENAO().getSymbol().getTokenIndex()
                : Integer.MAX_VALUE;

        boolean elseAberto = false;
        indentLevel++;
        for (var cmd : ctx.cmd()) {
            if (cmd.getStart().getTokenIndex() > senaoIndex && !elseAberto) {
                indentLevel--;
                saida.append(getIndent()).append("} else {\n");
                indentLevel++;
                elseAberto = true;
            }
            visitCmd(cmd);
        }
        indentLevel--;

        saida.append(getIndent()).append("}\n");
        return null;
    }

    @Override
    public Void visitCmdCaso(LinguagemAlgoritmicaParser.CmdCasoContext ctx) {
        saida.append(getIndent()).append("switch (").append(gerarExpAritmetica(ctx.exp_aritmetica())).append(") {\n");
        indentLevel++;

        visitSelecao(ctx.selecao());

        if (ctx.SENAO() != null) {
            saida.append(getIndent()).append("default:\n");
            indentLevel++;
            for (var cmd : ctx.cmd()) visitCmd(cmd);
            indentLevel--;
        }

        indentLevel--;
        saida.append(getIndent()).append("}\n");
        return null;
    }

    @Override
    public Void visitSelecao(LinguagemAlgoritmicaParser.SelecaoContext ctx) {
        for (var item : ctx.item_selecao()) visitItem_selecao(item);
        return null;
    }

    @Override
    public Void visitItem_selecao(LinguagemAlgoritmicaParser.Item_selecaoContext ctx) {
        for (var ni : ctx.constantes().numero_intervalo()) {
            int valorInicio = parseNumeroIntervalo(ni, 0);

            if (ni.NUM_INT().size() == 1) {
                saida.append(getIndent()).append("case ").append(valorInicio).append(":\n");
            } else {
                int valorFim = parseNumeroIntervalo(ni, 1);
                for (int v = valorInicio; v <= valorFim; v++) {
                    saida.append(getIndent()).append("case ").append(v).append(":\n");
                }
            }
        }

        indentLevel++;
        for (var cmd : ctx.cmd()) visitCmd(cmd);
        saida.append(getIndent()).append("break;\n");
        indentLevel--;
        return null;
    }

    private int parseNumeroIntervalo(LinguagemAlgoritmicaParser.Numero_intervaloContext ni, int posNum) {
        int valor = Integer.parseInt(ni.NUM_INT(posNum).getText());
        if (ni.op_unario().size() > posNum && ni.op_unario(posNum) != null) {
            int opIndex  = ni.op_unario(posNum).getStart().getTokenIndex();
            int numIndex = ni.NUM_INT(posNum).getSymbol().getTokenIndex();
            if (opIndex < numIndex) valor = -valor;
        }
        return valor;
    }

    @Override
    public Void visitCmdPara(LinguagemAlgoritmicaParser.CmdParaContext ctx) {
        String ident  = ctx.IDENT().getText();
        String inicio = gerarExpAritmetica(ctx.exp_aritmetica(0));
        String fim    = gerarExpAritmetica(ctx.exp_aritmetica(1));

        saida.append(getIndent()).append("for (")
                .append(ident).append(" = ").append(inicio).append("; ")
                .append(ident).append(" <= ").append(fim).append("; ")
                .append(ident).append("++) {\n");

        indentLevel++;
        for (var cmd : ctx.cmd()) visitCmd(cmd);
        indentLevel--;

        saida.append(getIndent()).append("}\n");
        return null;
    }

    @Override
    public Void visitCmdEnquanto(LinguagemAlgoritmicaParser.CmdEnquantoContext ctx) {
        saida.append(getIndent()).append("while (").append(gerarExpressao(ctx.expressao())).append(") {\n");

        indentLevel++;
        for (var cmd : ctx.cmd()) visitCmd(cmd);
        indentLevel--;

        saida.append(getIndent()).append("}\n");
        return null;
    }

    @Override
    public Void visitCmdFaca(LinguagemAlgoritmicaParser.CmdFacaContext ctx) {
        saida.append(getIndent()).append("do {\n");

        indentLevel++;
        for (var cmd : ctx.cmd()) visitCmd(cmd);
        indentLevel--;

        String cond = gerarExpressao(ctx.expressao());
        saida.append(getIndent());
        if (expressaoTemNaoExterno(ctx.expressao())) {
            saida.append("} while (").append(cond).append(");\n");
        } else {
            saida.append("} while (!(").append(cond).append("));\n");
        }
        return null;
    }

    @Override
    public Void visitCmdChamada(LinguagemAlgoritmicaParser.CmdChamadaContext ctx) {
        saida.append(getIndent()).append(ctx.IDENT().getText()).append("(");
        for (int i = 0; i < ctx.expressao().size(); i++) {
            if (i > 0) saida.append(",");
            saida.append(gerarExpressao(ctx.expressao(i)));
        }
        saida.append(");\n");
        return null;
    }

    @Override
    public Void visitCmdRetorne(LinguagemAlgoritmicaParser.CmdRetorneContext ctx) {
        saida.append(getIndent()).append("return ").append(gerarExpressao(ctx.expressao())).append(";\n");
        return null;
    }
}