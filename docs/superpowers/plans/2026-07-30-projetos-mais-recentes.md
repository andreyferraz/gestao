# Projetos mais recentes primeiro — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer `GET /api/projetos` entregar projetos por criação decrescente para que a paginação do consumidor coloque novos projetos na primeira página.

**Architecture:** A criação persistirá um instante UTC em `projeto.created_at`; a migração SQLite preencherá registros legados e o schema de teste também conterá a coluna. Uma consulta explícita no repositório aplicará `ORDER BY created_at DESC, id DESC`, sem expor a data no contrato JSON nem alterá-la em edições.

**Tech Stack:** Java 17, Spring Boot, Spring Data JDBC, SQLite, H2 e JUnit 5.

## Global Constraints

- “Mais recente” significa criado por último.
- Editar um projeto não altera sua posição.
- A paginação continua no site externo.
- O formato JSON público não muda.
- `created_at` não será exposto na resposta.

---

### Task 1: Coluna de criação e migração SQLite

**Files:**
- Modify: `src/main/resources/schema.sql`
- Modify: `src/main/java/com/andreyferraz/gestao/config/ProjetoSchemaInitializer.java`
- Test: `src/test/java/com/andreyferraz/gestao/config/ProjetoSchemaInitializerTest.java`

**Interfaces:**
- Consumes: `ProjetoSchemaInitializer(JdbcTemplate)` e `ensureProjetoTable()`.
- Produces: coluna SQL `projeto.created_at TEXT`, preenchida para linhas legadas.

- [ ] **Step 1: Escrever testes que falham para banco novo e migração**

Atualizar o teste de estrutura para esperar `created_at` após `link` e criar:

```java
@Test
void sqliteComTabelaLegada_deveAdicionarEPreencherCreatedAt() {
    jdbcTemplate.execute("""
            CREATE TABLE projeto (
                id TEXT PRIMARY KEY,
                titulo TEXT NOT NULL,
                descricao TEXT NOT NULL,
                imagem_url TEXT NOT NULL,
                link TEXT NOT NULL
            )
            """);
    jdbcTemplate.update("""
            INSERT INTO projeto (id, titulo, descricao, imagem_url, link)
            VALUES (?, ?, ?, ?, ?)
            """, "legado", "Legado", "Descrição", "legado.webp", "https://example.com");

    new ProjetoSchemaInitializer(jdbcTemplate).ensureProjetoTable();

    assertThat(jdbcTemplate.queryForObject(
            "SELECT created_at FROM projeto WHERE id = 'legado'", String.class))
            .isNotBlank();
}
```

No teste que insere um projeto depois da criação da tabela, incluir
`created_at` explicitamente no `INSERT`, pois a aplicação — e não o banco —
será responsável por fornecer esse valor.

- [ ] **Step 2: Executar o teste e confirmar a falha**

Run:

```bash
./mvnw -Dtest=ProjetoSchemaInitializerTest test
```

Expected: FAIL porque a tabela criada e a tabela legada ainda não recebem `created_at`.

- [ ] **Step 3: Implementar a coluna e a migração mínima**

Acrescentar ao `CREATE TABLE` do initializer e de `schema.sql`:

```sql
created_at TEXT NOT NULL
```

Depois do `CREATE TABLE IF NOT EXISTS`, detectar a coluna com `PRAGMA table_info(projeto)`. Quando ausente:

```java
jdbcTemplate.execute("ALTER TABLE projeto ADD COLUMN created_at TEXT");
jdbcTemplate.update("""
        UPDATE projeto
        SET created_at = STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'now')
        WHERE created_at IS NULL OR trim(created_at) = ''
        """);
```

Executar o mesmo `UPDATE` também quando a coluna já existir, cobrindo uma migração interrompida.

- [ ] **Step 4: Executar os testes do initializer**

Run:

```bash
./mvnw -Dtest=ProjetoSchemaInitializerTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/schema.sql \
  src/main/java/com/andreyferraz/gestao/config/ProjetoSchemaInitializer.java \
  src/test/java/com/andreyferraz/gestao/config/ProjetoSchemaInitializerTest.java
git commit -m "feat: add project creation timestamp"
```

