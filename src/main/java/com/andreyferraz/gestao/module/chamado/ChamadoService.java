package com.andreyferraz.gestao.module.chamado;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChamadoService {

	private final JdbcTemplate jdbcTemplate;

	public Chamado criar(Chamado chamado) {
		validarChamado(chamado);
		if (chamado.getId() == null) {
			chamado.setId(UUID.randomUUID());
		}
		if (chamado.getStatus() == null) {
			chamado.setStatus(Chamado.Status.ABERTO);
		}

		jdbcTemplate.update(
				"INSERT INTO chamado (id, cliente_id, descricao_problema, status) VALUES (?, ?, ?, ?)",
				chamado.getId().toString(),
				chamado.getClienteId().toString(),
				chamado.getDescricaoProblema(),
				chamado.getStatus().name());

		return buscarPorId(chamado.getId());
	}

	public List<Chamado> listarTodos() {
		return jdbcTemplate.query(
				"SELECT id, cliente_id, descricao_problema, status FROM chamado ORDER BY rowid DESC",
				(rs, rowNum) -> mapRow(rs));
	}

	public Chamado buscarPorId(UUID id) {
		List<Chamado> chamados = jdbcTemplate.query(
				"SELECT id, cliente_id, descricao_problema, status FROM chamado WHERE id = ?",
				(rs, rowNum) -> mapRow(rs),
				id.toString());
		if (chamados.isEmpty()) {
			throw new NoSuchElementException("Chamado nao encontrado para o id: " + id);
		}
		return chamados.get(0);
	}

	public Chamado atualizarStatus(UUID id, Chamado.Status status) {
		if (status == null) {
			throw new IllegalArgumentException("Status do chamado e obrigatorio.");
		}

		int linhasAfetadas = jdbcTemplate.update(
				"UPDATE chamado SET status = ? WHERE id = ?",
				status.name(),
				id.toString());

		if (linhasAfetadas == 0) {
			throw new NoSuchElementException("Chamado nao encontrado para o id: " + id);
		}

		return buscarPorId(id);
	}

	private void validarChamado(Chamado chamado) {
		if (chamado.getClienteId() == null) {
			throw new IllegalArgumentException("Cliente do chamado e obrigatorio.");
		}
		if (chamado.getDescricaoProblema() == null || chamado.getDescricaoProblema().isBlank()) {
			throw new IllegalArgumentException("Descricao do problema e obrigatoria.");
		}
	}

	private Chamado mapRow(ResultSet rs) throws SQLException {
		return new Chamado(
				UUID.fromString(rs.getString("id")),
				UUID.fromString(rs.getString("cliente_id")),
				rs.getString("descricao_problema"),
				Chamado.Status.valueOf(rs.getString("status")));
	}
}
