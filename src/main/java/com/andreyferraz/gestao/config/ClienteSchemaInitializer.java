package com.andreyferraz.gestao.config;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClienteSchemaInitializer {

	private static final String CLIENTE_TABLE = "cliente";

	private final JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void ensureContatoColumn() {
		if (!isSqlite()) {
			return;
		}

		boolean hasContato = hasColumn(CLIENTE_TABLE, "contato");
		boolean hasValorMensal = hasColumn(CLIENTE_TABLE, "valor_mensal");
		boolean hasInformacoesUteis = hasColumn(CLIENTE_TABLE, "informacoes_uteis");

		if (!hasContato) {
			jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN contato TEXT");
		}

		if (!hasInformacoesUteis) {
			jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN informacoes_uteis TEXT");
		}

		if (!hasValorMensal) {
			jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN valor_mensal NUMERIC NOT NULL DEFAULT 0");
		}

		if (!hasColumn(CLIENTE_TABLE, "vendedor_id")) {
			jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN vendedor_id TEXT");
		}

		if (!hasColumn(CLIENTE_TABLE, "created_at")) {
			jdbcTemplate.execute("ALTER TABLE cliente ADD COLUMN created_at TEXT");
		}

		jdbcTemplate.execute("""
				UPDATE cliente
				SET created_at = STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'now')
				WHERE created_at IS NULL OR trim(created_at) = ''
				""");

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

	private boolean hasColumn(String tableName, String columnName) {
		return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
			DatabaseMetaData metaData = connection.getMetaData();
			try (var resultSet = metaData.getColumns(null, null, tableName, columnName)) {
				return resultSet.next();
			}
		}));
	}

	private boolean isSqlite() {
		return Boolean.TRUE.equals(jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
			try {
				String productName = connection.getMetaData().getDatabaseProductName();
				return productName != null && productName.toLowerCase(Locale.ROOT).contains("sqlite");
			} catch (SQLException ex) {
				return false;
			}
		}));
	}

}
