package com.andreyferraz.gestao.module.cliente;

import java.time.LocalDate;
import java.util.UUID;

public record ClienteDashboardDto(
		UUID id,
		String nome,
		String contato,
		String dominioAplicacao,
		LocalDate dataVencimentoDominio,
		Boolean ativo,
		double valorMensal) {
}
