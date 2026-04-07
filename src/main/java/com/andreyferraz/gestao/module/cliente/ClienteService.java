package com.andreyferraz.gestao.module.cliente;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

	private final ClienteRepository clienteRepository;
	private final JdbcTemplate jdbcTemplate;

	public Cliente criar(Cliente cliente) {
		if (cliente.getId() == null) {
			cliente.setId(UUID.randomUUID());
		}

		jdbcTemplate.update(
				"INSERT INTO cliente (id, nome, contato, dominio_aplicacao, data_vencimento_dominio, valor_mensal, ativo) VALUES (?, ?, ?, ?, ?, ?, ?)",
				cliente.getId().toString(),
				cliente.getNome(),
				cliente.getContato(),
				cliente.getDominioAplicacao(),
				cliente.getDataVencimentoDominio() != null ? cliente.getDataVencimentoDominio().toString() : null,
				cliente.getValorMensal() != null ? cliente.getValorMensal() : BigDecimal.ZERO,
				cliente.getAtivo() != null && cliente.getAtivo() == 1 ? 1 : 0);

		return buscarPorId(cliente.getId());
	}

	public List<Cliente> listarTodos() {
		return StreamSupport.stream(clienteRepository.findAll().spliterator(), false)
				.toList();
	}

	public List<ClienteDashboardDto> listarResumoDashboard() {
		return listarTodos().stream()
				.map(cliente -> {
					double valorMensal = cliente.getValorMensal() != null
							? cliente.getValorMensal().doubleValue()
							: 0.0;

					return new ClienteDashboardDto(
							cliente.getId(),
							cliente.getNome(),
							cliente.getContato(),
							cliente.getDominioAplicacao(),
							cliente.getDataVencimentoDominio(),
							cliente.getAtivo() != null && cliente.getAtivo() == 1,
							valorMensal);
				})
				.toList();
	}

	public Cliente buscarPorId(UUID id) {
		return clienteRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado para o id: " + id));
	}

	public Cliente atualizar(UUID id, Cliente clienteAtualizado) {
		buscarPorId(id);

		jdbcTemplate.update(
				"UPDATE cliente SET nome = ?, contato = ?, dominio_aplicacao = ?, data_vencimento_dominio = ?, valor_mensal = ?, ativo = ? WHERE id = ?",
				clienteAtualizado.getNome(),
				clienteAtualizado.getContato(),
				clienteAtualizado.getDominioAplicacao(),
				clienteAtualizado.getDataVencimentoDominio() != null ? clienteAtualizado.getDataVencimentoDominio().toString() : null,
				clienteAtualizado.getValorMensal() != null ? clienteAtualizado.getValorMensal() : BigDecimal.ZERO,
				clienteAtualizado.getAtivo() != null && clienteAtualizado.getAtivo() == 1 ? 1 : 0,
				id.toString());

		return buscarPorId(id);
	}

	public void remover(UUID id) {
		if (!clienteRepository.existsById(id)) {
			throw new NoSuchElementException("Cliente nao encontrado para o id: " + id);
		}
		clienteRepository.deleteById(id);
	}

}
