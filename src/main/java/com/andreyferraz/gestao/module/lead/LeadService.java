package com.andreyferraz.gestao.module.lead;

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
public class LeadService {

	private final LeadRepository leadRepository;
	private final JdbcTemplate jdbcTemplate;

	public Lead criar(Lead lead) {
		validarLead(lead);
		if (lead.getId() == null) {
			lead.setId(UUID.randomUUID());
		}

		jdbcTemplate.update(
				"INSERT INTO lead (id, nome, telefone, orcamento_desenvolvimento, orcamento_manutencao_hospedagem, observacoes) VALUES (?, ?, ?, ?, ?, ?)",
				lead.getId().toString(),
				lead.getNome(),
				lead.getTelefone(),
				lead.getOrcamentoDesenvolvimento() != null ? lead.getOrcamentoDesenvolvimento() : BigDecimal.ZERO,
				lead.getOrcamentoManutencaoHospedagem() != null ? lead.getOrcamentoManutencaoHospedagem() : BigDecimal.ZERO,
				lead.getObservacoes());

		return buscarPorId(lead.getId());
	}

	public List<Lead> listarTodos() {
		return StreamSupport.stream(leadRepository.findAll().spliterator(), false)
				.toList();
	}

	public Lead buscarPorId(UUID id) {
		return leadRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Lead nao encontrado para o id: " + id));
	}

	public Lead atualizar(UUID id, Lead leadAtualizado) {
		validarLead(leadAtualizado);

		Lead leadExistente = buscarPorId(id);
		leadExistente.setNome(leadAtualizado.getNome());
		leadExistente.setTelefone(leadAtualizado.getTelefone());
		leadExistente.setOrcamentoDesenvolvimento(leadAtualizado.getOrcamentoDesenvolvimento());
		leadExistente.setOrcamentoManutencaoHospedagem(leadAtualizado.getOrcamentoManutencaoHospedagem());
		leadExistente.setObservacoes(leadAtualizado.getObservacoes());

		return leadRepository.save(leadExistente);
	}

	public void remover(UUID id) {
		if (!leadRepository.existsById(id)) {
			throw new NoSuchElementException("Lead nao encontrado para o id: " + id);
		}
		leadRepository.deleteById(id);
	}

	private void validarLead(Lead lead) {
		if (lead.getNome() == null || lead.getNome().isBlank()) {
			throw new IllegalArgumentException("O nome do lead e obrigatorio.");
		}
		if (lead.getTelefone() == null || lead.getTelefone().isBlank()) {
			throw new IllegalArgumentException("O telefone do lead e obrigatorio.");
		}
		if (lead.getOrcamentoDesenvolvimento() == null
				|| lead.getOrcamentoDesenvolvimento().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("O orcamento de desenvolvimento nao pode ser negativo.");
		}
		if (lead.getOrcamentoManutencaoHospedagem() == null
				|| lead.getOrcamentoManutencaoHospedagem().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("O orcamento de manutencao/hospedagem nao pode ser negativo.");
		}
	}
}
