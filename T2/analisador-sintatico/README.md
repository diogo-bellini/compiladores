# 📖 Descrição

Este projeto foi desenvolvido para a disciplina de Compiladores da UFSCar.

O objetivo é implementar um analisador sintático utilizando ANTLR, capaz de processar arquivos de entrada, identificar tokens e verificar de acordo com uma gramática definida.

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

Para executar o analisador léxico, utilize, na pasta raíz do projeto, o comando:

```bash
java -jar target/analisador-sintatico-1.0-SNAPSHOT-jar-with-dependencies.jar <arquivo_de_entrada.txt> <arquivo_de_saida.txt>
```
Os possíveis erros sintáticos vão ser salvos no arquivo de saída.

Exemplos de entradas estão em [Entradas](https://github.com/diogo-bellini/compiladores/tree/main/casos-de-teste/2.casos_teste_t2/entrada) e suas saídas esperadas em [Saídas](https://github.com/diogo-bellini/compiladores/tree/main/casos-de-teste/2.casos_teste_t2/saida).
