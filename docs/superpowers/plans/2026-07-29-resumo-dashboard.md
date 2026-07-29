# Módulo de Resumo do Painel Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Adicionar ao painel uma tela inicial de resumo com indicadores, gráfico de clientes ativos por valor mensal exato e os cinco cadastros mais recentes de clientes e leads.

**Architecture:** Um novo pacote backend `module/resumo` fará consultas de leitura consolidadas e publicará `GET /resumo`. Um módulo JavaScript isolado consumirá esse contrato, controlará o Chart.js e preencherá a aba Resumo sem misturar as regras de apresentação com o arquivo legado do dashboard.

**Tech Stack:** Java 17, Spring Boot 4.0.5, Spring Data JDBC, SQLite/H2, Thymeleaf, JavaScript ES5/UMD, Node.js 22 `node:test`, Chart.js 4.4.7.

## Global Constraints

- O gráfico agrupa somente clientes ativos pelo valor mensal exato.
- Clientes ativos com valor mensal zero pertencem à fatia `R$ 0,00`.
- Total de clientes inclui ativos e inativos; receita mensal e domínios ativos consideram somente ativos.
- As listas retornam no máximo cinco registros e usam a criação original, nunca a atualização.
- A aba Resumo abre em toda entrada ou recarga; a última aba não é restaurada.
- O endpoint é somente leitura e retorna valores monetários como `BigDecimal`.
- Chart.js deve permanecer fixado em `4.4.7`.
- Dados da API devem entrar no DOM por `textContent`, sem interpolação em `innerHTML`.
- O layout deve empilhar os cards em telas de até `900px`.
- Execute Maven com Java 17. Nesta máquina use `JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home`.
- A suíte base requer o mock maker por subclassificação porque o sandbox bloqueia o auto-attach do Byte Buddy.

---

### Task 1: Estabilizar a suíte existente no ambiente

**Files:**
- Create: `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`

**Interfaces:**
- Consumes: Mockito já fornecido pelas dependências de teste do Spring Boot.
- Produces: configuração `mock-maker-subclass` usada por todos os testes Mockito.

- [ ] **Step 1: Confirmar a falha ambiental existente**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw test
```

Expected: FAIL antes das asserções, com `Could not initialize inline Byte Buddy mock maker` e `Could not self-attach`.

- [ ] **Step 2: Configurar o mock maker sem auto-attach**

Create `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker` with exactly:

```text
mock-maker-subclass
```

- [ ] **Step 3: Confirmar a linha de base verde**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw test
```

Expected: PASS, including the 11 tests that existed before this feature.

- [ ] **Step 4: Commit**

```bash
git add src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
git commit -m "test: stabilize mockito in restricted runtime"
```

---

### Task 2: Registrar a data original nas novas inserções

**Files:**
- Modify: `src/main/java/com/andreyferraz/gestao/module/cliente/Cliente.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/cliente/ClienteRepository.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/cliente/ClienteService.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/lead/Lead.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/lead/LeadRepository.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/lead/LeadService.java`
- Modify: `src/main/resources/schema.sql`
- Modify: `src/test/java/com/andreyferraz/gestao/module/cliente/ClienteServiceTest.java`
- Modify: `src/test/java/com/andreyferraz/gestao/module/lead/LeadServiceTest.java`

**Interfaces:**
- Consumes: `Instant.now().toString()` para gerar timestamps UTC ISO 8601.
- Produces: `Cliente.getCreatedAt()`, `Lead.getCreatedAt()` e colunas `cliente.created_at`/`lead.created_at`.

- [ ] **Step 1: Escrever testes falhos para criação e preservação**

Add to `ClienteServiceTest`:

```java
@Test
void criar_deveDefinirDataOriginalEmUtc() {
    var input = clienteValido();

    var result = clienteService.criar(input);

    assertNotNull(result.getCreatedAt());
    assertDoesNotThrow(() -> Instant.parse(result.getCreatedAt()));
    verify(clienteRepository).inserir(
            eq(result.getId()), eq(result.getNome()), eq(result.getContato()),
            eq(result.getDominioAplicacao()), eq(result.getDataVencimentoDominio()),
            eq(result.getInformacoesUteis()), eq(result.getValorMensal()),
            eq(result.getAtivo()), isNull(), eq(result.getCreatedAt()));
}

@Test
void atualizar_devePreservarDataOriginal() {
    UUID id = UUID.randomUUID();
    var existente = clienteValido();
    existente.setId(id);
    existente.setCreatedAt("2026-06-01T10:00:00Z");
    var alterado = clienteValido();
    alterado.setCreatedAt("2099-01-01T00:00:00Z");
    when(clienteRepository.findById(id)).thenReturn(Optional.of(existente));

    var result = clienteService.atualizar(id, alterado);

    assertEquals("2026-06-01T10:00:00Z", result.getCreatedAt());
}
```

Add a private `clienteValido()` fixture containing the same complete fields used by the existing creation tests. Import `java.time.Instant`, `java.util.Optional` and `assertDoesNotThrow`.

Use this exact fixture:

```java
private Cliente clienteValido() {
    var cliente = new Cliente();
    cliente.setNome("Nome");
    cliente.setContato("contato");
    cliente.setDominioAplicacao("dominio");
    cliente.setDataVencimentoDominio(LocalDate.of(2026, 1, 1));
    cliente.setInformacoesUteis("informacoes");
    cliente.setValorMensal(new BigDecimal("100.00"));
    cliente.setAtivo(1);
    cliente.setVendedorId(null);
    return cliente;
}
```

Add to `LeadServiceTest`:

