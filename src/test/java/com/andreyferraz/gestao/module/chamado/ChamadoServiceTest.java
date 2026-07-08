package com.andreyferraz.gestao.module.chamado;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChamadoServiceTest {

    @Mock
    private ChamadoRepository chamadoRepository;

    @InjectMocks
    private ChamadoService chamadoService;

    @Test
    void atualizarStatus_quandoResolvido_deveExcluirChamado() {
        UUID chamadoId = UUID.randomUUID();
        var chamado = new Chamado(chamadoId, UUID.randomUUID(), "Problema", Chamado.Status.ABERTO);

        when(chamadoRepository.findById(chamadoId)).thenReturn(Optional.of(chamado));

        Optional<Chamado> resultado = chamadoService.atualizarStatus(chamadoId, Chamado.Status.RESOLVIDO);

        assertTrue(resultado.isEmpty());
        verify(chamadoRepository).excluirPorId(chamadoId);
        verify(chamadoRepository, never()).atualizarStatus(chamadoId, Chamado.Status.RESOLVIDO);
    }

    @Test
    void listarTodos_deveRemoverResolvidosAntesDeListar() {
        chamadoService.listarTodos();

        verify(chamadoRepository).excluirResolvidos();
        verify(chamadoRepository).findAllOrderByRecente();
    }
}
