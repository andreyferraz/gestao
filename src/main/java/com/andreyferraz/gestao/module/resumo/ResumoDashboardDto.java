package com.andreyferraz.gestao.module.resumo;

import java.util.List;

public record ResumoDashboardDto(
		ResumoIndicadoresDto indicadores,
		List<DistribuicaoValorMensalDto> distribuicaoValoresMensais,
		List<ClienteRecenteDto> ultimosClientes,
		List<LeadRecenteDto> ultimosLeads) {
}