```java
@Test
void criar_deveDefinirDataOriginalEmUtc() {
    var input = new Lead(
            null, "Lead", "11999999999", BigDecimal.TEN, BigDecimal.ONE,
            "observacao", null, null);
    when(leadRepository.findById(any(UUID.class))).thenAnswer(invocation -> {
        input.setId(invocation.getArgument(0));
        return Optional.of(input);
    });

    var result = leadService.criar(input);

    assertNotNull(result.getCreatedAt());
    assertDoesNotThrow(() -> Instant.parse(result.getCreatedAt()));
    verify(leadRepository).inserir(
            eq(result.getId()), eq("Lead"), eq("11999999999"),
            eq(BigDecimal.TEN), eq(BigDecimal.ONE), eq("observacao"),
            eq(result.getCreatedAt()));
}
```

Update existing `new Lead(...)` fixtures to the exact constructor order:

```java
new Lead(id, nome, telefone, desenvolvimento, manutencao, observacoes, createdAt, updatedAt)
```

- [ ] **Step 2: Executar os testes e observar a falha correta**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=ClienteServiceTest,LeadServiceTest test
```

Expected: FAIL at compilation because `createdAt` and the new repository parameters do not exist.

- [ ] **Step 3: Adicionar os campos e persistir o timestamp na criação**

Add to both entities:

```java
@Column("created_at")
private String createdAt;
```

Place `Lead.createdAt` immediately before `updatedAt`.

Change the insert signatures and SQL:

```java
// ClienteRepository
@Query("""
        INSERT INTO cliente (
            id, nome, contato, dominio_aplicacao, data_vencimento_dominio,
            informacoes_uteis, valor_mensal, ativo, vendedor_id, created_at
        ) VALUES (
            :id, :nome, :contato, :dominioAplicacao, :dataVencimentoDominio,
            :informacoesUteis, :valorMensal, :ativo, :vendedorId, :createdAt
        )
        """)
void inserir(
        UUID id, String nome, String contato, String dominioAplicacao,
        LocalDate dataVencimentoDominio, String informacoesUteis,
        BigDecimal valorMensal, Integer ativo, UUID vendedorId, String createdAt);
```

```java
// LeadRepository
@Query("""
        INSERT INTO lead (
            id, nome, telefone, orcamento_desenvolvimento,
            orcamento_manutencao_hospedagem, observacoes, created_at, updated_at
        ) VALUES (
            :id, :nome, :telefone, :orcamentoDesenvolvimento,
            :orcamentoManutencaoHospedagem, :observacoes, :createdAt,
            STRFTIME('%Y-%m-%d %H:%M:%f', 'now')
        )
        """)
void inserir(
        UUID id, String nome, String telefone,
        BigDecimal orcamentoDesenvolvimento,
        BigDecimal orcamentoManutencaoHospedagem,
        String observacoes, String createdAt);
```

In each `criar` method, set the timestamp unconditionally before calling the repository:

```java
entity.setCreatedAt(Instant.now().toString());
```

In `ClienteService.atualizar`, retain the entity returned by `findById` and copy its original timestamp:

```java
Cliente existente = clienteRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException(
                "Cliente nao encontrado para o id: " + id));
clienteAtualizado.setId(id);
clienteAtualizado.setCreatedAt(existente.getCreatedAt());
```

Do not add `created_at` to either update SQL.

Add to both table definitions in `schema.sql`:

```sql
created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
```

- [ ] **Step 4: Executar os testes focados**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=ClienteServiceTest,LeadServiceTest test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/andreyferraz/gestao/module/cliente \
  src/main/java/com/andreyferraz/gestao/module/lead \
  src/main/resources/schema.sql \
  src/test/java/com/andreyferraz/gestao/module/cliente/ClienteServiceTest.java \
  src/test/java/com/andreyferraz/gestao/module/lead/LeadServiceTest.java
git commit -m "feat: preserve original registration timestamps"
```

---

### Task 3: Migrar timestamps em bancos SQLite existentes

**Files:**
- Modify: `src/main/java/com/andreyferraz/gestao/config/ClienteSchemaInitializer.java`
- Modify: `src/main/java/com/andreyferraz/gestao/config/LeadSchemaInitializer.java`
- Create: `src/test/java/com/andreyferraz/gestao/config/CreatedAtSchemaInitializerTest.java`

**Interfaces:**
- Consumes: `ClienteSchemaInitializer.ensureContatoColumn()` e `LeadSchemaInitializer.ensureUpdatedAtColumn()`.
- Produces: colunas preenchidas no formato `yyyy-MM-dd'T'HH:mm:ss.SSS'Z'` em bancos antigos.

- [ ] **Step 1: Escrever testes de migração com SQLite real**

Create `CreatedAtSchemaInitializerTest` using `SingleConnectionDataSource`:

```java
class CreatedAtSchemaInitializerTest {

    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    @Test
    void clienteAntigo_deveReceberCreatedAtUtc() {
        jdbcTemplate.execute("""
                CREATE TABLE cliente (
                    id TEXT PRIMARY KEY,
                    data_vencimento_dominio TEXT,
                    ativo INTEGER
                )
                """);
        jdbcTemplate.update(
                "INSERT INTO cliente (id, ativo) VALUES (?, 1)",
                UUID.randomUUID().toString());

        new ClienteSchemaInitializer(jdbcTemplate).ensureContatoColumn();

        String createdAt = jdbcTemplate.queryForObject(
                "SELECT created_at FROM cliente", String.class);
        assertNotNull(createdAt);
        assertDoesNotThrow(() -> Instant.parse(createdAt));
    }

    @Test
    void leadAntigo_deveUsarUpdatedAtComoMelhorDataOriginal() {
        jdbcTemplate.execute("""
                CREATE TABLE lead (
                    id TEXT PRIMARY KEY,
                    updated_at TEXT
                )
                """);
        jdbcTemplate.update(
                "INSERT INTO lead (id, updated_at) VALUES (?, ?)",
                UUID.randomUUID().toString(), "2026-05-10 12:30:45.123");

        new LeadSchemaInitializer(jdbcTemplate).ensureUpdatedAtColumn();

        assertEquals(
                "2026-05-10T12:30:45.123Z",
                jdbcTemplate.queryForObject(
                        "SELECT created_at FROM lead", String.class));
    }
}
```

