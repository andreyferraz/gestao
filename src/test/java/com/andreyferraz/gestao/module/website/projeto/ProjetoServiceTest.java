package com.andreyferraz.gestao.module.website.projeto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.andreyferraz.gestao.core.service.FileUploadService;

@ExtendWith(MockitoExtension.class)
class ProjetoServiceTest {

    @Mock
    private ProjetoRepository repository;

    @Mock
    private FileUploadService fileUploadService;

    @Mock
    private MultipartFile imagem;

    private ProjetoService service;

    @BeforeEach
    void setUp() {
        service = new ProjetoService(
                repository,
                fileUploadService,
                new ProjetoDescricaoSanitizer());
    }

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
    void criarProjeto_deveSanitizarDescricaoAntesDePersistir() {
        Projeto projeto = projetoValido();
        projeto.setDescricao(
                "<h2>Projeto</h2><p onclick=\"alert(1)\">Texto <strong>rico</strong></p>");
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");

        Projeto criado = service.criarProjeto(projeto, imagem);

        assertEquals(
                "<h2>Projeto</h2><p>Texto <strong>rico</strong></p>",
                criado.getDescricao());
        verify(repository).inserir(
                eq(criado.getId()),
                eq("Titulo"),
                eq("<h2>Projeto</h2><p>Texto <strong>rico</strong></p>"),
                eq("nova.webp"),
                eq("https://example.com"));
    }

    @Test
    void criarProjeto_quandoInsercaoFalhar_deveRemoverImagemNova() {
        Projeto projeto = projetoValido();
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        DataAccessResourceFailureException falhaOriginal = new DataAccessResourceFailureException("db");
        doThrow(falhaOriginal)
                .when(repository)
                .inserir(any(), any(), any(), any(), any());

        DataAccessResourceFailureException lancada = assertThrows(DataAccessResourceFailureException.class,
                () -> service.criarProjeto(projeto, imagem));

        assertSame(falhaOriginal, lancada);
        verify(fileUploadService).removerImagem("nova.webp");
    }

    @Test
    void criarProjeto_quandoLimpezaFalhar_deveSuprimirFalhaSemOcultarErroOriginal() {
        Projeto projeto = projetoValido();
        DataAccessResourceFailureException falhaOriginal = new DataAccessResourceFailureException("db");
        IllegalStateException falhaRemocao = new IllegalStateException("cleanup");
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        doThrow(falhaOriginal).when(repository).inserir(any(), any(), any(), any(), any());
        doThrow(falhaRemocao).when(fileUploadService).removerImagem("nova.webp");

        DataAccessResourceFailureException lancada = assertThrows(DataAccessResourceFailureException.class,
                () -> service.criarProjeto(projeto, imagem));

        assertSame(falhaOriginal, lancada);
        assertEquals(1, lancada.getSuppressed().length);
        assertSame(falhaRemocao, lancada.getSuppressed()[0]);
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
    void criarProjeto_quandoLinkNaoForHttp_deveRejeitarAntesDoUpload() {
        Projeto projeto = projetoValido();
        projeto.setLink("javascript:alert(1)");

        assertThrows(IllegalArgumentException.class,
                () -> service.criarProjeto(projeto, imagem));

        verifyNoInteractions(fileUploadService, repository);
    }

    @Test
    void criarProjeto_quandoLinkForUriHttpOpaca_deveRejeitarAntesDoUpload() {
        Projeto projeto = projetoValido();
        projeto.setLink("http:arquivo");

        assertThrows(IllegalArgumentException.class,
                () -> service.criarProjeto(projeto, imagem));

        verifyNoInteractions(fileUploadService, repository);
    }

    @Test
    void criarProjeto_quandoLinkHttpNaoTiverHost_deveRejeitarAntesDoUpload() {
        Projeto projeto = projetoValido();
        projeto.setLink("https:/caminho");

        assertThrows(IllegalArgumentException.class,
                () -> service.criarProjeto(projeto, imagem));

        verifyNoInteractions(fileUploadService, repository);
    }

    @Test
    void criarProjeto_quandoTransacaoForRevertida_deveRemoverImagemNova() {
        Projeto projeto = projetoValido();
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.criarProjeto(projeto, imagem);

            List<TransactionSynchronization> sincronizacoes = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, sincronizacoes.size());
            sincronizacoes.forEach(sincronizacao ->
                    sincronizacao.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(fileUploadService).removerImagem("nova.webp");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void listarProjetos_deveUsarOrdemDeAtualizacaoRecenteDoRepositorio() {
        Projeto recente = projetoValido();
        Projeto antigo = new Projeto(null, "Outro", "Outra descricao", "outra.webp", "https://other.example.com");
        when(repository.findAllOrderByAtualizacaoRecente())
                .thenReturn(List.of(recente, antigo));

        List<Projeto> projetos = service.listarProjetos();

        assertEquals(List.of(recente, antigo), projetos);
        verify(repository).findAllOrderByAtualizacaoRecente();
    }

    @Test
    void editarProjeto_semNovaImagem_devePreservarImagemExistente() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "antiga.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);

        Projeto resultado = service.editarProjeto(id, projetoValido(), null);

        assertEquals(id, resultado.getId());
        assertEquals("antiga.webp", resultado.getImagemUrl());
        verify(repository).atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "antiga.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com");
        verify(fileUploadService, never()).salvarImagem(any());
        verify(fileUploadService, never()).removerImagem(any());
    }

