# Projeto CRUD REST API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Complete the Projeto CRUD and expose public read APIs for `https://www.andreyferraz.com.br` while restricting mutations to administrators.

**Architecture:** Keep `Projeto` as the Spring Data JDBC aggregate, add explicit UUID insert/update queries, and make `ProjetoService` own validation plus compensated file lifecycle. A multipart REST controller maps request/response DTOs, `FileUploadService` safely stores and serves WebP files, and Spring Security combines public reads, admin writes, form login, HTTP Basic, CSRF, and restricted CORS.

**Tech Stack:** Java 17, Spring Boot 4.0.5, Spring Data JDBC, Spring MVC, Spring Security, Bean Validation, SQLite/H2, JUnit 6, Mockito, MockMvc, ImageIO WebP.

## Global Constraints

- Public browser consumer origin: exactly `https://www.andreyferraz.com.br`.
- `GET` project and image endpoints are public; project mutations require role `ADMIN`.
- Keep the existing form login and enable HTTP Basic as an alternative.
- Keep CSRF enabled; cross-origin mutation methods are not allowed.
- Creation requires an image; update images are optional.
- Persist only the generated WebP filename; expose a full image URL in API responses.
- Do not build the administration HTML form in this change.
- Preserve unrelated user changes and stage only files belonging to each task.

---

### Task 1: Prove and Fix UUID Persistence

**Files:**
- Create: `src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepositoryTest.java`
- Modify: `src/test/resources/application.properties`
- Modify: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepository.java`
- Modify: `src/main/resources/schema.sql`

**Interfaces:**
- Produces: `void inserir(UUID id, String titulo, String descricao, String imagemUrl, String link)`
- Produces: `void atualizar(UUID id, String titulo, String descricao, String imagemUrl, String link)`
- Produces: database columns `titulo`, `descricao`, `imagem_url`, and `link` as `NOT NULL` for new databases.

- [ ] **Step 1: Write the failing repository integration tests**

```java
@DataJdbcTest
@Import(DbConfig.class)
class ProjetoRepositoryTest {

    @Autowired
    private ProjetoRepository repository;

    @Test
    void inserir_devePersistirUuidEProjetoCompleto() {
        UUID id = UUID.randomUUID();

        repository.inserir(id, "Site", "Descricao", "imagem.webp", "https://example.com");

        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals(id, salvo.getId());
        assertEquals("Site", salvo.getTitulo());
        assertEquals("imagem.webp", salvo.getImagemUrl());
    }

    @Test
    void atualizar_deveAlterarCamposDoProjeto() {
        UUID id = UUID.randomUUID();
        repository.inserir(id, "Antigo", "Antes", "antiga.webp", "https://old.example");

        repository.atualizar(id, "Novo", "Depois", "nova.webp", "https://new.example");

        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals("Novo", salvo.getTitulo());
        assertEquals("nova.webp", salvo.getImagemUrl());
    }
}
```

- [ ] **Step 2: Run the repository tests and verify RED**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -Dtest=ProjetoRepositoryTest test
```

Expected: test compilation fails because `inserir` and `atualizar` do not exist.

- [ ] **Step 3: Configure lowercase H2 identifiers and add explicit queries**

Change the test datasource URL to:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE
```

Add to `ProjetoRepository`:

```java
@Modifying
@Query("""
        INSERT INTO projeto (id, titulo, descricao, imagem_url, link)
        VALUES (:id, :titulo, :descricao, :imagemUrl, :link)
        """)
void inserir(UUID id, String titulo, String descricao, String imagemUrl, String link);

@Modifying
@Query("""
        UPDATE projeto
        SET titulo = :titulo,
            descricao = :descricao,
            imagem_url = :imagemUrl,
            link = :link
        WHERE id = :id
        """)
