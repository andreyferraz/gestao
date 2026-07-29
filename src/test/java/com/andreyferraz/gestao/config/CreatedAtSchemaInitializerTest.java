package com.andreyferraz.gestao.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

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