- [ ] **Step 2: Executar e confirmar a falha por coluna ausente**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=CreatedAtSchemaInitializerTest test
```

Expected: FAIL because neither initializer creates `created_at`.

- [ ] **Step 3: Implementar as migrações idempotentes**

In `ClienteSchemaInitializer.ensureContatoColumn()` add:

```java
if (!hasColumn(CLIENTE_TABLE, "created_at")) {
    jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN created_at TEXT");
}

jdbcTemplate.execute("""
        UPDATE cliente
        SET created_at = STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'now')
        WHERE created_at IS NULL OR trim(created_at) = ''
        """);
```

In `LeadSchemaInitializer.ensureUpdatedAtColumn()` add:

```java
if (!hasColumn(LEAD_TABLE, "created_at")) {
    jdbcTemplate.execute("ALTER TABLE lead ADD COLUMN created_at TEXT");
}

jdbcTemplate.execute("""
        UPDATE lead
        SET created_at = COALESCE(
            STRFTIME('%Y-%m-%dT%H:%M:%fZ', updated_at),
            STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'now')
        )
        WHERE created_at IS NULL OR trim(created_at) = ''
        """);
```

Keep the existing SQLite guard so H2 startup remains unchanged.

- [ ] **Step 4: Executar os testes de migração e a suíte**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=CreatedAtSchemaInitializerTest test
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw test
```

Expected: both commands PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/andreyferraz/gestao/config/ClienteSchemaInitializer.java \
  src/main/java/com/andreyferraz/gestao/config/LeadSchemaInitializer.java \
  src/test/java/com/andreyferraz/gestao/config/CreatedAtSchemaInitializerTest.java
git commit -m "feat: migrate original registration timestamps"
```

---

### Task 4: Consultar e compor o resumo

**Files:**
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/ResumoIndicadoresDto.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/DistribuicaoValorMensalDto.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/ClienteRecenteDto.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/LeadRecenteDto.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/ResumoDashboardDto.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/ResumoRepository.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/ResumoService.java`
- Create: `src/test/java/com/andreyferraz/gestao/module/resumo/ResumoRepositoryTest.java`

**Interfaces:**
- Produces:
  - `ResumoRepository.buscarIndicadores(): ResumoIndicadoresDto`
  - `ResumoRepository.buscarDistribuicaoValoresMensais(): List<DistribuicaoValorMensalDto>`
  - `ResumoRepository.buscarUltimosClientes(): List<ClienteRecenteDto>`
  - `ResumoRepository.buscarUltimosLeads(): List<LeadRecenteDto>`
  - `ResumoService.obterResumo(): ResumoDashboardDto`

- [ ] **Step 1: Escrever testes de consulta contra SQLite real**

Create `ResumoRepositoryTest` with an in-memory `SingleConnectionDataSource`. In `@BeforeEach`, create minimal `cliente` and `lead` tables including all columns read by the repository.

Add four tests with literal fixtures:

```java
@Test
void buscarIndicadores_deveSepararTotalDeReceitaAtiva() {
    inserirCliente("Ativo 100", "2026-07-01T10:00:00Z", "100.00", 1);
    inserirCliente("Ativo 250", "2026-07-02T10:00:00Z", "250.00", 1);
    inserirCliente("Inativo 900", "2026-07-03T10:00:00Z", "900.00", 0);

    var result = repository.buscarIndicadores();

    assertEquals(3, result.totalClientes());
    assertEquals(0, new BigDecimal("350.00").compareTo(result.receitaMensalAtiva()));
    assertEquals(2, result.dominiosAtivos());
}

@Test
void buscarDistribuicao_deveAgruparValoresExatosSomenteDeAtivos() {
    inserirCliente("Zero", "2026-07-01T10:00:00Z", "0.00", 1);
    inserirCliente("Cem A", "2026-07-02T10:00:00Z", "100.00", 1);
    inserirCliente("Cem B", "2026-07-03T10:00:00Z", "100.0", 1);
    inserirCliente("Inativo", "2026-07-04T10:00:00Z", "100.00", 0);

    var result = repository.buscarDistribuicaoValoresMensais();

    assertEquals(2, result.size());
    assertEquals(0, BigDecimal.ZERO.compareTo(result.get(0).valorMensal()));
    assertEquals(1, result.get(0).quantidadeClientes());
    assertEquals(0, new BigDecimal("100.00").compareTo(result.get(1).valorMensal()));
    assertEquals(2, result.get(1).quantidadeClientes());
}

@Test
void buscarUltimosClientes_deveLimitarCincoEIncluirInativos() {
    for (int day = 1; day <= 6; day++) {
        inserirCliente(
                "Cliente " + day,
                "2026-07-0" + day + "T10:00:00Z",
                "50.00",
                day == 6 ? 0 : 1);
    }

    var result = repository.buscarUltimosClientes();

    assertEquals(5, result.size());
    assertEquals("Cliente 6", result.get(0).nome());
    assertFalse(result.get(0).ativo());
    assertEquals("Cliente 2", result.get(4).nome());
}

@Test
void buscarUltimosLeads_deveOrdenarPorCriacaoSemUsarAtualizacao() {
    inserirLead(
            "Lead antigo editado", "2026-07-01T10:00:00Z",
            "2026-07-29 18:00:00.000", "500.00");
    inserirLead(
            "Lead novo", "2026-07-20T10:00:00Z",
            "2026-07-20 10:00:00.000", "300.00");

    var result = repository.buscarUltimosLeads();

    assertEquals("Lead novo", result.get(0).nome());
    assertEquals("Lead antigo editado", result.get(1).nome());
}
```