### Task 2: Persistência e listagem ordenada

**Files:**
- Modify: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepository.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoService.java`
- Test: `src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepositoryTest.java`
- Test: `src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoServiceTest.java`

**Interfaces:**
- Consumes: tabela `projeto` com `created_at`.
- Produces: `void inserir(UUID id, String titulo, String descricao, String imagemUrl, String link, String createdAt)` e `List<Projeto> findAllOrderByCriacaoRecente()`.

- [ ] **Step 1: Escrever o teste de repositório que falha**

Criar um teste com datas explícitas:

```java
@Test
void findAllOrderByCriacaoRecente_deveRetornarMaisNovoPrimeiro() {
    UUID antigo = UUID.randomUUID();
    UUID recente = UUID.randomUUID();
    repository.inserir(
            antigo, "Antigo", "Descrição", "antigo.webp",
            "https://old.example", "2026-07-29T10:00:00Z");
    repository.inserir(
            recente, "Recente", "Descrição", "recente.webp",
            "https://new.example", "2026-07-30T10:00:00Z");

    assertEquals(
            java.util.List.of(recente, antigo),
            repository.findAllOrderByCriacaoRecente().stream()
                    .map(Projeto::getId)
                    .toList());
}
```

Atualizar as chamadas já existentes a `inserir` com uma data UTC fixa.

- [ ] **Step 2: Escrever o teste de serviço que falha**

Substituir o teste atual de listagem por:

```java
@Test
void listarProjetos_deveUsarOrdemDeCriacaoRecenteDoRepositorio() {
    Projeto recente = projetoValido();
    Projeto antigo = new Projeto(
            null, "Outro", "Outra descrição", "outra.webp",
            "https://other.example.com");
    when(repository.findAllOrderByCriacaoRecente())
            .thenReturn(List.of(recente, antigo));

    assertEquals(List.of(recente, antigo), service.listarProjetos());
    verify(repository).findAllOrderByCriacaoRecente();
}
```

Atualizar os `verify` e `doThrow` de criação para a assinatura de seis argumentos,
usando `anyString()` no instante.

- [ ] **Step 3: Executar os testes e confirmar as falhas**

Run:

```bash
./mvnw -Dtest=ProjetoRepositoryTest,ProjetoServiceTest test
```

Expected: FAIL de compilação porque a nova assinatura e a consulta ainda não existem.

- [ ] **Step 4: Implementar inserção temporal e consulta ordenada**

No repositório:

```java
@Modifying
@Query("""
        INSERT INTO projeto (
            id, titulo, descricao, imagem_url, link, created_at
        )
        VALUES (:id, :titulo, :descricao, :imagemUrl, :link, :createdAt)
        """)
void inserir(
        UUID id, String titulo, String descricao,
        String imagemUrl, String link, String createdAt);

@Query("""
        SELECT id, titulo, descricao, imagem_url, link
        FROM projeto
        ORDER BY created_at DESC, id DESC
        """)
List<Projeto> findAllOrderByCriacaoRecente();
```

Adicionar `java.util.List` aos imports do repositório.

No serviço, passar `Instant.now().toString()` na criação e trocar a listagem por:

```java
return projetoRepository.findAllOrderByCriacaoRecente();
```

Não adicionar `created_at` às consultas de atualização.
Nos dois testes de compatibilidade que inserem linhas diretamente com
`JdbcTemplate`, acrescentar `created_at` e uma data UTC fixa para satisfazer o
novo schema de teste.

- [ ] **Step 5: Executar os testes focados**

Run:

```bash
./mvnw -Dtest=ProjetoRepositoryTest,ProjetoServiceTest,ProjetoControllerTest test
```

Expected: PASS.

- [ ] **Step 6: Executar a suíte completa**

Run:

```bash
./mvnw test
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepository.java \
  src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoService.java \
  src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepositoryTest.java \
  src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoServiceTest.java
git commit -m "feat: list newest projects first"
```
