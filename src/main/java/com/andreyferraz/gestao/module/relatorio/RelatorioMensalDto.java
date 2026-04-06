package com.andreyferraz.gestao.module.relatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.andreyferraz.gestao.module.financeiro.Movimentacao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioMensalDto {

	private int ano;

	private int mes;

	private BigDecimal totalEntradas;

	private BigDecimal totalSaidas;

	private BigDecimal saldo;

	private List<ItemRelatorioDto> movimentacoes;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ItemRelatorioDto {
		private UUID id;
		private Movimentacao.Tipo tipo;
		private BigDecimal valor;
		private LocalDate dataOcorrencia;
		private String descricao;
		private UUID clienteId;
	}

}
