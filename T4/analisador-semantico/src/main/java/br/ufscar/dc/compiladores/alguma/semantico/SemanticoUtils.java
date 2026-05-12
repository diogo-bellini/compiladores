package br.ufscar.dc.compiladores.alguma.semantico;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

// Classe utilitária para verificação de tipos e registro de erros semânticos
public class SemanticoUtils {
    public static List<String> errosSemanticos = new ArrayList<String>();

    // Adiciona erro semântico formatado com linha e descrição
    public static void adicionarErro(Token token, TipoErro tipoErro) {
        adicionarErro(token, tipoErro, token.getText());
    }
    public static void adicionarErro(Token token, TipoErro tipoErro, String nome) {
        int linha = token.getLine();
        String erroMessage = String.format("Linha %d: ", linha);
        switch (tipoErro) {
            case IDENTIFICADOR_NAO_DECLARADO:
                erroMessage += String.format("identificador %s nao declarado", nome);
                break;
            case ATRIBUICAO_NAO_COMPATIVEL:
                erroMessage += String.format("atribuicao nao compativel para %s", nome);
                break;
            case TIPO_NAO_DECLARADO:
                erroMessage += String.format("tipo %s nao declarado", nome);
                break;
            case IDENTIFICADOR_REPETIDO:
                erroMessage += String.format("identificador %s ja declarado anteriormente", nome);
                break;
            case INCOMPATIBILIDADE_PARAMETROS:
                erroMessage += String.format("incompatibilidade de parametros na chamada de %s", nome);
                break;
            case RETORNE_FORA_FUNCAO:
                erroMessage += "comando retorne nao permitido nesse escopo";
                break;
        }
        errosSemanticos.add(erroMessage);
    }

    // Verifica se ambos os tipos são numéricos
    public static boolean tiposNumericos(Tipos t1, Tipos t2) {
        return (t1 == Tipos.INTEIRO || t1 == Tipos.REAL) && (t2 == Tipos.INTEIRO || t2 == Tipos.REAL);
    }

    // Verifica compatibilidade geral de tipos
    public static boolean tiposCompativeis(Tipos t1, Tipos t2) {
        if (t1 == Tipos.INVALIDO || t2 == Tipos.INVALIDO) return true;
        if (tiposNumericos(t1, t2)) return true;
        return t1 == t2;
    }

    public static boolean tiposCompativeisEstritos(Tipos esperado, Tipos recebido) {
        return esperado == recebido;
    }

    // Promoção numérica (inteiro -> real)
    public static Tipos promoverNumerico(Tipos t1, Tipos t2) {
        if (!tiposNumericos(t1, t2)) {
            return Tipos.INVALIDO;
        }
        if (t1 == Tipos.REAL || t2 == Tipos.REAL) {
            return Tipos.REAL;
        }
        return Tipos.INTEIRO;
    }

    // Combina expressões lógicas (AND/OR)
    public static Tipos combinarLogico(Tipos ret, Tipos t) {
        if (ret == null) return t;
        if (ret != Tipos.LOGICO || t != Tipos.LOGICO) {
            return Tipos.INVALIDO;
        }
        return Tipos.LOGICO;
    }