void atualizar(UUID id, String titulo, String descricao, String imagemUrl, String link);
```

Change the Projeto table for new databases to:

```sql
CREATE TABLE IF NOT EXISTS projeto (
    id TEXT PRIMARY KEY,
    titulo TEXT NOT NULL,
    descricao TEXT NOT NULL,
    imagem_url TEXT NOT NULL,
    link TEXT NOT NULL
);
```

- [ ] **Step 4: Run the repository tests and verify GREEN**

Run the focused command from Step 2. Expected: both tests pass.

- [ ] **Step 5: Commit the persistence slice**

```bash
git add src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepositoryTest.java src/test/resources/application.properties src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRepository.java src/main/resources/schema.sql
git commit -m "fix: persist projetos with assigned UUIDs"
```

### Task 2: Correct Creation and Validation

**Files:**
- Create: `src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoServiceTest.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoService.java`

**Interfaces:**
- Produces: `Projeto criarProjeto(Projeto projeto, MultipartFile imagem)`
- Produces: `List<Projeto> listarProjetos()`
- Produces: validation that rejects blank strings and non-HTTP(S) links.

- [ ] **Step 1: Write failing service tests for UUID creation and compensation**

```java
@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock ProjetoRepository repository;
    @Mock FileUploadService fileUploadService;
    @Mock MultipartFile imagem;
    @InjectMocks ProjetoService service;

    @Test
    void criarProjeto_deveGerarUuidInserirERetornarProjeto() {
        Projeto projeto = projetoValido();
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");

        Projeto criado = service.criarProjeto(projeto, imagem);

        assertNotNull(criado.getId());
        assertEquals("nova.webp", criado.getImagemUrl());
        verify(repository).inserir(
                eq(criado.getId()), eq("Titulo"), eq("Descricao"),
                eq("nova.webp"), eq("https://example.com"));
    }

    @Test
    void criarProjeto_quandoInsercaoFalhar_deveRemoverImagemNova() {
        Projeto projeto = projetoValido();
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        doThrow(new DataAccessResourceFailureException("db"))
                .when(repository)
                .inserir(any(), any(), any(), any(), any());

        assertThrows(DataAccessResourceFailureException.class,
                () -> service.criarProjeto(projeto, imagem));

        verify(fileUploadService).removerImagem("nova.webp");
    }

    @Test
    void criarProjeto_quandoTituloForBranco_deveRejeitarAntesDoUpload() {
        Projeto projeto = projetoValido();
        projeto.setTitulo("   ");

        assertThrows(IllegalArgumentException.class,
                () -> service.criarProjeto(projeto, imagem));

        verifyNoInteractions(fileUploadService, repository);
    }

    @Test
    void criarProjeto_quandoLinkNaoForHttp_deveRejeitar() {
        Projeto projeto = projetoValido();
        projeto.setLink("javascript:alert(1)");

        assertThrows(IllegalArgumentException.class,
                () -> service.criarProjeto(projeto, imagem));
    }
}
```

- [ ] **Step 2: Run the service tests and verify RED**

Run:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -Dtest=ProjetoServiceTest test
```

Expected: compilation or assertion failure because creation returns `void`,
uses `save`, and accepts blank strings.

- [ ] **Step 3: Implement minimal creation, validation, and listing**

Implement:

```java
@Transactional
public Projeto criarProjeto(Projeto projeto, MultipartFile imagem) {
    validarProjeto(projeto);
    validarImagemObrigatoria(imagem);

    String nomeImagem = fileUploadService.salvarImagem(imagem);
    projeto.setId(UUID.randomUUID());
    projeto.setImagemUrl(nomeImagem);
    try {
        projetoRepository.inserir(
                projeto.getId(), projeto.getTitulo(), projeto.getDescricao(),
                projeto.getImagemUrl(), projeto.getLink());
        return projeto;
    } catch (RuntimeException ex) {
        removerImagemSemOcultarErro(nomeImagem, ex);
        throw ex;
    }
}

@Transactional(readOnly = true)
public List<Projeto> listarProjetos() {
    return StreamSupport.stream(projetoRepository.findAll().spliterator(), false)
            .toList();
}
```

`validarProjeto` uses `String.isBlank()` and parses `link` with `URI`; it
accepts only absolute `http` and `https` schemes. Cleanup exceptions are added
as suppressed exceptions to the original persistence failure.

- [ ] **Step 4: Run service and repository tests and verify GREEN**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -Dtest=ProjetoServiceTest,ProjetoRepositoryTest test
```

Expected: all focused tests pass.

- [ ] **Step 5: Commit the creation slice**

```bash
git add src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoServiceTest.java src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoService.java
git commit -m "fix: validate and create projetos safely"
```

### Task 3: Correct Update and Delete Image Lifecycle

**Files:**
- Modify: `src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoServiceTest.java`
- Modify: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoService.java`

