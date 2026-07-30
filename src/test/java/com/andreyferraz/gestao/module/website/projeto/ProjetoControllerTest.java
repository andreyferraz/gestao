package com.andreyferraz.gestao.module.website.projeto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.andreyferraz.gestao.core.exception.GlobalExceptionHandler;
import com.andreyferraz.gestao.core.service.FileUploadService;
import com.andreyferraz.gestao.config.SecurityConfig;
import com.andreyferraz.gestao.module.usuario.Usuario;
import com.andreyferraz.gestao.module.usuario.UsuarioRepository;

@WebMvcTest(ProjetoController.class)
@AutoConfigureMockMvc
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        ProjetoDescricaoSanitizer.class
})
class ProjetoControllerTest {

    private static final String ORIGEM_PRODUCAO = "https://www.andreyferraz.com.br";
    private static final String NOME_IMAGEM_VALIDO =
            "123e4567-e89b-12d3-a456-426614174000.webp";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ProjetoService projetoService;

    @MockitoBean
    private FileUploadService fileUploadService;

    @MockitoBean
    private UsuarioRepository usuarioRepository;

    @TempDir
    Path diretorioTemporario;

    @Test
    void listar_deveRetornarProjetosComUrlAbsolutaBaseadaNoContextoAtual() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetoService.listarProjetos()).thenReturn(List.of(
                projeto(id, "Titulo", "imagem.webp")));

        mockMvc.perform(get("/gestao/api/projetos")
                        .contextPath("/gestao")
                        .with(request -> {
                            request.setScheme("https");
                            request.setSecure(true);
                            request.setServerPort(443);
                            return request;
                        })
                        .header(HttpHeaders.HOST, "api.example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].titulo").value("Titulo"))
                .andExpect(jsonPath("$[0].imagemUrl")
                        .value("https://api.example.com/gestao/api/projetos/imagens/imagem.webp"));
    }

    @Test
    void buscarPorId_deveRetornarProjeto() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetoService.buscarProjetoPorId(id)).thenReturn(
                projeto(id, "Titulo", "imagem.webp"));

        mockMvc.perform(get("/api/projetos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.titulo").value("Titulo"))
                .andExpect(jsonPath("$.imagemUrl")
                        .value("http://localhost/api/projetos/imagens/imagem.webp"));
    }

    @Test
    void buscarPorId_deveSanitizarDescricaoLegadaAntesDeResponder() throws Exception {
        UUID id = UUID.randomUUID();
        Projeto legado = new Projeto(
                id,
                "Titulo",
                "<p onclick=\"alert(1)\">Texto <strong>rico</strong>"
                        + "<script>alert(2)</script></p>",
                "imagem.webp",
                "https://example.com");
        when(projetoService.buscarProjetoPorId(id)).thenReturn(legado);

        mockMvc.perform(get("/api/projetos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao")
                        .value("<p>Texto <strong>rico</strong></p>"));
    }

    @Test
    void buscarPorId_inexistenteDeveRetornarApiError404() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetoService.buscarProjetoPorId(id))
                .thenThrow(new NoSuchElementException("Projeto nao encontrado."));

        mockMvc.perform(get("/api/projetos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Projeto nao encontrado."))
                .andExpect(jsonPath("$.path").value("/api/projetos/" + id));
    }

    @Test
    void criar_deveRetornar201LocationEProjeto() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile imagem = imagem();
        when(projetoService.criarProjeto(any(Projeto.class), eq(imagem)))
                .thenReturn(projeto(id, "Titulo", "imagem.webp"));

        mockMvc.perform(multipart("/api/projetos")
                        .file(imagem)
                        .param("titulo", "Titulo")
                        .param("descricao", "Descricao")
                        .param("link", "https://example.com")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION,
                        "http://localhost/api/projetos/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.imagemUrl")
                        .value("http://localhost/api/projetos/imagens/imagem.webp"));

        verify(projetoService).criarProjeto(
                eq(new Projeto(null, "Titulo", "Descricao", null, "https://example.com")),
                eq(imagem));
    }

    @Test
    void editar_semImagemDeveRetornar200EPreservarImagemNoServico() throws Exception {
        UUID id = UUID.randomUUID();
        when(projetoService.editarProjeto(eq(id), any(Projeto.class), eq(null)))
                .thenReturn(projeto(id, "Atualizado", "anterior.webp"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/projetos/{id}", id)
                        .param("titulo", "Atualizado")
                        .param("descricao", "Descricao atualizada")
                        .param("link", "https://example.com/atualizado")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Atualizado"))
                .andExpect(jsonPath("$.imagemUrl")
                        .value("http://localhost/api/projetos/imagens/anterior.webp"));

        verify(projetoService).editarProjeto(
                eq(id),
                eq(new Projeto(
                        null, "Atualizado", "Descricao atualizada", null,
                        "https://example.com/atualizado")),
                eq(null));
    }

    @Test
    void editar_comImagemDeveRetornar200EEnviarImagemAoServico() throws Exception {
        UUID id = UUID.randomUUID();
        MockMultipartFile imagem = imagem();
        when(projetoService.editarProjeto(eq(id), any(Projeto.class), eq(imagem)))
                .thenReturn(projeto(id, "Atualizado", "nova.webp"));

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/projetos/{id}", id)
                        .file(imagem)
                        .param("titulo", "Atualizado")
                        .param("descricao", "Descricao atualizada")
                        .param("link", "https://example.com/atualizado")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo").value("Atualizado"))
                .andExpect(jsonPath("$.imagemUrl")
                        .value("http://localhost/api/projetos/imagens/nova.webp"));

        verify(projetoService).editarProjeto(
                eq(id),
                eq(new Projeto(
                        null, "Atualizado", "Descricao atualizada", null,
                        "https://example.com/atualizado")),
                eq(imagem));
    }

    @Test
    void deletar_deveRetornar204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/projetos/{id}", id)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(projetoService).deletarProjeto(id);
    }

    @Test
    void criar_comTituloEmBrancoDeveRetornar400SemInvocarServico() throws Exception {
        mockMvc.perform(multipart("/api/projetos")
                        .file(imagem())
                        .param("titulo", "  ")
                        .param("descricao", "Descricao")
                        .param("link", "https://example.com")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Título do projeto é obrigatório."));

        verify(projetoService, never()).criarProjeto(any(), any());
    }

    @Test
    void criar_semImagemDeveRetornar400SemInvocarServico() throws Exception {
        mockMvc.perform(multipart("/api/projetos")
                .param("titulo", "Titulo")
                .param("descricao", "Descricao")
                .param("link", "https://example.com")
                .with(user("admin").roles("ADMIN"))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Imagem é obrigatória."));

        verify(projetoService, never()).criarProjeto(any(), any());
    }

    @Test
    void criar_comLinkInvalidoDeveRetornar400SemInvocarServico() throws Exception {
        mockMvc.perform(multipart("/api/projetos")
                        .file(imagem())
                        .param("titulo", "Titulo")
                        .param("descricao", "Descricao")
                        .param("link", "javascript:alert(1)")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Link do projeto deve usar HTTP ou HTTPS."));

        verify(projetoService, never()).criarProjeto(any(), any());
    }

    @Test
    void obterImagem_deveRetornarWebpComBytesExatos() throws Exception {
        byte[] bytes = { 0x01, 0x23, 0x45, 0x67 };
        Path imagem = Files.write(diretorioTemporario.resolve(NOME_IMAGEM_VALIDO), bytes);
        when(fileUploadService.getCaminhoCompleto(NOME_IMAGEM_VALIDO)).thenReturn(imagem);

        mockMvc.perform(get("/api/projetos/imagens/{arquivo}", NOME_IMAGEM_VALIDO))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void obterImagem_comNomeInvalidoDeveRetornar400() throws Exception {
        when(fileUploadService.getCaminhoCompleto("invalido.webp"))
                .thenThrow(new IllegalArgumentException("Nome de imagem inválido."));

        mockMvc.perform(get("/api/projetos/imagens/invalido.webp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Nome de imagem inválido."));
    }

    @Test
    void obterImagem_ausenteDeveRetornar404() throws Exception {
        Path ausente = diretorioTemporario.resolve("ausente.webp");
        when(fileUploadService.getCaminhoCompleto("ausente.webp")).thenReturn(ausente);

        mockMvc.perform(get("/api/projetos/imagens/ausente.webp"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Imagem não encontrada."));
    }

    @Test
    void listar_comHeadDeveSerPublico() throws Exception {
        mockMvc.perform(head("/api/projetos"))
                .andExpect(status().isOk());
    }

    @Test
    void criar_semAutenticacaoDeveRetornar401() throws Exception {
        mockMvc.perform(novaRequisicaoDeCriacao().with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void editar_semAutenticacaoDeveRetornar401() throws Exception {
        mockMvc.perform(novaRequisicaoDeEdicao(UUID.randomUUID()).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deletar_semAutenticacaoDeveRetornar401() throws Exception {
        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void criar_semAutenticacaoESemCsrfDeveRetornarDesafioBasic() throws Exception {
        mockMvc.perform(novaRequisicaoDeCriacao())
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void editar_semAutenticacaoESemCsrfDeveRetornarDesafioBasic() throws Exception {
        mockMvc.perform(novaRequisicaoDeEdicao(UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void deletar_semAutenticacaoESemCsrfDeveRetornarDesafioBasic() throws Exception {
        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void criar_comUsuarioSemPapelAdminDeveRetornar403() throws Exception {
        mockMvc.perform(novaRequisicaoDeCriacao()
                        .with(user("usuario").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void editar_comUsuarioSemPapelAdminDeveRetornar403() throws Exception {
        mockMvc.perform(novaRequisicaoDeEdicao(UUID.randomUUID())
                        .with(user("usuario").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletar_comUsuarioSemPapelAdminDeveRetornar403() throws Exception {
        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID())
                        .with(user("usuario").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void criar_comAdminSemCsrfDeveRetornar403() throws Exception {
        mockMvc.perform(novaRequisicaoDeCriacao()
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void criar_comUsuarioSemCsrfDeveRetornar403() throws Exception {
        mockMvc.perform(novaRequisicaoDeCriacao()
                        .with(user("usuario").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletar_comHttpBasicDeveAutenticarPeloRepositorio() throws Exception {
        UUID id = UUID.randomUUID();
        String senha = "senha-segura";
        when(usuarioRepository.findByUsername("admin-basic")).thenReturn(Optional.of(
                new Usuario(
                        UUID.randomUUID(),
                        "admin-basic",
                        passwordEncoder.encode(senha),
                        1,
                        "ADMIN")));

        mockMvc.perform(delete("/api/projetos/{id}", id)
                        .with(httpBasic("admin-basic", senha))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(projetoService).deletarProjeto(id);
    }

    @Test
    void deletar_comHttpBasicAdminValidoSemCsrfDeveRetornar403() throws Exception {
        String senha = "senha-admin";
        when(usuarioRepository.findByUsername("admin-sem-csrf")).thenReturn(Optional.of(
                new Usuario(
                        UUID.randomUUID(),
                        "admin-sem-csrf",
                        passwordEncoder.encode(senha),
                        1,
                        "ADMIN")));

        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID())
                        .with(httpBasic("admin-sem-csrf", senha)))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void deletar_comHttpBasicUserValidoSemCsrfDeveRetornar403() throws Exception {
        String senha = "senha-user";
        when(usuarioRepository.findByUsername("user-sem-csrf")).thenReturn(Optional.of(
                new Usuario(
                        UUID.randomUUID(),
                        "user-sem-csrf",
                        passwordEncoder.encode(senha),
                        1,
                        "USER")));

        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID())
                        .with(httpBasic("user-sem-csrf", senha)))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void deletar_comHttpBasicInvalidoSemCsrfDeveRetornarDesafioBasic() throws Exception {
        String senhaCorreta = "senha-correta";
        when(usuarioRepository.findByUsername("admin-senha-incorreta")).thenReturn(Optional.of(
                new Usuario(
                        UUID.randomUUID(),
                        "admin-senha-incorreta",
                        passwordEncoder.encode(senhaCorreta),
                        1,
                        "ADMIN")));

        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID())
                        .with(httpBasic("admin-senha-incorreta", "senha-incorreta")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void deletar_comHttpBasicMalformadoSemCsrfDeveRetornarDesafioBasic() throws Exception {
        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Basic !!!"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void criar_semAutenticacaoNoContextPathDeProducaoDeveRetornarDesafioBasic()
            throws Exception {
        mockMvc.perform(multipart("/gestao/api/projetos")
                        .contextPath("/gestao")
                        .file(imagem())
                        .param("titulo", "Titulo")
                        .param("descricao", "Descricao")
                        .param("link", "https://example.com"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void deletar_comHttpBasicDeUsuarioSemPapelAdminDeveRetornar403() throws Exception {
        String senha = "senha-user";
        when(usuarioRepository.findByUsername("basic-user")).thenReturn(Optional.of(
                new Usuario(
                        UUID.randomUUID(),
                        "basic-user",
                        passwordEncoder.encode(senha),
                        1,
                        "USER")));

        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID())
                        .with(httpBasic("basic-user", senha))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletar_comHttpBasicDeUsuarioInativoDeveRetornar401() throws Exception {
        String senha = "senha-inativa";
        when(usuarioRepository.findByUsername("basic-inativo")).thenReturn(Optional.of(
                new Usuario(
                        UUID.randomUUID(),
                        "basic-inativo",
                        passwordEncoder.encode(senha),
                        0,
                        "ADMIN")));

        mockMvc.perform(delete("/api/projetos/{id}", UUID.randomUUID())
                        .with(httpBasic("basic-inativo", senha))
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE,
                        "Basic realm=\"gestao\""));
    }

    @Test
    void paginaProtegidaHtmlDeveContinuarRedirecionandoParaLogin() throws Exception {
        mockMvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void cors_devePermitirGetDaOrigemDeProducaoSemCredenciais() throws Exception {
        mockMvc.perform(get("/api/projetos")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void cors_devePermitirHeadDaOrigemDeProducaoSemCredenciais() throws Exception {
        mockMvc.perform(head("/api/projetos")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void cors_devePermitirPreflightGetDaOrigemDeProducao() throws Exception {
        mockMvc.perform(options("/api/projetos")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                HttpHeaders.ACCEPT + ", " + HttpHeaders.CONTENT_TYPE))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        "GET,HEAD,OPTIONS"))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS,
                        "Accept, Content-Type"))
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
    }

    @Test
    void cors_devePermitirPreflightHeadDaOrigemDeProducao() throws Exception {
        mockMvc.perform(options("/api/projetos")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.HEAD.name()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_PRODUCAO))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS,
                        "GET,HEAD,OPTIONS"));
    }

    @Test
    void cors_deveProibirPreflightPostMesmoDaOrigemDeProducao() throws Exception {
        mockMvc.perform(options("/api/projetos")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cors_deveProibirOrigemEstrangeira() throws Exception {
        mockMvc.perform(options("/api/projetos")
                        .header(HttpHeaders.ORIGIN, "https://example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name()))
                .andExpect(status().isForbidden());
    }

    @Test
    void cors_deveProibirHeaderNaoPermitido() throws Exception {
        mockMvc.perform(options("/api/projetos")
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.GET.name())
                        .header(
                                HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                                HttpHeaders.AUTHORIZATION))
                .andExpect(status().isForbidden());
    }

    @Test
    void cors_deveProibirMutacaoCrossOriginMesmoParaAdmin() throws Exception {
        mockMvc.perform(novaRequisicaoDeCriacao()
                        .header(HttpHeaders.ORIGIN, ORIGEM_PRODUCAO)
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            novaRequisicaoDeCriacao() {
        return multipart("/api/projetos")
                .file(imagem())
                .param("titulo", "Titulo")
                .param("descricao", "Descricao")
                .param("link", "https://example.com");
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder
            novaRequisicaoDeEdicao(UUID id) {
        return multipart(HttpMethod.PUT, "/api/projetos/{id}", id)
                .param("titulo", "Atualizado")
                .param("descricao", "Descricao atualizada")
                .param("link", "https://example.com/atualizado");
    }

    private Projeto projeto(UUID id, String titulo, String imagemUrl) {
        return new Projeto(id, titulo, "Descricao", imagemUrl, "https://example.com");
    }

    private MockMultipartFile imagem() {
        return new MockMultipartFile(
                "imagem", "imagem.png", "image/png", new byte[] { 0x01, 0x02 });
    }
}