The helper methods must generate UUIDs, insert complete rows and never calculate expected results.

Use these table definitions and helpers:

```java
@BeforeEach
void setUp() {
    dataSource = new SingleConnectionDataSource("jdbc:sqlite::memory:", true);
    jdbcTemplate = new JdbcTemplate(dataSource);
    jdbcTemplate.execute("""
            CREATE TABLE cliente (
                id TEXT PRIMARY KEY,
                nome TEXT NOT NULL,
                valor_mensal NUMERIC,
                ativo INTEGER NOT NULL,
                created_at TEXT NOT NULL
            )
            """);
    jdbcTemplate.execute("""
            CREATE TABLE lead (
                id TEXT PRIMARY KEY,
                nome TEXT NOT NULL,
                orcamento_manutencao_hospedagem NUMERIC,
                created_at TEXT NOT NULL,
                updated_at TEXT
            )
            """);
    repository = new ResumoRepository(jdbcTemplate);
}

private void inserirCliente(
        String nome, String createdAt, String valorMensal, int ativo) {
    jdbcTemplate.update("""
            INSERT INTO cliente (id, nome, valor_mensal, ativo, created_at)
            VALUES (?, ?, ?, ?, ?)
            """,
            UUID.randomUUID().toString(), nome,
            new BigDecimal(valorMensal), ativo, createdAt);
}

private void inserirLead(
        String nome, String createdAt, String updatedAt, String orcamento) {
    jdbcTemplate.update("""
            INSERT INTO lead (
                id, nome, orcamento_manutencao_hospedagem, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?)
            """,
            UUID.randomUUID().toString(), nome,
            new BigDecimal(orcamento), createdAt, updatedAt);
}
```

Destroy the single connection in `@AfterEach` with `dataSource.destroy()`.

- [ ] **Step 2: Executar e observar a falha por classes ausentes**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=ResumoRepositoryTest test
```

Expected: FAIL at compilation because the summary package does not exist.

- [ ] **Step 3: Criar os DTOs com tipos consistentes**

```java
public record ResumoIndicadoresDto(
        long totalClientes,
        BigDecimal receitaMensalAtiva,
        long dominiosAtivos) {
}

public record DistribuicaoValorMensalDto(
        BigDecimal valorMensal,
        long quantidadeClientes) {
}

public record ClienteRecenteDto(
        UUID id,
        String nome,
        String createdAt,
        BigDecimal valorMensal,
        boolean ativo) {
}

public record LeadRecenteDto(
        UUID id,
        String nome,
        String createdAt,
        BigDecimal orcamentoManutencaoHospedagem) {
}

public record ResumoDashboardDto(
        ResumoIndicadoresDto indicadores,
        List<DistribuicaoValorMensalDto> distribuicaoValoresMensais,
        List<ClienteRecenteDto> ultimosClientes,
        List<LeadRecenteDto> ultimosLeads) {
}
```

- [ ] **Step 4: Implementar as consultas reais**

Create `ResumoRepository` as a concrete `@Repository` with constructor-injected `JdbcTemplate`. Use these exact queries:

```java
private static final String INDICADORES_SQL = """
        SELECT
            COUNT(*) AS total_clientes,
            COALESCE(SUM(CASE WHEN ativo = 1 THEN valor_mensal ELSE 0 END), 0)
                AS receita_mensal_ativa,
            COALESCE(SUM(CASE WHEN ativo = 1 THEN 1 ELSE 0 END), 0)
                AS dominios_ativos
        FROM cliente
        """;

private static final String DISTRIBUICAO_SQL = """
        SELECT
            COALESCE(valor_mensal, 0) AS valor_mensal,
            COUNT(*) AS quantidade_clientes
        FROM cliente
        WHERE ativo = 1
        GROUP BY COALESCE(valor_mensal, 0)
        ORDER BY COALESCE(valor_mensal, 0) ASC
        """;

private static final String ULTIMOS_CLIENTES_SQL = """
        SELECT id, nome, created_at, COALESCE(valor_mensal, 0) AS valor_mensal, ativo
        FROM cliente
        ORDER BY created_at DESC, id DESC
        LIMIT 5
        """;

private static final String ULTIMOS_LEADS_SQL = """
        SELECT id, nome, created_at,
               COALESCE(orcamento_manutencao_hospedagem, 0)
                   AS orcamento_manutencao_hospedagem
        FROM lead
        ORDER BY created_at DESC, id DESC
        LIMIT 5
        """;
```

Map UUIDs with `UUID.fromString(rs.getString("id"))`, money with `rs.getBigDecimal(...)`, counts with `rs.getLong(...)`, and active status with `rs.getInt("ativo") == 1`.

Create `ResumoService`:

```java
@Service
@RequiredArgsConstructor
public class ResumoService {

    private final ResumoRepository resumoRepository;

