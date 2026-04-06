package com.andreyferraz.gestao.module.relatorio;

import java.math.BigDecimal;
import java.util.Objects;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.andreyferraz.gestao.module.financeiro.Movimentacao;
import com.andreyferraz.gestao.module.financeiro.MovimentacaoRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioService {

	private final MovimentacaoRepository movimentacaoRepository;

	public RelatorioMensalDto gerarRelatorioMensal(int ano, int mes) {
		if (mes < 1 || mes > 12) {
			throw new IllegalArgumentException("Mes invalido. Informe um valor entre 1 e 12.");
		}

		YearMonth competencia = YearMonth.of(ano, mes);

		List<Movimentacao> movimentacoesDoMes = StreamSupport
				.stream(movimentacaoRepository.findAll().spliterator(), false)
				.filter(mov -> mov.getDataOcorrencia() != null)
				.filter(mov -> YearMonth.from(mov.getDataOcorrencia()).equals(competencia))
				.sorted(Comparator.comparing(Movimentacao::getDataOcorrencia).thenComparing(Movimentacao::getId))
				.toList();

		BigDecimal totalEntradas = somarPorTipo(movimentacoesDoMes, Movimentacao.Tipo.ENTRADA);
		BigDecimal totalSaidas = somarPorTipo(movimentacoesDoMes, Movimentacao.Tipo.SAIDA);
		BigDecimal saldo = totalEntradas.subtract(totalSaidas);

		List<RelatorioMensalDto.ItemRelatorioDto> itens = movimentacoesDoMes.stream()
				.map(mov -> new RelatorioMensalDto.ItemRelatorioDto(
						mov.getId(),
						mov.getTipo(),
						mov.getValor(),
						mov.getDataOcorrencia(),
						mov.getDescricao(),
						mov.getClienteId()))
				.toList();

		return new RelatorioMensalDto(ano, mes, totalEntradas, totalSaidas, saldo, itens);
	}

	private BigDecimal somarPorTipo(List<Movimentacao> movimentacoes, Movimentacao.Tipo tipo) {
		return movimentacoes.stream()
				.filter(mov -> mov.getTipo() == tipo)
				.map(Movimentacao::getValor)
				.filter(Objects::nonNull)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}