**Interfaces:**
- Produces: `Projeto editarProjeto(UUID id, Projeto projetoAtualizado, MultipartFile imagem)`
- Produces: `void deletarProjeto(UUID id)`
- Produces: `Projeto buscarProjetoPorId(UUID id)` with `NoSuchElementException`.

- [ ] **Step 1: Add failing tests for image preservation and replacement**

```java
@Test
void editarProjeto_semNovaImagem_devePreservarImagemExistente() {
    UUID id = UUID.randomUUID();
    Projeto existente = new Projeto(id, "Antigo", "Antes", "antiga.webp", "https://old.example");
    Projeto alterado = projetoValido();
    when(repository.findById(id)).thenReturn(Optional.of(existente));

    Projeto resultado = service.editarProjeto(id, alterado, null);

    assertEquals("antiga.webp", resultado.getImagemUrl());
    verify(repository).atualizar(id, "Titulo", "Descricao", "antiga.webp", "https://example.com");
    verify(fileUploadService, never()).salvarImagem(any());
}

@Test
void editarProjeto_comNovaImagem_devePersistirNovaERemoverAntiga() {
    UUID id = UUID.randomUUID();
    Projeto existente = new Projeto(id, "Antigo", "Antes", "antiga.webp", "https://old.example");
    Projeto alterado = projetoValido();
    when(repository.findById(id)).thenReturn(Optional.of(existente));
    when(imagem.isEmpty()).thenReturn(false);
    when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");

    Projeto resultado = service.editarProjeto(id, alterado, imagem);

    assertEquals("nova.webp", resultado.getImagemUrl());
    verify(repository).atualizar(id, "Titulo", "Descricao", "nova.webp", "https://example.com");
    verify(fileUploadService).removerImagem("antiga.webp");
}

@Test
void editarProjeto_quandoPersistenciaFalhar_deveRemoverNovaEPreservarAntiga() {
    UUID id = UUID.randomUUID();
    Projeto existente = new Projeto(id, "Antigo", "Antes", "antiga.webp", "https://old.example");
    when(repository.findById(id)).thenReturn(Optional.of(existente));
    when(imagem.isEmpty()).thenReturn(false);
    when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
    doThrow(new DataAccessResourceFailureException("db"))
            .when(repository)
            .atualizar(any(), any(), any(), any(), any());

    assertThrows(DataAccessResourceFailureException.class,
            () -> service.editarProjeto(id, projetoValido(), imagem));

    verify(fileUploadService).removerImagem("nova.webp");
    verify(fileUploadService, never()).removerImagem("antiga.webp");
}
```

- [ ] **Step 2: Add failing tests for deletion and 404**

```java
@Test
void deletarProjeto_deveExcluirRegistroEImagem() {
    UUID id = UUID.randomUUID();
    Projeto existente = new Projeto(id, "Titulo", "Descricao", "imagem.webp", "https://example.com");
    when(repository.findById(id)).thenReturn(Optional.of(existente));

    service.deletarProjeto(id);

    verify(repository).deleteById(id);
    verify(fileUploadService).removerImagem("imagem.webp");
}

@Test
void buscarProjetoPorId_quandoNaoExistir_deveLancarNoSuchElementException() {
    UUID id = UUID.randomUUID();
    when(repository.findById(id)).thenReturn(Optional.empty());

    assertThrows(NoSuchElementException.class,
            () -> service.buscarProjetoPorId(id));
}
```

- [ ] **Step 3: Run the focused tests and verify RED**

Run the Task 2 focused service command. Expected: failures from the old
`imagemUrl` requirement, missing cleanup, and `IllegalArgumentException` for
missing projects.

- [ ] **Step 4: Implement update, delete, and not-found semantics**

The update method validates the ID and fields, loads the existing project,
preserves its filename when `imagem` is null/empty, and applies the
replacement compensation rules. It calls the explicit repository `atualizar`
method and returns the updated entity with the path-controlled filename.

The delete method loads the project, calls `deleteById`, and removes its image
inside the transaction. All missing project paths throw
`NoSuchElementException("Projeto não encontrado com o ID: " + id)`.

- [ ] **Step 5: Run focused tests and verify GREEN**

Run the Task 2 focused service and repository command. Expected: all pass.

- [ ] **Step 6: Commit the lifecycle slice**

