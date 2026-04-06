package com.andreyferraz.gestao.module.financeiro;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FinanceiroService {

	private final MovimentacaoRepository movimentacaoRepository;

	public Movimentacao criar(Movimentacao movimentacao) {
		validarMovimentacao(movimentacao);
		if (movimentacao.getId() == null) {
			movimentacao.setId(UUID.randomUUID());
		}
		return movimentacaoRepository.save(movimentacao);
	}

	public List<Movimentacao> listarTodas() {
		return StreamSupport.stream(movimentacaoRepository.findAll().spliterator(), false)
				.toList();
	}

	public Movimentacao buscarPorId(UUID id) {
		return movimentacaoRepository.findById(id)
				.orElseThrow(() -> new NoSuchElementException("Movimentacao nao encontrada para o id: " + id));
	}

	public List<Movimentacao> listarPorCliente(UUID clienteId) {
		return movimentacaoRepository.findByClienteId(clienteId);
	}

	public List<Movimentacao> listarPorTipo(Movimentacao.Tipo tipo) {
		return movimentacaoRepository.findByTipo(tipo);
	}

	public Movimentacao atualizar(UUID id, Movimentacao movimentacaoAtualizada) {
		validarMovimentacao(movimentacaoAtualizada);

		Movimentacao movimentacaoExistente = buscarPorId(id);
		movimentacaoExistente.setTipo(movimentacaoAtualizada.getTipo());
		movimentacaoExistente.setValor(movimentacaoAtualizada.getValor());
		movimentacaoExistente.setDataOcorrencia(movimentacaoAtualizada.getDataOcorrencia());
		movimentacaoExistente.setDescricao(movimentacaoAtualizada.getDescricao());
		movimentacaoExistente.setClienteId(movimentacaoAtualizada.getClienteId());

		return movimentacaoRepository.save(movimentacaoExistente);
	}

	public void remover(UUID id) {
		if (!movimentacaoRepository.existsById(id)) {
			throw new NoSuchElementException("Movimentacao nao encontrada para o id: " + id);
		}
		movimentacaoRepository.deleteById(id);
	}

	private void validarMovimentacao(Movimentacao movimentacao) {
		if (movimentacao.getTipo() == null) {
			throw new IllegalArgumentException("O tipo da movimentacao e obrigatorio.");
		}
		if (movimentacao.getValor() == null || movimentacao.getValor().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("O valor da movimentacao deve ser maior que zero.");
		}
		if (movimentacao.getDataOcorrencia() == null) {
			throw new IllegalArgumentException("A data de ocorrencia e obrigatoria.");
		}
	}

}