    @Transactional(readOnly = true)
    public ResumoDashboardDto obterResumo() {
        return new ResumoDashboardDto(
                resumoRepository.buscarIndicadores(),
                resumoRepository.buscarDistribuicaoValoresMensais(),
                resumoRepository.buscarUltimosClientes(),
                resumoRepository.buscarUltimosLeads());
    }
}
```

- [ ] **Step 5: Executar o teste focado**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=ResumoRepositoryTest test
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/andreyferraz/gestao/module/resumo \
  src/test/java/com/andreyferraz/gestao/module/resumo/ResumoRepositoryTest.java
git commit -m "feat: aggregate dashboard summary data"
```

---

### Task 5: Publicar o contrato `GET /resumo`

**Files:**
- Create: `src/main/java/com/andreyferraz/gestao/module/resumo/ResumoController.java`
- Create: `src/test/java/com/andreyferraz/gestao/module/resumo/ResumoControllerTest.java`

**Interfaces:**
- Consumes: `ResumoService.obterResumo(): ResumoDashboardDto`.
- Produces: `GET /resumo` com `200 OK` e `ResumoDashboardDto` em JSON.

- [ ] **Step 1: Escrever o teste falho do contrato HTTP**

```java
class ResumoControllerTest {

    @Test
    void getResumo_deveRetornarContratoConsolidado() throws Exception {
        ResumoService service = mock(ResumoService.class);
        UUID clienteId = UUID.randomUUID();
        UUID leadId = UUID.randomUUID();
        var resposta = new ResumoDashboardDto(
                new ResumoIndicadoresDto(3, new BigDecimal("350.00"), 2),
                List.of(new DistribuicaoValorMensalDto(
                        new BigDecimal("100.00"), 2)),
                List.of(new ClienteRecenteDto(
                        clienteId, "Cliente novo", "2026-07-29T10:00:00Z",
                        new BigDecimal("100.00"), true)),
                List.of(new LeadRecenteDto(
                        leadId, "Lead novo", "2026-07-29T09:00:00Z",
                        new BigDecimal("300.00"))));
        when(service.obterResumo()).thenReturn(resposta);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ResumoController(service))
                .build();

        mockMvc.perform(get("/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.totalClientes").value(3))
                .andExpect(jsonPath("$.indicadores.receitaMensalAtiva").value(350.0))
                .andExpect(jsonPath("$.indicadores.dominiosAtivos").value(2))
                .andExpect(jsonPath("$.distribuicaoValoresMensais[0].valorMensal").value(100.0))
                .andExpect(jsonPath("$.distribuicaoValoresMensais[0].quantidadeClientes").value(2))
                .andExpect(jsonPath("$.ultimosClientes[0].id").value(clienteId.toString()))
                .andExpect(jsonPath("$.ultimosClientes[0].createdAt")
                        .value("2026-07-29T10:00:00Z"))
                .andExpect(jsonPath("$.ultimosLeads[0].id").value(leadId.toString()));
    }
}
```

- [ ] **Step 2: Executar e confirmar 404/classe ausente**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=ResumoControllerTest test
```

Expected: FAIL at compilation because `ResumoController` does not exist.

- [ ] **Step 3: Criar o controller mínimo**

```java
@RestController
@RequestMapping("/resumo")
@RequiredArgsConstructor
public class ResumoController {

    private final ResumoService resumoService;

    @GetMapping
    public ResponseEntity<ResumoDashboardDto> obterResumo() {
        return ResponseEntity.ok(resumoService.obterResumo());
    }
}
```

- [ ] **Step 4: Executar teste e suíte backend**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=ResumoControllerTest test
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/andreyferraz/gestao/module/resumo/ResumoController.java \
  src/test/java/com/andreyferraz/gestao/module/resumo/ResumoControllerTest.java
git commit -m "feat: expose consolidated summary endpoint"
```

---

### Task 6: Criar o controlador frontend do resumo

**Files:**
- Create: `src/main/resources/static/js/resumo.js`
- Create: `src/test/js/resumo.test.js`

**Interfaces:**
- Produces: `window.GestaoResumo.criarPainel(options): { carregar(): Promise<void>, destruir(): void }`.
- `options` requires `document`, `Chart`, `buscarJson`, `formatarMoeda`, `formatarDataHora`.
- Consumes these DOM ids: `kpi-clientes`, `kpi-receita`, `kpi-dominios`, `resumo-feedback`, `resumo-grafico`, `resumo-grafico-estado`, `resumo-grafico-legenda`, `resumo-clientes-lista`, `resumo-leads-lista`.

- [ ] **Step 1: Escrever testes Node para dados, vazio, falha e recarga**

Create a minimal `FakeElement` in the test with `textContent`, `hidden`, `children`, `replaceChildren`, `appendChild`, and `getContext`. Create a fake document whose `getElementById` returns one element for every required id and whose `createElement` returns a `FakeElement`.

Use these exact test doubles and builders:

```javascript
class FakeElement {
    constructor() {
        this.textContent = "";
        this.hidden = false;
        this.children = [];
        this.className = "";
    }

    replaceChildren() {
        this.children = [];
    }

    appendChild(child) {
        this.children.push(child);
        return child;
    }

    getContext() {
        return {};
    }
}

function criarAmbiente() {
    const ids = [
        "kpi-clientes", "kpi-receita", "kpi-dominios", "resumo-feedback",
        "resumo-grafico", "resumo-grafico-estado", "resumo-grafico-legenda",
        "resumo-clientes-lista", "resumo-leads-lista"
    ];
    const elementos = Object.fromEntries(ids.map(function (id) {
        return [id, new FakeElement()];
    }));
    const graficos = [];

    class FakeChart {
        constructor(context, config) {
            this.context = context;
            this.config = config;
            this.destroyed = false;
            graficos.push(this);
        }

        destroy() {
            this.destroyed = true;
        }
    }

    return {
        elementos: elementos,
        graficos: graficos,
        FakeChart: FakeChart,
        document: {
            getElementById: function (id) { return elementos[id]; },
            createElement: function () { return new FakeElement(); }
        }
    };
}

function criarPainelComPayload(ambiente, payload) {
    return GestaoResumo.criarPainel({
        document: ambiente.document,
        Chart: ambiente.FakeChart,
        buscarJson: async function () { return payload; },
        formatarMoeda: function (value) {
            return "R$ " + Number(value).toFixed(2);
        },
        formatarDataHora: function () {
            return "29/07/2026 10:00";
        }
    });
}

function payloadComUmaFatia() {
    return {
        indicadores: {
            totalClientes: 1,
            receitaMensalAtiva: 100,
            dominiosAtivos: 1
        },
        distribuicaoValoresMensais: [
            { valorMensal: 100, quantidadeClientes: 1 }
        ],
        ultimosClientes: [],
        ultimosLeads: []
    };
}
```

