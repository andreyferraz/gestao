package com.andreyferraz.gestao.module.chamado;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChamadoService {

	private final ChamadoRepository chamadoRepository;

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
		return chamadoRepository.findAllOrderByRecente();
	}

	public Chamado buscarPorId(UUID id) {
		return chamadoRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Chamado nao encontrado para o id: " + id));
	}

	public Chamado atualizarStatus(UUID id, Chamado.Status status) {
		if (status == null) {
			throw new IllegalArgumentException("Status do chamado e obrigatorio.");
		}

		buscarPorId(id);
		chamadoRepository.atualizarStatus(id, status);
		return buscarPorId(id);
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