    // Verificação principal de tipo de expressões
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.ExpressaoContext ctx) {
        Tipos ret = null;
        for (var tlog : ctx.termo_logico()) {
            Tipos t = verificarTipo(escopos, tlog);
            ret = combinarLogico(ret, t);
            if (ret == Tipos.INVALIDO) {
                return ret;
            }
        }
        return ret;
    }

    // Avalia termo lógico (AND implícito entre fatores)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.Termo_logicoContext ctx) {
        Tipos ret = null;

        for (var f : ctx.fator_logico()) {
            Tipos t = verificarTipo(escopos, f);

            // Combina resultados lógicos (todos devem ser LOGICO)
            ret = combinarLogico(ret, t);

            if (ret == Tipos.INVALIDO) {
                return ret; // interrompe em caso de erro
            }
        }
        return ret;
    }

    // Avalia fator lógico (possível uso de NOT)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.Fator_logicoContext ctx) {
        Tipos t = verificarTipo(escopos, ctx.parcela_logica());

        // NOT só é válido para expressões lógicas
        if (ctx.NAO() != null) {
            if (t != Tipos.LOGICO) {
                return Tipos.INVALIDO;
            }
        }
        return t;
    }

    // Parcela lógica: booleano literal ou expressão relacional
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.Parcela_logicaContext ctx) {
        if (ctx.VERDADEIRO() != null || ctx.FALSO() != null) {
            return Tipos.LOGICO;
        }
        return verificarTipo(escopos, ctx.exp_relacional());
    }

    // Avalia expressões relacionais (>, <, =, etc.)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.Exp_relacionalContext ctx) {
        Tipos t1 = verificarTipo(escopos, ctx.exp_aritmetica(0));

        // Caso sem operador relacional (apenas expressão aritmética)
        if (ctx.op_relacional() == null) {
            return t1;
        }

        Tipos t2 = verificarTipo(escopos, ctx.exp_aritmetica(1));
        String op = ctx.op_relacional().getText();

        // Operadores relacionais exigem tipos numéricos
        if (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<=")) {
            if (!tiposNumericos(t1, t2)) {
                return Tipos.INVALIDO;
            }
            return Tipos.LOGICO;
        }

        // Igualdade permite tipos compatíveis
        if (op.equals("=") || op.equals("<>")) {
            if (!tiposCompativeis(t1, t2)) {
                return Tipos.INVALIDO;
            }
            return Tipos.LOGICO;
        }
        return Tipos.INVALIDO;
    }

    // Avalia expressão aritmética (+ e -)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.Exp_aritmeticaContext ctx) {
        Tipos ret = null;

        for (var termo : ctx.termo()) {
            Tipos t = verificarTipo(escopos, termo);

            if (ret == null) {
                ret = t; // primeiro termo define o tipo inicial
            } else {
                // concatenação de literais (caso permitido na linguagem)
                if (ret == Tipos.LITERAL && t == Tipos.LITERAL) {
                    continue;
                }
                // promoção numérica (int -> real)
                else if (tiposNumericos(ret, t)) {
                    ret = promoverNumerico(ret, t);
                }
                else {
                    return Tipos.INVALIDO;
                }
            }
        }
        return ret;
    }

    // Avalia termo (* e /)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.TermoContext ctx) {
        Tipos ret = null;

        for (var f : ctx.fator()) {
            Tipos t = verificarTipo(escopos, f);

            if (ret == null) {
                ret = t;
            } else {
                // multiplicação/divisão só entre numéricos
                if (!tiposNumericos(ret, t)) {
                    return Tipos.INVALIDO;
                }
                ret = promoverNumerico(ret, t);
            }
        }
        return ret;
    }

    // Avalia fator (% ou outros agrupamentos)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.FatorContext ctx) {
        Tipos ret = null;

        for (var p : ctx.parcela()) {
            Tipos t = verificarTipo(escopos, p);

            if (ret == null) {
                ret = t;
            } else {
                if (!tiposNumericos(ret, t)) {
                    return Tipos.INVALIDO;
                }
                ret = promoverNumerico(ret, t);
            }
        }
        return ret;
    }

    // Decide entre parcela unária ou não unária
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.ParcelaContext ctx) {
        if (ctx.parcela_unario() != null) {
            return verificarTipo(escopos, ctx.parcela_unario());
        } else {
            return verificarTipo(escopos, ctx.parcela_nao_unario());
        }
    }

    // Avalia parcela unária (variáveis, números, expressões)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.Parcela_unarioContext ctx) {
        if (ctx.identificador() != null) {
            LinguagemAlgoritmicaParser.IdentificadorContext identCtx = ctx.identificador();
            String nomeBase = identCtx.IDENT(0).getText();
            EntradaTabelaDeSimbolos e = escopos.buscar(nomeBase);

            // variável não declarada
            if (e == null) {
                SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, identCtx.getText());
                return Tipos.INVALIDO;
            }

            EntradaTabelaDeSimbolos atual = e;

            for (int i = 1; i < identCtx.IDENT().size(); i++) {
                String campo = identCtx.IDENT(i).getText();

                // não é registro mas tentou acessar campo
                if (!atual.ehRegistro || atual.camposRegistro == null) {
                    SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, identCtx.getText());
                    return Tipos.INVALIDO;
                }

                EntradaTabelaDeSimbolos prox = atual.camposRegistro.buscar(campo);

                // campo não existe (CASO CRÍTICO: Preco vs preco)
                if (prox == null) {
                    SemanticoUtils.adicionarErro(identCtx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, identCtx.getText());
                    return Tipos.INVALIDO;
                }

                atual = prox;
            }

            return atual.tipo;
        }

        if (ctx.IDENT() != null) {
            String nome = ctx.IDENT().getText();
            EntradaTabelaDeSimbolos e = escopos.buscar(nome);

            if (e == null) {
                SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.IDENTIFICADOR_NAO_DECLARADO, nome);
                return Tipos.INVALIDO;
            }

            if (ctx.expressao() != null && !ctx.expressao().isEmpty()) {
                List<Tipos> tiposPassados = new ArrayList<>();

                for (var expr : ctx.expressao()) {
                    tiposPassados.add(verificarTipo(escopos, expr));
                }

                List<Tipos> tiposEsperados = e.tiposParametros;

                if (tiposEsperados.size() != tiposPassados.size()) {
                    SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.INCOMPATIBILIDADE_PARAMETROS, nome);
                } else {
                    for (int i = 0; i < tiposEsperados.size(); i++) {
                        if (!tiposCompativeisEstritos(tiposEsperados.get(i), tiposPassados.get(i))) {
                            SemanticoUtils.adicionarErro(ctx.IDENT().getSymbol(), TipoErro.INCOMPATIBILIDADE_PARAMETROS, nome);
                            break;
                        }
                    }
                }
            }
            return e.tipo;
        }

        if (ctx.NUM_INT() != null) {
            return Tipos.INTEIRO;
        }

        if (ctx.NUM_REAL() != null) {
            return Tipos.REAL;
        }

        if (ctx.expressao() != null) {
            return verificarTipo(escopos, ctx.expressao(0));
        }

        return Tipos.INVALIDO;
    }

    // Avalia parcela não unária (strings e endereços)
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.Parcela_nao_unarioContext ctx) {
        if (ctx.CADEIA() != null) {
            return Tipos.LITERAL;
        }

        // Identificador usado como endereço
        if (ctx.identificador() != null) {
            String nome = ctx.identificador().getText();
            EntradaTabelaDeSimbolos e = escopos.buscar(nome);

            if (e == null) {
                SemanticoUtils.adicionarErro(ctx.identificador().start, TipoErro.IDENTIFICADOR_NAO_DECLARADO);
                return Tipos.INVALIDO;
            }
            return Tipos.ENDERECO;
        }
        return Tipos.INVALIDO;
    }
    
    public static Tipos verificarTipo(Escopos escopos, LinguagemAlgoritmicaParser.IdentificadorContext ctx) {
        String nomeBase = ctx.IDENT(0).getText();
        EntradaTabelaDeSimbolos e = escopos.buscar(nomeBase);

        if (e == null) {
            adicionarErro(ctx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, ctx.getText());
            return Tipos.INVALIDO;
        }

        EntradaTabelaDeSimbolos atual = e;
        
        // Percorre os campos do registro
        for (int i = 1; i < ctx.IDENT().size(); i++) {
            String nomeCampo = ctx.IDENT(i).getText();
            
            if (!atual.ehRegistro || atual.camposRegistro == null) {
                adicionarErro(ctx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, ctx.getText());
                return Tipos.INVALIDO;
            }

            EntradaTabelaDeSimbolos campo = atual.camposRegistro.buscar(nomeCampo);
            
            if (campo == null) {
                adicionarErro(ctx.start, TipoErro.IDENTIFICADOR_NAO_DECLARADO, ctx.getText());
                return Tipos.INVALIDO;
            }
            atual = campo;
        }
        return atual.tipo;
    }
}