Add these tests:

```javascript
test("carregar renderiza indicadores, listas seguras e gráfico", async function () {
    const ambiente = criarAmbiente();
    const payload = {
        indicadores: {
            totalClientes: 3,
            receitaMensalAtiva: 350,
            dominiosAtivos: 2
        },
        distribuicaoValoresMensais: [
            { valorMensal: 100, quantidadeClientes: 2 }
        ],
        ultimosClientes: [
            {
                id: "c1",
                nome: "<img src=x onerror=alert(1)>",
                createdAt: "2026-07-29T10:00:00Z",
                valorMensal: 100,
                ativo: true
            }
        ],
        ultimosLeads: [
            {
                id: "l1",
                nome: "Lead novo",
                createdAt: "2026-07-29T09:00:00Z",
                orcamentoManutencaoHospedagem: 300
            }
        ]
    };
    const painel = GestaoResumo.criarPainel({
        document: ambiente.document,
        Chart: ambiente.FakeChart,
        buscarJson: async function () { return payload; },
        formatarMoeda: function (value) { return "R$ " + Number(value).toFixed(2); },
        formatarDataHora: function () { return "29/07/2026 10:00"; }
    });

    await painel.carregar();

    assert.equal(ambiente.elementos["kpi-clientes"].textContent, "3");
    assert.equal(ambiente.elementos["kpi-receita"].textContent, "R$ 350.00");
    assert.match(
        ambiente.elementos["resumo-clientes-lista"].children[0].textContent,
        /<img src=x onerror=alert\(1\)>/);
    assert.equal(ambiente.graficos[0].config.data.labels[0], "R$ 100.00");
    assert.equal(
        ambiente.elementos["resumo-grafico-legenda"].children[0].textContent,
        "R$ 100.00 — 2 clientes");
});

test("carregar mostra estados vazios sem instanciar gráfico", async function () {
    const ambiente = criarAmbiente();
    const painel = criarPainelComPayload(ambiente, {
        indicadores: { totalClientes: 0, receitaMensalAtiva: 0, dominiosAtivos: 0 },
        distribuicaoValoresMensais: [],
        ultimosClientes: [],
        ultimosLeads: []
    });

    await painel.carregar();

    assert.equal(ambiente.graficos.length, 0);
    assert.equal(ambiente.elementos["resumo-grafico"].hidden, true);
    assert.equal(
        ambiente.elementos["resumo-grafico-estado"].textContent,
        "Nenhum cliente ativo para exibir.");
    assert.equal(
        ambiente.elementos["resumo-clientes-lista"].children[0].textContent,
        "Nenhum cliente cadastrado.");
});

test("carregar isola falha do endpoint no feedback", async function () {
    const ambiente = criarAmbiente();
    const painel = GestaoResumo.criarPainel({
        document: ambiente.document,
        Chart: ambiente.FakeChart,
        buscarJson: async function () { throw new Error("offline"); },
        formatarMoeda: String,
        formatarDataHora: String
    });

    await painel.carregar();

    assert.equal(
        ambiente.elementos["resumo-feedback"].textContent,
        "Não foi possível carregar o resumo agora.");
});

test("recarregar destroi o gráfico anterior", async function () {
    const ambiente = criarAmbiente();
    const painel = criarPainelComPayload(ambiente, payloadComUmaFatia());

    await painel.carregar();
    await painel.carregar();

    assert.equal(ambiente.graficos[0].destroyed, true);
    assert.equal(ambiente.graficos.length, 2);
});
```

- [ ] **Step 2: Executar e confirmar módulo ausente**

Run:

```bash
node --test src/test/js/resumo.test.js
```

Expected: FAIL with `Cannot find module .../resumo.js`.

- [ ] **Step 3: Implementar o módulo UMD e renderização segura**

Wrap the module so Node receives `module.exports` and the browser receives `window.GestaoResumo`.

Implement `criarPainel(options)` with this lifecycle:

```javascript
function criarPainel(options) {
    const doc = options.document;
    const elementos = obterElementos(doc);
    let grafico = null;

    function destruirGrafico() {
        if (grafico && typeof grafico.destroy === "function") {
            grafico.destroy();
        }
        grafico = null;
    }

    function renderizarLista(container, itens, mensagemVazia, criarItem) {
        container.replaceChildren();
        if (itens.length === 0) {
            const vazio = doc.createElement("li");
            vazio.className = "resumo-vazio";
            vazio.textContent = mensagemVazia;
            container.appendChild(vazio);
            return;
        }
        itens.forEach(function (item) {
            container.appendChild(criarItem(item));
        });
    }

    async function carregar() {
        elementos.feedback.textContent = "Carregando resumo...";
        try {
            const payload = await options.buscarJson("/resumo");
            renderizarIndicadores(payload.indicadores || {});
            renderizarClientes(payload.ultimosClientes || []);
            renderizarLeads(payload.ultimosLeads || []);
            renderizarGrafico(payload.distribuicaoValoresMensais || []);
            elementos.feedback.textContent = "";
        } catch (error) {
            destruirGrafico();
            elementos.feedback.textContent =
                    "Não foi possível carregar o resumo agora.";
        }
    }

    return { carregar: carregar, destruir: destruirGrafico };
}
```

