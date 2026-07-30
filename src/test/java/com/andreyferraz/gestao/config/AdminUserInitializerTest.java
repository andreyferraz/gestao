package com.andreyferraz.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminUserInitializer();
    }

    @Test
    void adminExistente_semSegredoDeveSerPreservado() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        runner("").run();

        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class));
        verifyNoMoreInteractions(jdbcTemplate);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void adminExistente_comNovoSegredoDevePreservarSenhaAtivoERole() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(1);

        runner("novo-segredo").run();

        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class));
        verifyNoMoreInteractions(jdbcTemplate);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void adminAusente_semSegredoDeveFalharComMensagemClara() {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);

        assertThatThrownBy(() -> runner("  ").run())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Defina APP_BOOTSTRAP_ADMIN_PASSWORD para criar o usuario admin.");

        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class));
        verifyNoMoreInteractions(jdbcTemplate);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void adminAusente_comSegredoDeveCriarAdminAtivoComHash() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class))).thenReturn(0);
        when(passwordEncoder.encode("segredo-de-teste")).thenReturn("hash-bcrypt");

        runner("segredo-de-teste").run();

        verify(passwordEncoder).encode("segredo-de-teste");
        verify(jdbcTemplate).update(
                contains("INSERT INTO usuarios"),
                anyString(),
                eq("hash-bcrypt"));
        verify(jdbcTemplate, never()).update(contains("UPDATE usuarios"));
    }

    @Test
    void insertConcorrente_quandoAdminPassaAExistirDeveSerConsideradoSucesso() {
        UncategorizedSQLException collision = sqliteConstraint("username duplicado");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(0, 1);
        when(passwordEncoder.encode("segredo-de-teste")).thenReturn("hash-bcrypt");
        when(jdbcTemplate.update(
                contains("INSERT INTO usuarios"),
                anyString(),
                eq("hash-bcrypt")))
                .thenThrow(collision);

        assertThatCode(() -> runner("segredo-de-teste").run())
                .doesNotThrowAnyException();

        verify(jdbcTemplate, times(2))
                .queryForObject(anyString(), eq(Integer.class));
    }

    @Test
    void insertConcorrente_quandoAdminContinuaAusenteDevePropagarErroOriginal() {
        UncategorizedSQLException collision = sqliteConstraint("id duplicado");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(0, 0);
        when(passwordEncoder.encode("segredo-de-teste")).thenReturn("hash-bcrypt");
        when(jdbcTemplate.update(
                contains("INSERT INTO usuarios"),
                anyString(),
                eq("hash-bcrypt")))
                .thenThrow(collision);

        assertThatThrownBy(() -> runner("segredo-de-teste").run())
                .isSameAs(collision);

        verify(jdbcTemplate, times(2))
                .queryForObject(anyString(), eq(Integer.class));
    }

    @Test
    void insertFalha_quandoReconsultaTambemFalhaDevePreservarErroOriginal()
            throws Exception {
        UncategorizedSQLException insertFailure = sqliteConstraint("constraint");
        DataAccessResourceFailureException verificationFailure =
                new DataAccessResourceFailureException("banco indisponivel");
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class)))
                .thenReturn(0)
                .thenThrow(verificationFailure);
        when(passwordEncoder.encode("segredo-de-teste")).thenReturn("hash-bcrypt");
        when(jdbcTemplate.update(
                contains("INSERT INTO usuarios"),
                anyString(),
                eq("hash-bcrypt")))
                .thenThrow(insertFailure);

        Throwable thrown = catchThrowable(() -> runner("segredo-de-teste").run());

        assertThat(thrown).isSameAs(insertFailure);
        assertThat(thrown.getSuppressed()).containsExactly(verificationFailure);
    }

    private UncategorizedSQLException sqliteConstraint(String message) {
        return new UncategorizedSQLException(
                "criar admin",
                "INSERT INTO usuarios",
                new SQLException(message));
    }

    private CommandLineRunner runner(String password) {
        return initializer.createDefaultAdmin(jdbcTemplate, passwordEncoder, password);
    }
}
