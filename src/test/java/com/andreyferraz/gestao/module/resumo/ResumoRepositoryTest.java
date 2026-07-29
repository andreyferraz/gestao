package com.andreyferraz.gestao.module.resumo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

class ResumoRepositoryTest {

	private SingleConnectionDataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	private ResumoRepository repository;

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

	@AfterEach
	void tearDown() {
		dataSource.destroy();
	}

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
}
