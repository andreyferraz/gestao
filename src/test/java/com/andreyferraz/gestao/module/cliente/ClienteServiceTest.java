package com.andreyferraz.gestao.module.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void criar_deveInserirERetornarCliente() {
        var input = new Cliente(null, "Nome", "contato", "dominio", LocalDate.of(2026, 1, 1), BigDecimal.valueOf(100), 1);

        UUID generatedId = UUID.randomUUID();
        var returned = new Cliente(generatedId, input.getNome(), input.getContato(), input.getDominioAplicacao(), input.getDataVencimentoDominio(), input.getValorMensal(), input.getAtivo());

        when(clienteRepository.findById(any(UUID.class))).thenReturn(Optional.of(returned));
        doNothing().when(clienteRepository).inserir(any(UUID.class), anyString(), anyString(), anyString(), any(LocalDate.class), any(BigDecimal.class), any(Integer.class));

        var result = clienteService.criar(input);

        assertEquals(returned, result);
        verify(clienteRepository).inserir(any(UUID.class), eq("Nome"), eq("contato"), eq("dominio"), eq(LocalDate.of(2026, 1, 1)), eq(BigDecimal.valueOf(100)), eq(1));
    }

    @Test
    void remover_quandoNaoExistir_deveLancar() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsById(id)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> clienteService.remover(id));
    }

    @Test
    void listarResumoDashboard_trataValorNuloComoZero() {
        var c = new Cliente(UUID.randomUUID(), "N", "c", "d", LocalDate.now(), null, 0);
        when(clienteRepository.findAll()).thenReturn(List.of(c));
        var resumo = clienteService.listarResumoDashboard();
        assertEquals(1, resumo.size());
        assertEquals(0.0, resumo.get(0).valorMensal());
    }

}
