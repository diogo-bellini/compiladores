# 📖 Descrição

Este projeto foi desenvolvido para a disciplina de Compiladores da UFSCar.

O objetivo é implementar aprimorar o analisador semantico T3 detectando 5 novos tipos de erros:
- Identificador (variável, constante, procedimento, função, tipo) já declarado anteriormente no escopo, mas agora envolvendo também ponteiros, registros, funções
  - O mesmo identificador não pode ser usado novamente no mesmo escopo mesmo que para categorias diferentes

- Identificador (variável, constante, procedimento, função) não declarado, mas agora envolvendo também ponteiros, registros, funções

- Incompatibilidade entre argumentos e parâmetros formais (número, ordem e tipo) na chamada de um procedimento ou uma função
  - A quantidade e tipo dos argumentos deve ser exata
    - endereço → ponteiro
    - real → real
    - inteiro → inteiro
    - literal → literal
    - logico → logico
    - registro → registro (com mesmo nome de tipo)

- Atribuição não compatível com o tipo declarado, agora envolvendo ponteiros e registros
  - Atribuições possíveis
    - ponteiro ← endereço
    - (real | inteiro) ← (real | inteiro)
    - literal ← literal
    - logico ← logico
    - registro ← registro (com mesmo nome de tipo)

  - As mesmas restrições são válidas para expressões, por exemplo, ao tentar combinar um literal com um logico (como em literal + logico) deve dar tipo_indefinido e inviabilizar a atribuição

- Uso do comando 'retorne' em um escopo não permitido


---

# ⚙️ Pré-requisitos

Antes de executar o projeto, é necessário ter instalado:

- **Java JDK 17** (ou compatível)
- **Apache Maven 3.9+**

Para verificar as versões instaladas:

```bash
java -version
mvn -version
```
# 🛠️ Compilação

Para compilar o projeto, execute o seguinte comando na raiz do projeto:

```bash
mvn clean package
```
Após a compilação, será gerado um arquivo `.jar` na pasta `target/`.

---

# ▶️ Execução

Para executar o analisador semântico, utilize, na pasta raíz do projeto, o comando:

```bash
java -jar target/analisador-semantico-1.0-SNAPSHOT-jar-with-dependencies.jar <arquivo_de_entrada.txt> <arquivo_de_saida.txt>
```
Os possíveis erros semanticos vão ser salvos no arquivo de saída.

Exemplos de entradas estão em [Entradas](https://github.com/diogo-bellini/compiladores/tree/main/casos-de-teste/4.casos_teste_t4/entrada) e suas saídas esperadas em [Saídas](https://github.com/diogo-bellini/compiladores/tree/main/casos-de-teste/4.casos_teste_t4/saida).