```bash
git add src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoServiceTest.java src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoService.java
git commit -m "fix: manage projeto image lifecycle"
```

### Task 4: Harden WebP File Storage

**Files:**
- Create: `src/test/java/com/andreyferraz/gestao/core/service/FileUploadServiceTest.java`
- Modify: `src/main/java/com/andreyferraz/gestao/core/service/FileUploadService.java`
- Modify: `pom.xml`
- Modify: `.gitignore`

**Interfaces:**
- Produces: `Path getCaminhoCompleto(String nomeArquivo)` restricted to generated WebP names.
- Produces: `boolean arquivoExiste(String nomeArquivo)` using safe resolution.
- Produces: a runtime `ImageIO` WebP writer on supported deployment platforms.

- [ ] **Step 1: Write failing traversal and invalid-content tests**

```java
class FileUploadServiceTest {

    @TempDir Path tempDir;
    private FileUploadService service;

    @BeforeEach
    void setUp() {
        service = new FileUploadService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "webpQuality", 0.75f);
    }

    @Test
    void getCaminhoCompleto_quandoNomeEscaparDoDiretorio_deveRejeitar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getCaminhoCompleto("../../senha.txt"));
    }

    @Test
    void salvarImagem_quandoConteudoNaoForImagem_deveRejeitarSemCriarArquivo() {
        MockMultipartFile arquivo = new MockMultipartFile(
                "imagem", "arquivo.png", "image/png", "nao e imagem".getBytes(UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> service.salvarImagem(arquivo));
        assertTrue(Files.list(tempDir).findAny().isEmpty());
    }
}
```

- [ ] **Step 2: Write a failing valid WebP conversion test**

Build and verify the image with:

```java
@Test
void salvarImagem_quandoPngValido_deveConverterParaWebp() throws IOException {
    BufferedImage original = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    ByteArrayOutputStream png = new ByteArrayOutputStream();
    assertTrue(ImageIO.write(original, "png", png));
    MockMultipartFile arquivo = new MockMultipartFile(
            "imagem", "imagem.png", "image/png", png.toByteArray());

    String nome = service.salvarImagem(arquivo);

    assertTrue(nome.matches("[0-9a-f-]{36}\\.webp"));
    Path salvo = service.getCaminhoCompleto(nome);
    assertTrue(Files.isRegularFile(salvo));
    assertNotNull(ImageIO.read(salvo.toFile()));
}
```

- [ ] **Step 3: Run file tests and verify RED**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -Dtest=FileUploadServiceTest test
```

Expected: traversal is accepted and invalid-image errors use the wrong
exception/cleanup behavior.

- [ ] **Step 4: Add the WebP runtime and safe file resolution**

Add:

```xml
<dependency>
    <groupId>com.github.usefulness</groupId>
    <artifactId>webp-imageio</artifactId>
    <version>0.11.0</version>
    <scope>runtime</scope>
</dependency>
```

In `FileUploadService`, require generated names to match:

```java
private static final Pattern NOME_IMAGEM =
        Pattern.compile("^[0-9a-fA-F-]{36}\\.webp$");
```

Resolve every public filename through:

```java
private Path resolverArquivoSeguro(String nomeArquivo) {
    if (nomeArquivo == null || !NOME_IMAGEM.matcher(nomeArquivo).matches()) {
        throw new IllegalArgumentException("Nome de imagem inválido.");
    }
    Path raiz = Paths.get(uploadDir).toAbsolutePath().normalize();
    Path candidato = raiz.resolve(nomeArquivo).normalize();
    if (!candidato.startsWith(raiz)) {
        throw new IllegalArgumentException("Caminho de imagem inválido.");
    }
    return candidato;
}
```

Validate multipart emptiness and decodability before conversion. Delete the
partial destination in every conversion failure path. Add a bounded timeout
to the `cwebp` process and destroy it forcibly on timeout.

Add `/uploads/` to `.gitignore`.

- [ ] **Step 5: Run file tests and verify GREEN**

Run the focused command from Step 3. Expected: all file tests pass without
requiring a separately installed `cwebp`.

- [ ] **Step 6: Run all focused Projeto tests**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -Dtest=FileUploadServiceTest,ProjetoServiceTest,ProjetoRepositoryTest test
```

Expected: all focused tests pass.

- [ ] **Step 7: Commit the storage slice**

