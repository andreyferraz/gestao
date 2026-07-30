package com.andreyferraz.gestao.config;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.RequiredArgsConstructor;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class UsuarioSchemaInitializer implements ApplicationRunner {

    private static final String MIGRATION_TABLE = "usuarios_role_default_migration";

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void run(ApplicationArguments args) {
        migrateRoleDefault();
    }

    void migrateRoleDefault() {
        if (!isSqlite()) {
            return;
        }

        transactionTemplate.executeWithoutResult(status -> {
            if (!"ADMIN".equalsIgnoreCase(roleDefault())) {
                return;
            }

            rebuildUsuariosTable();
        });
    }

    private String roleDefault() {
        return jdbcTemplate.queryForList("PRAGMA table_info(usuarios)").stream()
                .filter(column -> "role".equalsIgnoreCase(
                        String.valueOf(valueIgnoreCase(column, "name"))))
                .map(column -> valueIgnoreCase(column, "dflt_value"))
                .map(this::normalizeDefault)
                .findFirst()
                .orElse("");
    }

    private Object valueIgnoreCase(Map<String, Object> row, String key) {
        return row.entrySet().stream()
                .filter(entry -> key.equalsIgnoreCase(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private String normalizeDefault(Object defaultValue) {
        if (defaultValue == null) {
            return "";
        }

        String normalized = defaultValue.toString().trim();
        while (normalized.startsWith("(") && normalized.endsWith(")")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        if (normalized.length() >= 2
                && ((normalized.startsWith("'") && normalized.endsWith("'"))
                || (normalized.startsWith("\"") && normalized.endsWith("\"")))) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        return normalized;
    }

    private void rebuildUsuariosTable() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS " + MIGRATION_TABLE);
        jdbcTemplate.execute("""
                CREATE TABLE usuarios_role_default_migration (
                    id TEXT PRIMARY KEY,
                    username TEXT NOT NULL UNIQUE,
                    senha TEXT NOT NULL,
                    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
                    role TEXT NOT NULL DEFAULT 'USER'
                )
                """);
        jdbcTemplate.execute("""
                INSERT INTO usuarios_role_default_migration
                    (id, username, senha, ativo, role)
                SELECT id, username, senha, ativo, role
                FROM usuarios
                """);
        jdbcTemplate.execute("DROP TABLE usuarios");
        jdbcTemplate.execute(
                "ALTER TABLE " + MIGRATION_TABLE + " RENAME TO usuarios");
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
