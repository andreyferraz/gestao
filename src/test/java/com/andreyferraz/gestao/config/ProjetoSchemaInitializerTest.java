package com.andreyferraz.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class ProjetoSchemaInitializerTest {

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
    void sqliteSemTabelaProjeto_deveCriarEstruturaCompleta() {
        new ProjetoSchemaInitializer(jdbcTemplate).ensureProjetoTable();

        List<Map<String, Object>> columns =
                jdbcTemplate.queryForList("PRAGMA table_info(projeto)");

        assertThat(columns)
                .extracting(column -> String.valueOf(column.get("name")))
                .containsExactly("id", "titulo", "descricao", "imagem_url", "link");
        assertThat(columns.subList(1, 5))
                .allSatisfy(column ->
                        assertThat(((Number) column.get("notnull")).intValue())
                                .isEqualTo(1));
    }

    @Test
    void sqliteComTabelaProjeto_devePreservarDadosAoExecutarNovamente() {
        ProjetoSchemaInitializer initializer =
                new ProjetoSchemaInitializer(jdbcTemplate);
        initializer.ensureProjetoTable();
        jdbcTemplate.update("""
                INSERT INTO projeto (id, titulo, descricao, imagem_url, link)
                VALUES (?, ?, ?, ?, ?)
                """,
                "projeto-id",
                "Projeto existente",
                "Descrição",
                "https://andreyferraz.com.br/imagem.png",
                "https://andreyferraz.com.br");

        initializer.ensureProjetoTable();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT titulo FROM projeto WHERE id = ?",
                String.class,
                "projeto-id"))
                .isEqualTo("Projeto existente");
    }

    @Test
    void bancoNaoSqlite_deveSerIgnorado() {
        dataSource.destroy();
        dataSource = new SingleConnectionDataSource(
                "jdbc:h2:mem:projeto-schema-" + UUID.randomUUID(), true);
        jdbcTemplate = new JdbcTemplate(dataSource);

        new ProjetoSchemaInitializer(jdbcTemplate).ensureProjetoTable();

        assertThat(jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = 'PUBLIC'
                  AND TABLE_NAME = 'PROJETO'
                """, Integer.class))
                .isZero();
    }
}
