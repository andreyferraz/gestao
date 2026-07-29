package com.andreyferraz.gestao.module.resumo;

import java.math.BigDecimal;

public record ResumoIndicadoresDto(
		long totalClientes,
		BigDecimal receitaMensalAtiva,
		long dominiosAtivos) {
}