```bash
git add pom.xml .gitignore src/main/java/com/andreyferraz/gestao/core/service/FileUploadService.java src/test/java/com/andreyferraz/gestao/core/service/FileUploadServiceTest.java
git commit -m "fix: secure projeto image storage"
```

### Task 5: Add the REST Controller and DTOs

**Files:**
- Create: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRequest.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoResponse.java`
- Create: `src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoController.java`
- Create: `src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoControllerTest.java`

**Interfaces:**
- Produces: the six endpoints defined in the approved design.
- Produces: `ProjetoRequest` with `titulo`, `descricao`, and `link`.
- Produces: `ProjetoResponse(UUID id, String titulo, String descricao, String imagemUrl, String link)`.

- [ ] **Step 1: Write failing public-read controller tests**

Use `@WebMvcTest(ProjetoController.class)`, mock `ProjetoService`, and import
the exception handler. Assert:

```java
mockMvc.perform(get("/api/projetos"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].id").value(id.toString()))
        .andExpect(jsonPath("$[0].imagemUrl")
                .value("http://localhost/api/projetos/imagens/imagem.webp"));

mockMvc.perform(get("/api/projetos/{id}", id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.titulo").value("Titulo"));
```

Add a missing-project test that makes the service throw
`NoSuchElementException` and expects the existing `ApiError` JSON with `404`.

- [ ] **Step 2: Write failing multipart mutation tests**

Use `.param("titulo", "Titulo")`, `.param("descricao", "Descricao")`,
`.param("link", "https://example.com")`, and a `MockMultipartFile` named
`imagem`. Assert:

- POST returns `201`, `Location`, and the response body.
- PUT works without an image.
- PUT works with an image.
- DELETE returns `204`.
- Blank title returns `400` without invoking the service.

- [ ] **Step 3: Write a failing image response test**

Create a temporary `.webp` file, mock
`fileUploadService.getCaminhoCompleto("imagem.webp")`, and assert
`GET /api/projetos/imagens/imagem.webp` returns `200`,
`Content-Type: image/webp`, and the exact bytes.

- [ ] **Step 4: Run controller tests and verify RED**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -Dtest=ProjetoControllerTest test
```

Expected: compilation fails because the controller and DTOs do not exist.

- [ ] **Step 5: Implement DTOs and controller**

`ProjetoRequest` is a mutable class with `@NotBlank` fields and a `@Pattern`
for absolute HTTP(S) URLs:

```java
@NotBlank(message = "Link do projeto é obrigatório.")
@Pattern(
        regexp = "(?i)^https?://.+",
        message = "Link do projeto deve usar HTTP ou HTTPS.")
private String link;
```

`ProjetoController` uses `@RestController`, `@RequestMapping("/api/projetos")`,
constructor injection, `@Valid @ModelAttribute ProjetoRequest`, and
`@RequestParam` for multipart images. Use
`ServletUriComponentsBuilder.fromCurrentContextPath()` to build full image
URLs and the POST `Location`.

The image endpoint returns `PathResource` only when the safe path is a regular
file; otherwise it throws `NoSuchElementException`.

- [ ] **Step 6: Run controller tests and verify GREEN**

Run the Task 5 focused command. Expected: all controller behavior tests pass.

- [ ] **Step 7: Commit the controller slice**

```bash
git add src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoRequest.java src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoResponse.java src/main/java/com/andreyferraz/gestao/module/website/projeto/ProjetoController.java src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoControllerTest.java
git commit -m "feat: expose projeto REST API"
```

### Task 6: Apply Authentication, Authorization, and CORS

**Files:**
- Modify: `src/main/java/com/andreyferraz/gestao/config/SecurityConfig.java`
- Modify: `src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoControllerTest.java`

**Interfaces:**
- Produces: public project `GET` routes.
- Produces: role-`ADMIN` mutation routes.
- Produces: CORS property `app.cors.allowed-origins` with production default.

- [ ] **Step 1: Add failing security tests**

Import `SecurityConfig` into the MVC test and mock `UsuarioRepository`. Add:

```java
mockMvc.perform(get("/api/projetos"))
        .andExpect(status().isOk());

mockMvc.perform(multipart("/api/projetos")
                .file(imagem)
                .param("titulo", "Titulo")
                .param("descricao", "Descricao")
                .param("link", "https://example.com")
                .with(csrf()))
        .andExpect(status().isUnauthorized());

mockMvc.perform(multipart("/api/projetos")
                .file(imagem)
                .param("titulo", "Titulo")
                .param("descricao", "Descricao")
                .param("link", "https://example.com")
                .with(user("usuario").roles("USER"))
                .with(csrf()))
        .andExpect(status().isForbidden());
```

Add a successful admin request with `.with(user("admin").roles("ADMIN"))`
and `.with(csrf())`.

- [ ] **Step 2: Add failing CORS tests**

```java
mockMvc.perform(options("/api/projetos")
                .header(ORIGIN, "https://www.andreyferraz.com.br")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
        .andExpect(status().isOk())
        .andExpect(header().string(
                ACCESS_CONTROL_ALLOW_ORIGIN,
                "https://www.andreyferraz.com.br"));

mockMvc.perform(options("/api/projetos")
                .header(ORIGIN, "https://www.andreyferraz.com.br")
                .header(ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isForbidden());
```

Add a foreign-origin preflight and expect `403`.

- [ ] **Step 3: Run controller tests and verify RED**

Run the Task 5 focused controller command. Expected: existing security
requires authentication for GET and has no configured CORS policy.

- [ ] **Step 4: Implement security and CORS**

In `SecurityConfig`:

```java
http
    .cors(Customizer.withDefaults())
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(HttpMethod.GET, "/api/projetos", "/api/projetos/**").permitAll()
        .requestMatchers(HttpMethod.POST, "/api/projetos").hasRole("ADMIN")
        .requestMatchers(HttpMethod.PUT, "/api/projetos/**").hasRole("ADMIN")
        .requestMatchers(HttpMethod.DELETE, "/api/projetos/**").hasRole("ADMIN")
        .requestMatchers("/login", "/error", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
        .anyRequest().authenticated())
    .httpBasic(Customizer.withDefaults())
    .formLogin(form -> form
        .loginPage("/login")
        .defaultSuccessUrl("/dashboard", true)
        .permitAll())
    .logout(logout -> logout
        .logoutSuccessUrl("/login?logout")
        .permitAll());
```

Provide a `CorsConfigurationSource` bean that reads:

```java
@Value("${app.cors.allowed-origins:https://www.andreyferraz.com.br}")
List<String> allowedOrigins
```

Apply it to `/api/projetos/**` with allowed methods `GET`, `HEAD`, `OPTIONS`
and allowed headers `Accept`, `Content-Type`. Do not allow credentials because
public reads require none.

- [ ] **Step 5: Run controller/security tests and verify GREEN**

Run the focused controller command. Expected: public reads, admin mutations,
and exact-origin CORS tests all pass.

- [ ] **Step 6: Commit the security slice**

```bash
git add src/main/java/com/andreyferraz/gestao/config/SecurityConfig.java src/test/java/com/andreyferraz/gestao/module/website/projeto/ProjetoControllerTest.java
git commit -m "feat: secure projeto API mutations"
```

### Task 7: Final Integration and Verification

**Files:**
- Modify if required by verified failures: only files already listed in Tasks 1-6.

**Interfaces:**
- Consumes: all CRUD, file, controller, security, and CORS behavior from earlier tasks.
- Produces: a build verified on JDK 17 with no Projeto regressions.

- [ ] **Step 1: Run all focused tests**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw -Dtest=FileUploadServiceTest,ProjetoServiceTest,ProjetoRepositoryTest,ProjetoControllerTest test
```

Expected: all focused tests pass with zero failures and zero errors.

- [ ] **Step 2: Run the complete Maven suite**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw test
```

Expected: every test passes with `BUILD SUCCESS`.

- [ ] **Step 3: Run package verification**

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-17.jdk/Contents/Home ./mvnw clean package
```

Expected: the application packages successfully and the WebP runtime is
included in the dependency graph.

- [ ] **Step 4: Inspect the final diff and repository status**

```bash
git diff --check
git status --short
git log --oneline -8
```

Expected: no whitespace errors; only intentional user or implementation
changes remain.

- [ ] **Step 5: Request a code review**

Dispatch a read-only reviewer against the implementation commits and address
all Critical and Important findings before completion.

- [ ] **Step 6: Re-run verification after review fixes**

Repeat Steps 1-3 and report the exact test counts and build result.
