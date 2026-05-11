package com.andreyferraz.gestao.module.vendedor;

import java.util.UUID;

public record VendedorDashboardDto(
		UUID id,
		String nome,
		String telefone,
		String email,
		Boolean ativo,
		long clientesAssociados,
		long clientesAtivos,
		boolean aptoComissao,
		double comissaoTotal) {
}