    @Test
    void editarProjeto_deveSanitizarDescricaoAntesDeAtualizar() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        Projeto alterado = projetoValido();
        alterado.setDescricao("<p style=\"color:red\">Nova <em>descrição</em></p>");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo",
                "<p>Nova <em>descrição</em></p>",
                "antiga.webp",
                "https://example.com",
                "Antigo",
                "Antes",
                "antiga.webp",
                "https://old.example.com"))
                .thenReturn(1);

        Projeto resultado = service.editarProjeto(id, alterado, null);

        assertEquals("<p>Nova <em>descrição</em></p>", resultado.getDescricao());
    }

    @Test
    void editarProjeto_comImagemVazia_devePreservarImagemExistente() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(true);
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "antiga.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);

        Projeto resultado = service.editarProjeto(id, projetoValido(), imagem);

        assertEquals("antiga.webp", resultado.getImagemUrl());
        verify(repository).atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "antiga.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com");
        verify(fileUploadService, never()).salvarImagem(any());
        verify(fileUploadService, never()).removerImagem(any());
    }

    @Test
    void editarProjeto_comNovaImagem_deveAtualizarAntesDeRemoverAntiga() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);

        Projeto resultado = service.editarProjeto(id, projetoValido(), imagem);

        assertEquals("nova.webp", resultado.getImagemUrl());
        InOrder ordem = inOrder(repository, fileUploadService);
        ordem.verify(fileUploadService).salvarImagem(imagem);
        ordem.verify(repository).atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com");
        ordem.verify(fileUploadService).removerImagem("antiga.webp");
        verify(fileUploadService, never()).removerImagem("nova.webp");
    }

    @Test
    void editarProjeto_quandoAtualizacaoFalhar_deveRemoverNovaEPreservarAntiga() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        DataAccessResourceFailureException falhaOriginal = new DataAccessResourceFailureException("db");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        doThrow(falhaOriginal).when(repository)
                .atualizarSeEstadoAtual(
                        any(), any(), any(), any(), any(), any(), any(), any(), any());

        DataAccessResourceFailureException lancada = assertThrows(DataAccessResourceFailureException.class,
                () -> service.editarProjeto(id, projetoValido(), imagem));

        assertSame(falhaOriginal, lancada);
        verify(fileUploadService).removerImagem("nova.webp");
        verify(fileUploadService, never()).removerImagem("antiga.webp");
    }

    @Test
    void editarProjeto_quandoTransacaoReverter_deveRemoverNovaEPreservarAntiga() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.editarProjeto(id, projetoValido(), imagem);

            verify(fileUploadService, never()).removerImagem(any());
            List<TransactionSynchronization> sincronizacoes =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, sincronizacoes.size());
            sincronizacoes.forEach(sincronizacao ->
                    sincronizacao.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(fileUploadService).removerImagem("nova.webp");
            verify(fileUploadService, never()).removerImagem("antiga.webp");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void editarProjeto_quandoTransacaoConfirmar_deveRemoverAntigaEPreservarNova() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.editarProjeto(id, projetoValido(), imagem);

            verify(fileUploadService, never()).removerImagem(any());
            List<TransactionSynchronization> sincronizacoes =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, sincronizacoes.size());
            sincronizacoes.forEach(TransactionSynchronization::afterCommit);
            sincronizacoes.forEach(sincronizacao ->
                    sincronizacao.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));

            verify(fileUploadService).removerImagem("antiga.webp");
            verify(fileUploadService, never()).removerImagem("nova.webp");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void editarProjeto_quandoLinkForInvalido_deveRejeitarAntesDeBuscarProjeto() {
        Projeto alterado = projetoValido();
        alterado.setLink("https:/sem-host");

        assertThrows(IllegalArgumentException.class,
                () -> service.editarProjeto(UUID.randomUUID(), alterado, null));

        verifyNoInteractions(repository, fileUploadService);
    }

    @Test
    void editarProjeto_comNovaImagem_quandoMetadadosMudaram_deveCompensarNovaImagem() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(0);

        assertThrows(OptimisticLockingFailureException.class,
                () -> service.editarProjeto(id, projetoValido(), imagem));

        verify(fileUploadService).removerImagem("nova.webp");
        verify(fileUploadService, never()).removerImagem("antiga.webp");
    }

    @Test
    void editarProjeto_semNovaImagem_quandoMetadadosMudaram_devePreservarArquivos() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "antiga.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(0);

        assertThrows(OptimisticLockingFailureException.class,
                () -> service.editarProjeto(id, projetoValido(), null));

        verifyNoInteractions(fileUploadService);
    }

    @Test
    void editarProjeto_quandoNaoExistir_deveLancarNoSuchElementException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.editarProjeto(id, projetoValido(), null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void editarProjeto_semNovaImagem_quandoImagemLegadaForAusente_deveExigirReparo(String imagemLegada) {
        UUID id = UUID.randomUUID();
        Projeto existente = new Projeto(
                id, "Antigo", "Antes", imagemLegada, "https://old.example.com");
        when(repository.findById(id)).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class,
                () -> service.editarProjeto(id, projetoValido(), null));

        verify(repository, never()).atualizarSeEstadoAtual(
                any(), any(), any(), any(), any(), any(), any(), any(), any());
        verifyNoInteractions(fileUploadService);
    }

    @Test
    void editarProjeto_comNovaImagem_quandoSnapshotLegadoTiverNulos_deveRepararSemRemoverNomeAusente() {
        UUID id = UUID.randomUUID();
        Projeto existente = new Projeto(id, null, null, null, null);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                null, null, null, null))
                .thenReturn(1);

        Projeto resultado = service.editarProjeto(id, projetoValido(), imagem);

        assertEquals("nova.webp", resultado.getImagemUrl());
        verify(fileUploadService).salvarImagem(imagem);
        verify(fileUploadService, never()).removerImagem(any());
    }

    @Test
    void editarProjeto_semSincronizacao_quandoLimpezaAntigaFalhar_devePropagarSemRemoverNova() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        IllegalStateException falhaLimpeza = new IllegalStateException("cleanup");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        doThrow(falhaLimpeza).when(fileUploadService).removerImagem("antiga.webp");

        IllegalStateException lancada = assertThrows(IllegalStateException.class,
                () -> service.editarProjeto(id, projetoValido(), imagem));

        assertSame(falhaLimpeza, lancada);
        verify(fileUploadService, never()).removerImagem("nova.webp");
    }

    @Test
    void editarProjeto_aposCommit_quandoLimpezaAntigaFalhar_devePropagarSemRemoverNova() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        IllegalStateException falhaLimpeza = new IllegalStateException("cleanup");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(imagem.isEmpty()).thenReturn(false);
        when(fileUploadService.salvarImagem(imagem)).thenReturn("nova.webp");
        when(repository.atualizarSeEstadoAtual(
                id,
                "Titulo", "Descricao", "nova.webp", "https://example.com",
                "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        doThrow(falhaLimpeza).when(fileUploadService).removerImagem("antiga.webp");
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.editarProjeto(id, projetoValido(), imagem);
            TransactionSynchronization sincronizacao =
                    TransactionSynchronizationManager.getSynchronizations().get(0);

            IllegalStateException lancada = assertThrows(
                    IllegalStateException.class, sincronizacao::afterCommit);

            assertSame(falhaLimpeza, lancada);
            verify(fileUploadService, never()).removerImagem("nova.webp");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletarProjeto_deveExcluirRegistroAntesDeRemoverImagem() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.deletarSeEstadoAtual(
                id, "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);

        service.deletarProjeto(id);

        InOrder ordem = inOrder(repository, fileUploadService);
        ordem.verify(repository).deletarSeEstadoAtual(
                id, "Antigo", "Antes", "antiga.webp", "https://old.example.com");
        ordem.verify(fileUploadService).removerImagem("antiga.webp");
    }

    @Test
    void deletarProjeto_quandoExclusaoFalhar_devePreservarImagem() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        doThrow(new DataAccessResourceFailureException("db"))
                .when(repository).deletarSeEstadoAtual(
                        id, "Antigo", "Antes", "antiga.webp", "https://old.example.com");

        assertThrows(DataAccessResourceFailureException.class,
                () -> service.deletarProjeto(id));

        verify(fileUploadService, never()).removerImagem(any());
    }

    @Test
    void deletarProjeto_quandoMetadadosMudaram_devePreservarImagem() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.deletarSeEstadoAtual(
                id, "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(0);

        assertThrows(OptimisticLockingFailureException.class,
                () -> service.deletarProjeto(id));

        verifyNoInteractions(fileUploadService);
    }

    @Test
    void deletarProjeto_quandoTransacaoReverter_devePreservarImagem() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.deletarSeEstadoAtual(
                id, "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deletarProjeto(id);

            verify(fileUploadService, never()).removerImagem(any());
            List<TransactionSynchronization> sincronizacoes =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, sincronizacoes.size());
            sincronizacoes.forEach(sincronizacao ->
                    sincronizacao.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(fileUploadService, never()).removerImagem(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletarProjeto_quandoTransacaoConfirmar_deveRemoverImagem() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.deletarSeEstadoAtual(
                id, "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deletarProjeto(id);

            verify(fileUploadService, never()).removerImagem(any());
            List<TransactionSynchronization> sincronizacoes =
                    TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, sincronizacoes.size());
            sincronizacoes.forEach(TransactionSynchronization::afterCommit);

            verify(fileUploadService).removerImagem("antiga.webp");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deletarProjeto_quandoNaoExistir_deveLancarNoSuchElementException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.deletarProjeto(id));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    void deletarProjeto_quandoImagemLegadaForAusente_deveExcluirSemRemoverArquivo(String imagemLegada) {
        UUID id = UUID.randomUUID();
        Projeto existente = new Projeto(id, null, null, imagemLegada, null);
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.deletarSeEstadoAtual(id, null, null, imagemLegada, null))
                .thenReturn(1);

        service.deletarProjeto(id);

        verifyNoInteractions(fileUploadService);
    }

    @Test
    void deletarProjeto_semSincronizacao_quandoLimpezaFalhar_devePropagar() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        IllegalStateException falhaLimpeza = new IllegalStateException("cleanup");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.deletarSeEstadoAtual(
                id, "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        doThrow(falhaLimpeza).when(fileUploadService).removerImagem("antiga.webp");

        IllegalStateException lancada = assertThrows(
                IllegalStateException.class, () -> service.deletarProjeto(id));

        assertSame(falhaLimpeza, lancada);
    }

    @Test
    void deletarProjeto_aposCommit_quandoLimpezaFalhar_devePropagar() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        IllegalStateException falhaLimpeza = new IllegalStateException("cleanup");
        when(repository.findById(id)).thenReturn(Optional.of(existente));
        when(repository.deletarSeEstadoAtual(
                id, "Antigo", "Antes", "antiga.webp", "https://old.example.com"))
                .thenReturn(1);
        doThrow(falhaLimpeza).when(fileUploadService).removerImagem("antiga.webp");
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.deletarProjeto(id);
            TransactionSynchronization sincronizacao =
                    TransactionSynchronizationManager.getSynchronizations().get(0);

            IllegalStateException lancada = assertThrows(
                    IllegalStateException.class, sincronizacao::afterCommit);

            assertSame(falhaLimpeza, lancada);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void buscarProjetoPorId_deveRetornarProjetoExistente() {
        UUID id = UUID.randomUUID();
        Projeto existente = projetoExistente(id);
        when(repository.findById(id)).thenReturn(Optional.of(existente));

        Projeto resultado = service.buscarProjetoPorId(id);

        assertSame(existente, resultado);
    }

    @Test
    void buscarProjetoPorId_quandoNaoExistir_deveLancarNoSuchElementException() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class,
                () -> service.buscarProjetoPorId(id));
    }

    @Test
    void buscarProjetoPorId_quandoIdForNulo_deveRejeitarAntesDoRepositorio() {
        assertThrows(IllegalArgumentException.class,
                () -> service.buscarProjetoPorId(null));

        verifyNoInteractions(repository);
    }

    private Projeto projetoValido() {
        return new Projeto(null, "Titulo", "Descricao", null, "https://example.com");
    }

    private Projeto projetoExistente(UUID id) {
        return new Projeto(id, "Antigo", "Antes", "antiga.webp", "https://old.example.com");
    }
}
