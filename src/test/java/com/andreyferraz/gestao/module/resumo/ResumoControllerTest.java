package com.andreyferraz.gestao.module.resumo;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ResumoControllerTest {

	@Test
	void getResumo_deveRetornarContratoConsolidado() throws Exception {
		ResumoService service = mock(ResumoService.class);
		UUID clienteId = UUID.randomUUID();
		UUID leadId = UUID.randomUUID();
		var resposta = new ResumoDashboardDto(
				new ResumoIndicadoresDto(3, new BigDecimal("350.00"), 2),
				List.of(new DistribuicaoValorMensalDto(new BigDecimal("100.00"), 2)),
				List.of(new ClienteRecenteDto(
						clienteId, "Cliente novo", "2026-07-29T10:00:00Z",
						new BigDecimal("100.00"), true)),
				List.of(new LeadRecenteDto(
						leadId, "Lead novo", "2026-07-29T09:00:00Z",
						new BigDecimal("300.00"))));
		when(service.obterResumo()).thenReturn(resposta);
		MockMvc mockMvc = MockMvcBuilders
				.standaloneSetup(new ResumoController(service))
				.build();

		mockMvc.perform(get("/resumo"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.indicadores.totalClientes").value(3))
				.andExpect(jsonPath("$.indicadores.receitaMensalAtiva").value(350.0))
				.andExpect(jsonPath("$.indicadores.dominiosAtivos").value(2))
				.andExpect(jsonPath("$.distribuicaoValoresMensais[0].valorMensal").value(100.0))
				.andExpect(jsonPath("$.distribuicaoValoresMensais[0].quantidadeClientes").value(2))
				.andExpect(jsonPath("$.ultimosClientes[0].id").value(clienteId.toString()))
				.andExpect(jsonPath("$.ultimosClientes[0].createdAt")
						.value("2026-07-29T10:00:00Z"))
				.andExpect(jsonPath("$.ultimosLeads[0].id").value(leadId.toString()));
	}
}
