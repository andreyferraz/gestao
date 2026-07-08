package com.andreyferraz.gestao.module.chamado;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChamadoService {

	private final ChamadoRepository chamadoRepository;

	@PostConstruct
	void limparChamadosResolvidosAoIniciar() {
		chamadoRepository.excluirResolvidos();
	}

	public Chamado criar(Chamado chamado) {
		validarChamado(chamado);
		if (chamado.getId() == null) {
			chamado.setId(UUID.randomUUID());
		}
		if (chamado.getStatus() == null) {
			chamado.setStatus(Chamado.Status.ABERTO);
		}

		chamadoRepository.inserir(
				chamado.getId(),
				chamado.getClienteId(),
				chamado.getDescricaoProblema(),
				chamado.getStatus());

		return buscarPorId(chamado.getId());
	}

	public List<Chamado> listarTodos() {
		chamadoRepository.excluirResolvidos();
		return chamadoRepository.findAllOrderByRecente();
	}

	public Chamado buscarPorId(UUID id) {
		return chamadoRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Chamado nao encontrado para o id: " + id));
	}

	public Optional<Chamado> atualizarStatus(UUID id, Chamado.Status status) {
		if (status == null) {
			throw new IllegalArgumentException("Status do chamado e obrigatorio.");
		}

		buscarPorId(id);
		if (status == Chamado.Status.RESOLVIDO) {
			chamadoRepository.excluirPorId(id);
			return Optional.empty();
		}

		chamadoRepository.atualizarStatus(id, status);
		return Optional.of(buscarPorId(id));
	}

	public Chamado atualizarDescricao(UUID id, String descricaoProblema) {
		if (descricaoProblema == null || descricaoProblema.isBlank()) {
			throw new IllegalArgumentException("Descricao do problema e obrigatoria.");
		}

		buscarPorId(id);
		chamadoRepository.atualizarDescricao(id, descricaoProblema.trim());
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
}
