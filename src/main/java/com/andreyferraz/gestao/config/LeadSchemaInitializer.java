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
public class LeadSchemaInitializer {

	private static final String LEAD_TABLE = "lead";

	private final JdbcTemplate jdbcTemplate;

	@PostConstruct
	public void ensureUpdatedAtColumn() {
		if (!isSqlite()) {
			return;
		}

		if (!hasColumn(LEAD_TABLE, "updated_at")) {
			jdbcTemplate.execute("ALTER TABLE lead ADD COLUMN updated_at TEXT");
		}

		jdbcTemplate.execute(
				"UPDATE lead "
						+ "SET updated_at = STRFTIME('%Y-%m-%d %H:%M:%f', 'now') "
						+ "WHERE updated_at IS NULL OR trim(updated_at) = ''");

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
