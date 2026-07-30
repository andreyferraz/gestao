package com.andreyferraz.gestao.module.website.projeto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jdbc.test.autoconfigure.DataJdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.andreyferraz.gestao.config.DbConfig;

@DataJdbcTest
@Import(DbConfig.class)
class ProjetoRepositoryTest {

    @Autowired
    private ProjetoRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void atualizarSeEstadoAtual_quandoEstadoForAtual_deveAtualizarUmaLinha() {
        UUID id = UUID.randomUUID();
        repository.inserir(id, "Antigo", "Antes", "antiga.webp", "https://old.example");

        int linhasAtualizadas = repository.atualizarSeEstadoAtual(
                id,
                "Novo", "Depois", "nova.webp", "https://new.example",
                "Antigo", "Antes", "antiga.webp", "https://old.example");

        assertEquals(1, linhasAtualizadas);
        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals("Novo", salvo.getTitulo());
        assertEquals("nova.webp", salvo.getImagemUrl());
    }

    @Test
    void atualizarSeEstadoAtual_quandoImagemForObsoleta_deveManterProjeto() {
        UUID id = UUID.randomUUID();
        repository.inserir(id, "Atual", "Descricao atual", "atual.webp", "https://current.example");

        int linhasAtualizadas = repository.atualizarSeEstadoAtual(
                id,
                "Novo", "Nova descricao", "nova.webp", "https://new.example",
                "Atual", "Descricao atual", "antiga.webp", "https://current.example");

        assertEquals(0, linhasAtualizadas);
        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals("Atual", salvo.getTitulo());
        assertEquals("atual.webp", salvo.getImagemUrl());
    }

    @Test
    void atualizarSeEstadoAtual_quandoSomenteMetadadosMudaram_deveManterProjeto() {
        UUID id = UUID.randomUUID();
        repository.inserir(id, "Concorrente", "Antes", "antiga.webp", "https://old.example");

        int linhasAtualizadas = repository.atualizarSeEstadoAtual(
                id,
                "Novo", "Depois", "nova.webp", "https://new.example",
                "Antigo", "Antes", "antiga.webp", "https://old.example");

        assertEquals(0, linhasAtualizadas);
        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals("Concorrente", salvo.getTitulo());
        assertEquals("antiga.webp", salvo.getImagemUrl());
    }

    @Test
    void deletarSeEstadoAtual_quandoEstadoForAtual_deveExcluirUmaLinha() {
        UUID id = UUID.randomUUID();
        repository.inserir(id, "Titulo", "Descricao", "imagem.webp", "https://example.com");

        int linhasExcluidas = repository.deletarSeEstadoAtual(
                id, "Titulo", "Descricao", "imagem.webp", "https://example.com");

        assertEquals(1, linhasExcluidas);
        assertFalse(repository.existsById(id));
    }

    @Test
    void deletarSeEstadoAtual_quandoImagemForObsoleta_devePreservarProjeto() {
        UUID id = UUID.randomUUID();
        repository.inserir(id, "Titulo", "Descricao", "atual.webp", "https://example.com");

        int linhasExcluidas = repository.deletarSeEstadoAtual(
                id, "Titulo", "Descricao", "antiga.webp", "https://example.com");

        assertEquals(0, linhasExcluidas);
        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals("atual.webp", salvo.getImagemUrl());
    }

    @Test
    void deletarSeEstadoAtual_quandoSomenteMetadadosMudaram_devePreservarProjeto() {
        UUID id = UUID.randomUUID();
        repository.inserir(id, "Concorrente", "Descricao", "imagem.webp", "https://example.com");

        int linhasExcluidas = repository.deletarSeEstadoAtual(
                id, "Titulo", "Descricao", "imagem.webp", "https://example.com");

        assertEquals(0, linhasExcluidas);
        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals("Concorrente", salvo.getTitulo());
        assertEquals("imagem.webp", salvo.getImagemUrl());
    }

    @Test
    void atualizarSeEstadoAtual_quandoSnapshotTiverMetadadosNulos_deveAtualizar() {
        permitirNulosLegados();
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO projeto (id, titulo, descricao, imagem_url, link) VALUES (?, ?, ?, ?, ?)",
                id, null, null, "legada.webp", null);

        int linhasAtualizadas = repository.atualizarSeEstadoAtual(
                id,
                "Novo", "Depois", "nova.webp", "https://new.example",
                null, null, "legada.webp", null);

        assertEquals(1, linhasAtualizadas);
        Projeto salvo = repository.findById(id).orElseThrow();
        assertEquals("Novo", salvo.getTitulo());
        assertEquals("nova.webp", salvo.getImagemUrl());
    }

    @Test
    void deletarSeEstadoAtual_quandoSnapshotTiverMetadadosEImagemNulos_deveExcluir() {
        permitirNulosLegados();
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO projeto (id, titulo, descricao, imagem_url, link) VALUES (?, ?, ?, ?, ?)",
                id, null, null, null, null);

        int linhasExcluidas = repository.deletarSeEstadoAtual(id, null, null, null, null);

        assertEquals(1, linhasExcluidas);
        assertFalse(repository.existsById(id));
    }

    private void permitirNulosLegados() {
        permitirNulo("titulo");
        permitirNulo("descricao");
        permitirNulo("imagem_url");
        permitirNulo("link");
    }

    private void permitirNulo(String coluna) {
        Boolean jaPermiteNulo = jdbcTemplate.queryForObject("""
                SELECT CASE WHEN IS_NULLABLE = 'YES' THEN TRUE ELSE FALSE END
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE UPPER(TABLE_SCHEMA) = 'PUBLIC'
                  AND UPPER(TABLE_NAME) = 'PROJETO'
                  AND UPPER(COLUMN_NAME) = UPPER(?)
                """, Boolean.class, coluna);
        if (!Boolean.TRUE.equals(jaPermiteNulo)) {
            jdbcTemplate.execute("ALTER TABLE projeto ALTER COLUMN " + coluna + " DROP NOT NULL");
        }
    }
}
