package com.andreyferraz.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.support.TransactionTemplate;

class UsuarioSchemaInitializerTest {

    @TempDir
    Path tempDir;

    private SingleConnectionDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        useDataSource("jdbc:sqlite:" + tempDir.resolve("usuarios.db"));
    }

    @AfterEach
    void tearDown() {
        dataSource.destroy();
    }

    @Test
    void sqliteLegado_deveMigrarDefaultPreservarDadosEManterRestricoes() {
        createUsuariosTable("ADMIN");
        jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo, role) VALUES (?, ?, ?, ?, ?)",
                "admin-id", "admin", "admin-hash", 1, "ADMIN");
        jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo, role) VALUES (?, ?, ?, ?, ?)",
                "user-id", "usuario", "user-hash", 0, "USER");

        initializer().migrateRoleDefault();

        assertThat(roleDefault()).isEqualTo("'USER'");
        assertThat(user("admin"))
                .containsEntry("id", "admin-id")
                .containsEntry("senha", "admin-hash")
                .containsEntry("ativo", 1)
                .containsEntry("role", "ADMIN");
        assertThat(user("usuario"))
                .containsEntry("id", "user-id")
                .containsEntry("senha", "user-hash")
                .containsEntry("ativo", 0)
                .containsEntry("role", "USER");

        int schemaVersionAfterFirstRun = schemaVersion();
        initializer().migrateRoleDefault();
        assertThat(schemaVersion()).isEqualTo(schemaVersionAfterFirstRun);

        jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo) VALUES (?, ?, ?, ?)",
                "default-id", "novo", "novo-hash", 1);
        assertThat(user("novo")).containsEntry("role", "USER");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo) VALUES (?, ?, ?, ?)",
                "outro-id", "novo", "hash", 1))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo) VALUES (?, ?, ?, ?)",
                "default-id", "outro", "hash", 1))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo) VALUES (?, ?, ?, ?)",
                "ativo-invalido", "invalido", "hash", 2))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void sqliteAtual_devePermanecerIntocado() {
        createUsuariosTable("USER");
        jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo, role) VALUES (?, ?, ?, ?, ?)",
                "admin-id", "admin", "hash", 1, "ADMIN");
        int schemaVersionBefore = schemaVersion();

        initializer().migrateRoleDefault();

        assertThat(schemaVersion()).isEqualTo(schemaVersionBefore);
        assertThat(user("admin"))
                .containsEntry("id", "admin-id")
                .containsEntry("role", "ADMIN");
    }

    @Test
    void bancoNaoSqlite_deveSerIgnorado() {
        dataSource.destroy();
        useDataSource("jdbc:h2:mem:usuario-schema-" + UUID.randomUUID());
        createUsuariosTable("ADMIN");

        assertThatCode(() -> initializer().migrateRoleDefault())
                .doesNotThrowAnyException();

        jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo) VALUES (?, ?, ?, ?)",
                "default-id", "novo", "hash", 1);
        assertThat(user("novo")).containsEntry("role", "ADMIN");
    }

    @Test
    void sqlite_deveTraduzirColisaoUniqueComoUncategorizedSQLException() {
        createUsuariosTable("USER");
        jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo) VALUES (?, ?, ?, ?)",
                "primeiro-id", "duplicado", "hash", 1);

        Throwable thrown = catchThrowable(() -> jdbcTemplate.update(
                "INSERT INTO usuarios (id, username, senha, ativo) VALUES (?, ?, ?, ?)",
                "segundo-id", "duplicado", "hash", 1));

        assertThat(thrown)
                .isInstanceOf(UncategorizedSQLException.class)
                .isNotInstanceOf(DuplicateKeyException.class);
    }

    private void useDataSource(String url) {
        dataSource = new SingleConnectionDataSource(url, true);
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private UsuarioSchemaInitializer initializer() {
        DataSourceTransactionManager transactionManager =
                new DataSourceTransactionManager(dataSource);
        return new UsuarioSchemaInitializer(
                jdbcTemplate,
                new TransactionTemplate(transactionManager));
    }

    private void createUsuariosTable(String defaultRole) {
        jdbcTemplate.execute("""
                CREATE TABLE usuarios (
                    id TEXT PRIMARY KEY,
                    username TEXT NOT NULL UNIQUE,
                    senha TEXT NOT NULL,
                    ativo INTEGER NOT NULL DEFAULT 1 CHECK (ativo IN (0, 1)),
                    role TEXT NOT NULL DEFAULT '%s'
                )
                """.formatted(defaultRole));
    }

    private String roleDefault() {
        return jdbcTemplate.queryForList("PRAGMA table_info(usuarios)").stream()
                .filter(column -> "role".equalsIgnoreCase(String.valueOf(column.get("name"))))
                .map(column -> String.valueOf(column.get("dflt_value")))
                .findFirst()
                .orElseThrow();
    }

    private int schemaVersion() {
        return jdbcTemplate.queryForObject("PRAGMA schema_version", Integer.class);
    }

    private Map<String, Object> user(String username) {
        return jdbcTemplate.queryForMap(
                "SELECT id, senha, ativo, role FROM usuarios WHERE username = ?",
                username);
    }
}
