package com.andreyferraz.gestao.module.resumo;

import java.math.BigDecimal;

public record DistribuicaoValorMensalDto(
		BigDecimal valorMensal,
		long quantidadeClientes) {
}
