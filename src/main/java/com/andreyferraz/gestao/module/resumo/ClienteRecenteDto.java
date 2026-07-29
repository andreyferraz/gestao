package com.andreyferraz.gestao.module.resumo;

import java.math.BigDecimal;
import java.util.UUID;

public record ClienteRecenteDto(
		UUID id,
		String nome,
		String createdAt,
		BigDecimal valorMensal,
		boolean ativo) {
}
