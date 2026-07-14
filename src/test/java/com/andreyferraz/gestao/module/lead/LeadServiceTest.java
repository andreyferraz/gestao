package com.andreyferraz.gestao.module.lead;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

	@Mock
	private LeadRepository leadRepository;

	@InjectMocks
	private LeadService leadService;

	@Test
	void listarTodos_deveUsarOrdenacaoPorAtualizacaoRecente() {
		var primeiro = new Lead(UUID.randomUUID(), "Lead 1", "1111", BigDecimal.ONE, BigDecimal.TEN, "obs 1", "2026-07-14 10:00:00.000");
		var segundo = new Lead(UUID.randomUUID(), "Lead 2", "2222", BigDecimal.TEN, BigDecimal.ONE, "obs 2", "2026-07-14 11:00:00.000");
		when(leadRepository.findAllOrderByAtualizacaoRecente()).thenReturn(List.of(segundo, primeiro));

		var result = leadService.listarTodos();

		assertEquals(List.of(segundo, primeiro), result);
		verify(leadRepository).findAllOrderByAtualizacaoRecente();
	}

	@Test
	void atualizar_devePersistirMudancaERecarregarLead() {
		UUID id = UUID.randomUUID();
		var existente = new Lead(id, "Lead antigo", "9999", BigDecimal.ZERO, BigDecimal.ZERO, "antes", "2026-07-14 09:00:00.000");
		var atualizado = new Lead(null, "Lead novo", "8888", BigDecimal.valueOf(50), BigDecimal.valueOf(75), "depois", null);
		var persistido = new Lead(id, "Lead novo", "8888", BigDecimal.valueOf(50), BigDecimal.valueOf(75), "depois", "2026-07-14 12:00:00.000");

		when(leadRepository.findById(id)).thenReturn(Optional.of(existente)).thenReturn(Optional.of(persistido));

		var result = leadService.atualizar(id, atualizado);

		assertEquals("Lead novo", result.getNome());
		verify(leadRepository).atualizar(id, "Lead novo", "8888", BigDecimal.valueOf(50), BigDecimal.valueOf(75), "depois");
	}
}