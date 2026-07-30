package com.andreyferraz.gestao.config;

import java.sql.SQLException;
import java.util.Locale;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjetoSchemaInitializer {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    void ensureProjetoTable() {
        if (!isSqlite()) {
            return;
        }

        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS projeto (
                    id TEXT PRIMARY KEY,
                    titulo TEXT NOT NULL,
                    descricao TEXT NOT NULL,
                    imagem_url TEXT NOT NULL,
                    link TEXT NOT NULL,
                    updated_at TEXT NOT NULL
                )
                """);

        if (!hasColumn("updated_at")) {
            jdbcTemplate.execute(
                    "ALTER TABLE projeto ADD COLUMN updated_at TEXT");
        }
        jdbcTemplate.update("""
                UPDATE projeto
                SET updated_at = STRFTIME('%Y-%m-%dT%H:%M:%fZ', 'now')
                WHERE updated_at IS NULL OR trim(updated_at) = ''
                """);
    }

    private boolean hasColumn(String columnName) {
        return jdbcTemplate.queryForList("PRAGMA table_info(projeto)")
                .stream()
                .map(column -> String.valueOf(column.get("name")))
                .anyMatch(columnName::equalsIgnoreCase);
    }

    private boolean isSqlite() {
        return Boolean.TRUE.equals(jdbcTemplate.execute(
                (ConnectionCallback<Boolean>) connection -> {
                    try {
                        String productName =
                                connection.getMetaData().getDatabaseProductName();
                        return productName != null
                                && productName.toLowerCase(Locale.ROOT).contains("sqlite");
                    } catch (SQLException ex) {
                        return false;
                    }
                }));
    }
}
