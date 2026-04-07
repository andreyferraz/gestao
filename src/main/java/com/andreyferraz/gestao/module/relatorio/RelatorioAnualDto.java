package com.andreyferraz.gestao.module.relatorio;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioAnualDto {

	private int ano;

	private BigDecimal totalEntradas;

	private BigDecimal totalSaidas;

	private BigDecimal saldo;

	private int quantidadeMovimentacoes;

	private List<ResumoMensalDto> resumoMensal;

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ResumoMensalDto {
		private int mes;
		private BigDecimal totalEntradas;
		private BigDecimal totalSaidas;
		private BigDecimal saldo;
		private int quantidadeMovimentacoes;
	}
}
