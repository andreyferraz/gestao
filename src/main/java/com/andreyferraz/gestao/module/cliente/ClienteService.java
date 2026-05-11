package com.andreyferraz.gestao.module.cliente;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andreyferraz.gestao.module.vendedor.VendedorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ClienteService {

	private final ClienteRepository clienteRepository;
	private final VendedorRepository vendedorRepository;

	@Transactional
	public Cliente criar(Cliente cliente) {
		if (cliente.getId() == null) {
			cliente.setId(UUID.randomUUID());
		}
		normalizarClienteParaPersistencia(cliente);

		clienteRepository.inserir(
				cliente.getId(),
				cliente.getNome(),
				cliente.getContato(),
				cliente.getDominioAplicacao(),
				cliente.getDataVencimentoDominio(),
				cliente.getValorMensal() != null ? cliente.getValorMensal() : BigDecimal.ZERO,
				cliente.getAtivo() != null && cliente.getAtivo() == 1 ? 1 : 0,
				validarVendedor(cliente.getVendedorId()));

		return cliente;
	}

	@Transactional(readOnly = true)
	public List<Cliente> listarTodos() {
		return StreamSupport.stream(clienteRepository.findAll().spliterator(), false)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ClienteDashboardDto> listarResumoDashboard() {
		Map<UUID, String> vendedoresPorId = carregarNomesVendedores();

		return StreamSupport.stream(clienteRepository.findAll().spliterator(), false)
				.map(cliente -> {
					double valorMensal = cliente.getValorMensal() != null
							? cliente.getValorMensal().doubleValue()
							: 0.0;
					String vendedorNome = cliente.getVendedorId() != null
							? vendedoresPorId.getOrDefault(cliente.getVendedorId(), "Vendedor nao encontrado")
							: null;

					return new ClienteDashboardDto(
							cliente.getId(),
							cliente.getNome(),
							cliente.getContato(),
							cliente.getDominioAplicacao(),
							cliente.getDataVencimentoDominio(),
							cliente.getVendedorId(),
							vendedorNome,
							cliente.getAtivo() != null && cliente.getAtivo() == 1,
							valorMensal);
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public Cliente buscarPorId(UUID id) {
		return clienteRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado para o id: " + id));
	}

	@Transactional
	public Cliente atualizar(UUID id, Cliente clienteAtualizado) {
		clienteRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Cliente nao encontrado para o id: " + id));
		clienteAtualizado.setId(id);
		normalizarClienteParaPersistencia(clienteAtualizado);

		clienteRepository.atualizar(
				id,
				clienteAtualizado.getNome(),
				clienteAtualizado.getContato(),
				clienteAtualizado.getDominioAplicacao(),
				clienteAtualizado.getDataVencimentoDominio(),
				clienteAtualizado.getValorMensal() != null ? clienteAtualizado.getValorMensal() : BigDecimal.ZERO,
				clienteAtualizado.getAtivo() != null && clienteAtualizado.getAtivo() == 1 ? 1 : 0,
				validarVendedor(clienteAtualizado.getVendedorId()));

		return clienteAtualizado;
	}

	@Transactional
	public void remover(UUID id) {
		if (!clienteRepository.existsById(id)) {
			throw new NoSuchElementException("Cliente nao encontrado para o id: " + id);
		}
		clienteRepository.deleteById(id);
	}

	private void normalizarClienteParaPersistencia(Cliente cliente) {
		cliente.setValorMensal(cliente.getValorMensal() != null ? cliente.getValorMensal() : BigDecimal.ZERO);
		cliente.setAtivo(cliente.getAtivo() != null && cliente.getAtivo() == 1 ? 1 : 0);
	}

	private UUID validarVendedor(UUID vendedorId) {
		if (vendedorId == null) {
			return null;
		}

		if (!vendedorRepository.existsById(vendedorId)) {
			throw new NoSuchElementException("Vendedor nao encontrado para o id: " + vendedorId);
		}

		return vendedorId;
	}

	private Map<UUID, String> carregarNomesVendedores() {
		Map<UUID, String> vendedoresPorId = new HashMap<>();
		StreamSupport.stream(vendedorRepository.findAll().spliterator(), false)
				.forEach(vendedor -> vendedoresPorId.put(vendedor.getId(), vendedor.getNome()));
		return vendedoresPorId;
	}

}