Build every client and lead `<li>` with `createElement`, set a single human-readable `textContent`, then append it. Never assign `innerHTML`.

For chart rendering:

- destroy the previous instance first;
- hide the canvas and show `Nenhum cliente ativo para exibir.` when the array is empty;
- if `options.Chart` is absent, show `Gráfico indisponível no momento.`;
- otherwise instantiate Chart with `type: "pie"`, monetary labels, quantities as data, the fixed palette `["#0f766e", "#0369a1", "#7c3aed", "#ea580c", "#dc2626", "#0891b2", "#65a30d", "#ca8a04"]`, and built-in legend disabled;
- populate `resumo-grafico-legenda` with safe `<li>` nodes in the format `R$ 100.00 — 2 clientes`.

- [ ] **Step 4: Executar os testes JS**

Run:

```bash
node --test src/test/js/resumo.test.js
```

Expected: 4 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/js/resumo.js src/test/js/resumo.test.js
git commit -m "feat: render dashboard summary safely"
```

---

### Task 7: Integrar a aba inicial, estilos e atualizações

**Files:**
- Modify: `src/main/resources/templates/home/dashboard.html`
- Modify: `src/main/resources/static/css/home.css`
- Modify: `src/main/resources/static/js/dashboard.js`
- Create: `src/test/java/com/andreyferraz/gestao/web/HomeControllerTest.java`

**Interfaces:**
- Consumes: `window.GestaoResumo.criarPainel(...)` e `GET /resumo`.
- Produces: aba `tab-resumo` ativa no HTML e `carregarResumoBackend()` para inicialização e pós-mutação.

- [ ] **Step 1: Escrever o teste falho da tela inicial renderizada**

Create:

```java
@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(username = "admin")
    void dashboard_deveAbrirComResumoAtivoEIndicadoresDentroDaAba() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("home/dashboard"))
                .andExpect(xpath(
                        "//button[@data-tab-target='tab-resumo' and contains(@class,'active')]")
                        .exists())
                .andExpect(xpath(
                        "//article[@id='tab-resumo' and contains(@class,'active')]")
                        .exists())
                .andExpect(xpath(
                        "//article[@id='tab-resumo']//p[@id='kpi-clientes']")
                        .exists())
                .andExpect(xpath(
                        "//article[@id='tab-resumo']//canvas[@id='resumo-grafico']")
                        .exists())
                .andExpect(xpath(
                        "//article[@id='tab-resumo']//ul[@id='resumo-clientes-lista']")
                        .exists())
                .andExpect(xpath(
                        "//article[@id='tab-resumo']//ul[@id='resumo-leads-lista']")
                        .exists());
    }
}
```

- [ ] **Step 2: Executar e confirmar a falha por markup ausente**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=HomeControllerTest test
```

Expected: FAIL because `tab-resumo` does not exist and `tab-clientes` is active.

- [ ] **Step 3: Adicionar o markup e scripts**

Before the Clientes button, add an active Resumo button:

```html
<button type="button" class="nav-tab active" data-tab-target="tab-resumo">
    <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M4 13h6V4H4v9zM14 20h6V11h-6v9zM4 20h6v-3H4v3zM14 7h6V4h-6v3z"/>
    </svg>
    <span>Resumo</span>
</button>
```

Remove `active` from the Clientes button. Move the current KPI row inside a new first tab panel and add the chart/lists:

```html
<article id="tab-resumo" class="panel tab-panel active">
    <div class="panel-head">
        <h3>Resumo</h3>
        <span class="chip">Visão geral</span>
    </div>
    <p class="panel-sub">Indicadores e cadastros mais recentes do painel.</p>
    <p id="resumo-feedback" class="obs" aria-live="polite"></p>

    <section class="kpi-row" aria-label="Resumo financeiro mensal">
        <article class="kpi">
            <h2>Total de Clientes</h2>
            <p id="kpi-clientes">0</p>
        </article>
        <article class="kpi">
            <h2>Receita Mensal Total</h2>
            <p id="kpi-receita">R$ 0,00</p>
        </article>
        <article class="kpi">
            <h2>Domínios Ativos</h2>
            <p id="kpi-dominios">0</p>
        </article>
    </section>

    <div class="resumo-grid">
        <section class="resumo-card resumo-grafico-card">
            <h4>Clientes ativos por valor mensal</h4>
            <div class="resumo-grafico-wrap">
                <canvas id="resumo-grafico"
                        role="img"
                        aria-label="Distribuição de clientes ativos por valor mensal"></canvas>
                <p id="resumo-grafico-estado" class="resumo-vazio"></p>
            </div>
            <ul id="resumo-grafico-legenda" class="resumo-legenda"></ul>
        </section>
        <section class="resumo-card">
            <h4>Últimos clientes cadastrados</h4>
            <ul id="resumo-clientes-lista" class="resumo-lista"></ul>
        </section>
        <section class="resumo-card">
            <h4>Últimos leads cadastrados</h4>
            <ul id="resumo-leads-lista" class="resumo-lista"></ul>
        </section>
    </div>
</article>
```

At the end of the document, load scripts in this order:

