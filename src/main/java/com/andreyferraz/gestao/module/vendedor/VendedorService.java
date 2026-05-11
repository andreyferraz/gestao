package com.andreyferraz.gestao.module.vendedor;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.andreyferraz.gestao.module.cliente.Cliente;
import com.andreyferraz.gestao.module.cliente.ClienteService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VendedorService {

	private static final int MIN_CLIENTES_COMISSAO = 5;
	private static final BigDecimal TAXA_COMISSAO = new BigDecimal("0.20");

	private final VendedorRepository vendedorRepository;
	private final ClienteService clienteService;
	private final JdbcTemplate jdbcTemplate;

	@Transactional
	public Vendedor criar(Vendedor vendedor) {
		validarVendedor(vendedor);
		if (vendedor.getId() == null) {
			vendedor.setId(UUID.randomUUID());
		}
		normalizarVendedorParaPersistencia(vendedor);

		vendedorRepository.inserir(
				vendedor.getId(),
				vendedor.getNome(),
				vendedor.getTelefone(),
				vendedor.getEmail(),
				vendedor.getAtivo());

		return vendedorRepository.findById(vendedor.getId()).orElse(vendedor);
	}

	@Transactional(readOnly = true)
	public List<Vendedor> listarTodos() {
		return StreamSupport.stream(vendedorRepository.findAll().spliterator(), false)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<VendedorDashboardDto> listarResumoDashboard() {
		List<Cliente> clientes = clienteService.listarTodos();

		return StreamSupport.stream(vendedorRepository.findAll().spliterator(), false)
				.map(vendedor -> montarResumo(vendedor, clientes))
				.toList();
	}

	@Transactional(readOnly = true)
	public Vendedor buscarPorId(UUID id) {
		return vendedorRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Vendedor nao encontrado para o id: " + id));
	}

	@Transactional
	public Vendedor atualizar(UUID id, Vendedor vendedorAtualizado) {
		validarVendedor(vendedorAtualizado);
		vendedorRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Vendedor nao encontrado para o id: " + id));
		vendedorAtualizado.setId(id);
		normalizarVendedorParaPersistencia(vendedorAtualizado);

		vendedorRepository.atualizar(
				id,
				vendedorAtualizado.getNome(),
				vendedorAtualizado.getTelefone(),
				vendedorAtualizado.getEmail(),
				vendedorAtualizado.getAtivo());

		return vendedorRepository.findById(id).orElse(vendedorAtualizado);
	}

	@Transactional
	public void remover(UUID id) {
		if (!vendedorRepository.existsById(id)) {
			throw new NoSuchElementException("Vendedor nao encontrado para o id: " + id);
		}

		jdbcTemplate.update("UPDATE cliente SET vendedor_id = NULL WHERE vendedor_id = ?", id);
		vendedorRepository.deleteById(id);
	}

	private VendedorDashboardDto montarResumo(Vendedor vendedor, List<Cliente> clientes) {
		List<Cliente> vinculados = clientes.stream()
				.filter(cliente -> vendedor.getId().equals(cliente.getVendedorId()))
				.toList();

		long clientesAtivos = vinculados.stream()
				.filter(cliente -> cliente.getAtivo() != null && cliente.getAtivo() == 1)
				.count();
		boolean aptoComissao = clientesAtivos >= MIN_CLIENTES_COMISSAO;
		double comissaoTotal = aptoComissao
				? vinculados.stream()
						.filter(cliente -> cliente.getAtivo() != null && cliente.getAtivo() == 1)
						.map(Cliente::getValorMensal)
						.filter(Objects::nonNull)
						.mapToDouble(BigDecimal::doubleValue)
						.sum() * TAXA_COMISSAO.doubleValue()
				: 0.0;

		return new VendedorDashboardDto(
				vendedor.getId(),
				vendedor.getNome(),
				vendedor.getTelefone(),
				vendedor.getEmail(),
				vendedor.getAtivo() != null && vendedor.getAtivo() == 1,
				vinculados.size(),
				clientesAtivos,
				aptoComissao,
				comissaoTotal);
	}

	private void validarVendedor(Vendedor vendedor) {
		if (vendedor.getNome() == null || vendedor.getNome().isBlank()) {
			throw new IllegalArgumentException("O nome do vendedor e obrigatorio.");
		}
		if (vendedor.getTelefone() == null || vendedor.getTelefone().isBlank()) {
			throw new IllegalArgumentException("O telefone do vendedor e obrigatorio.");
		}
	}

	private void normalizarVendedorParaPersistencia(Vendedor vendedor) {
		vendedor.setAtivo(vendedor.getAtivo() != null && vendedor.getAtivo() == 1 ? 1 : 0);
	}
}