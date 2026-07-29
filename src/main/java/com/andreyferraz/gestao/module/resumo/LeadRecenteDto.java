package com.andreyferraz.gestao.module.resumo;

import java.math.BigDecimal;
import java.util.UUID;

public record LeadRecenteDto(
		UUID id,
		String nome,
		String createdAt,
		BigDecimal orcamentoManutencaoHospedagem) {
}
