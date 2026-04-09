package com.andreyferraz.gestao.config;

import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClienteSchemaInitializer {

	private final JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void ensureContatoColumn() {
		List<Map<String, Object>> columns = jdbcTemplate.queryForList("PRAGMA table_info(cliente)");
		boolean hasContato = columns.stream()
				.anyMatch(column -> "contato".equalsIgnoreCase(String.valueOf(column.get("name"))));
		boolean hasValorMensal = columns.stream()
				.anyMatch(column -> "valor_mensal".equalsIgnoreCase(String.valueOf(column.get("name"))));

		if (!hasContato) {
			jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN contato TEXT");
		}

		if (!hasValorMensal) {
			jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN valor_mensal NUMERIC NOT NULL DEFAULT 0");
		}

		// Convert legacy epoch timestamps (seconds/milliseconds) to ISO date.
		jdbcTemplate.execute(
				"UPDATE cliente "
						+ "SET data_vencimento_dominio = "
						+ "CASE "
						+ "WHEN CAST(data_vencimento_dominio AS INTEGER) > 9999999999 THEN date(CAST(data_vencimento_dominio AS INTEGER) / 1000, 'unixepoch') "
						+ "ELSE date(CAST(data_vencimento_dominio AS INTEGER), 'unixepoch') "
						+ "END "
						+ "WHERE data_vencimento_dominio IS NOT NULL "
						+ "AND trim(data_vencimento_dominio) NOT GLOB '*[^0-9]*' "
						+ "AND length(trim(data_vencimento_dominio)) IN (10, 13)");

		// Convert legacy Brazilian date format (dd/MM/yyyy) to ISO yyyy-MM-dd.
		jdbcTemplate.execute(
				"UPDATE cliente "
						+ "SET data_vencimento_dominio = substr(trim(data_vencimento_dominio), 7, 4) || '-' || substr(trim(data_vencimento_dominio), 4, 2) || '-' || substr(trim(data_vencimento_dominio), 1, 2) "
						+ "WHERE data_vencimento_dominio IS NOT NULL "
						+ "AND trim(data_vencimento_dominio) LIKE '__/__/____'");

		// Prevent mapping failures by nulling out unsupported date formats.
		jdbcTemplate.execute(
				"UPDATE cliente "
						+ "SET data_vencimento_dominio = NULL "
						+ "WHERE data_vencimento_dominio IS NOT NULL "
						+ "AND trim(data_vencimento_dominio) <> '' "
						+ "AND trim(data_vencimento_dominio) NOT GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'");

		// Normalize legacy boolean-like values to SQLite integer convention 0/1.
		jdbcTemplate.execute(
				"UPDATE cliente "
						+ "SET valor_mensal = COALESCE(valor_mensal, 0)");

		jdbcTemplate.execute(
				"UPDATE cliente "
						+ "SET ativo = CASE "
						+ "WHEN lower(CAST(ativo AS TEXT)) IN ('true', '1') THEN 1 "
						+ "ELSE 0 "
						+ "END "
						+ "WHERE ativo IS NOT NULL");
	}

}