```html
<script defer src="https://cdn.jsdelivr.net/npm/chart.js@4.4.7/dist/chart.umd.min.js"></script>
<script defer src="/js/resumo.js?v=1" th:src="@{/js/resumo.js(v=1)}"></script>
<script defer src="https://cdn.jsdelivr.net/npm/jspdf@2.5.1/dist/jspdf.umd.min.js"></script>
<script defer src="/js/dashboard.js?v=20" th:src="@{/js/dashboard.js(v=20)}"></script>
```

- [ ] **Step 4: Estilizar desktop, vazio e mobile**

Add focused classes:

```css
.dashboard-page #tab-resumo .kpi-row {
    margin-top: 14px;
}

.dashboard-page .resumo-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
}

.dashboard-page .resumo-card {
    border: 1px solid var(--line);
    border-radius: 12px;
    padding: 14px;
    background: #f8fafc;
    min-width: 0;
}

.dashboard-page .resumo-grafico-card {
    grid-column: 1 / -1;
}

.dashboard-page .resumo-card h4 {
    margin: 0 0 12px;
    font-size: 15px;
}

.dashboard-page .resumo-grafico-wrap {
    position: relative;
    width: min(420px, 100%);
    min-height: 280px;
    margin: 0 auto;
}

.dashboard-page .resumo-lista,
.dashboard-page .resumo-legenda {
    list-style: none;
    padding: 0;
    margin: 0;
    display: grid;
    gap: 8px;
}

.dashboard-page .resumo-lista li,
.dashboard-page .resumo-legenda li {
    border: 1px solid var(--line);
    border-radius: 9px;
    padding: 9px 10px;
    background: #ffffff;
    color: var(--muted);
    font-size: 13px;
}

.dashboard-page .resumo-vazio {
    margin: 0;
    color: var(--muted);
    font-size: 13px;
}

@media (max-width: 900px) {
    .dashboard-page .resumo-grid {
        grid-template-columns: 1fr;
    }

    .dashboard-page .resumo-grafico-card {
        grid-column: auto;
    }
}
```

- [ ] **Step 5: Integrar carregamento inicial e pós-mutação**

In `dashboard.js`:

1. Remove `CHAVE_ABA_ATIVA`, `obterAbaInicial()`, the `localStorage.setItem` block in `ativarAba`, `obterReceitaMensal()`, `atualizarKpis()` and their calls.
2. Add a `formatarDataHora` function that accepts ISO timestamps and returns `Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" }).format(data)`, falling back to `Não informado`.
3. Declare `let resumoPainel = null`.
4. Add:

```javascript
const carregarResumoBackend = async function () {
    if (!window.GestaoResumo) {
        const feedback = document.getElementById("resumo-feedback");
        if (feedback) {
            feedback.textContent = "Não foi possível inicializar o resumo.";
        }
        return;
    }

    if (!resumoPainel) {
        resumoPainel = window.GestaoResumo.criarPainel({
            document: document,
            Chart: window.Chart,
            buscarJson: buscarJson,
            formatarMoeda: formatarMoeda,
            formatarDataHora: formatarDataHora
        });
    }

    await resumoPainel.carregar();
};
```

5. At the beginning of `iniciarPainel`, call:

```javascript
ativarAba("tab-resumo");
await carregarResumoBackend();
```

6. Remove `ativarAba(obterAbaInicial())`.
7. Call `await carregarResumoBackend()` after successful:
   - `salvarLead`;
   - `excluirLeadById`;
   - `excluirLeadSelecionado`;
   - `salvarNovoCliente`, after optional imported-lead deletion;
   - `excluirClienteSelecionado`.

Keep failures inside the summary controller so a summary refresh never changes the success result of a client or lead mutation.

- [ ] **Step 6: Executar testes focados**

Run:

```bash
node --test src/test/js/resumo.test.js
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw \
  -Dtest=HomeControllerTest,ResumoControllerTest,ResumoRepositoryTest test
```

Expected: all tests PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/resources/templates/home/dashboard.html \
  src/main/resources/static/css/home.css \
  src/main/resources/static/js/dashboard.js \
  src/test/java/com/andreyferraz/gestao/web/HomeControllerTest.java
git commit -m "feat: make summary the dashboard landing screen"
```

---

### Task 8: Validar o incremento completo

**Files:**
- Verify only; modify the owning task's files if a regression is found.

**Interfaces:**
- Verifies: database migration, `GET /resumo`, frontend module, initial navigation and responsive layout.

- [ ] **Step 1: Executar todas as verificações automatizadas**

Run:

```bash
node --test src/test/js/resumo.test.js
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw test
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -DskipTests package
git diff --check
```

Expected:

- 4 JavaScript tests PASS;
- all Java tests PASS;
- Maven package succeeds;
- `git diff --check` produces no output.

- [ ] **Step 2: Fazer a verificação funcional**

Start the application:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw spring-boot:run
```

In the authenticated dashboard verify:

1. `/dashboard` opens on Resumo after a fresh load and after reloading from another tab.
2. An active and an inactive client at the same value produce only one active count in the slice.
3. Two active clients at the same value share one slice.
4. A zero-value active client appears as `R$ 0,00`.
5. Editing a lead does not move it ahead of a newer lead.
6. Creating, editing and deleting clients/leads refreshes the summary.
7. No-data and backend-failure states remain readable.
8. At widths above and below `900px`, the cards use two columns and one column respectively without horizontal overflow.

Stop the application with `Ctrl-C`.

- [ ] **Step 3: Conferir o escopo final**

Run:

```bash
git status --short
git log --oneline -8
```

Expected: only intended feature files are present, all implementation commits are visible, and no generated `target/` content is tracked.
