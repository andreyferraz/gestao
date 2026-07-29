package com.andreyferraz.gestao.module.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
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

import com.andreyferraz.gestao.module.vendedor.VendedorRepository;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private VendedorRepository vendedorRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void criar_deveInserirERetornarCliente() {
        var input = new Cliente();
        input.setNome("Nome");
        input.setContato("contato");
        input.setDominioAplicacao("dominio");
        input.setDataVencimentoDominio(LocalDate.of(2026, 1, 1));
        input.setInformacoesUteis("infos importantes");
        input.setValorMensal(BigDecimal.valueOf(100));
        input.setAtivo(1);
        input.setVendedorId(null);

        var result = clienteService.criar(input);

        assertNotNull(result.getId());
        assertEquals(input.getNome(), result.getNome());
        assertEquals(input.getContato(), result.getContato());
        assertEquals(input.getDominioAplicacao(), result.getDominioAplicacao());
        assertEquals(input.getDataVencimentoDominio(), result.getDataVencimentoDominio());
        assertEquals(input.getInformacoesUteis(), result.getInformacoesUteis());
        assertEquals(input.getValorMensal(), result.getValorMensal());
        assertEquals(input.getAtivo(), result.getAtivo());
        verify(clienteRepository).inserir(any(UUID.class), eq("Nome"), eq("contato"), eq("dominio"), eq(LocalDate.of(2026, 1, 1)), eq("infos importantes"), eq(BigDecimal.valueOf(100)), eq(1), isNull(), any(String.class));
    }

    @Test
    void criar_quandoReleituraFalhar_deveRetornarClientePersistido() {
        var input = new Cliente();
        input.setNome("Nome");
        input.setContato("contato");
        input.setDominioAplicacao("dominio");
        input.setDataVencimentoDominio(LocalDate.of(2026, 1, 1));
        input.setInformacoesUteis("sem valor informado");
        input.setValorMensal(null);
        input.setAtivo(null);
        input.setVendedorId(null);

        var result = clienteService.criar(input);

        assertEquals(input.getId(), result.getId());
        assertEquals(BigDecimal.ZERO, result.getValorMensal());
        assertEquals(0, result.getAtivo());
    }

    @Test
    void criar_deveDefinirDataOriginalEmUtc() {
        var input = clienteValido();

        var result = clienteService.criar(input);

        assertNotNull(result.getCreatedAt());
        assertDoesNotThrow(() -> Instant.parse(result.getCreatedAt()));
        verify(clienteRepository).inserir(
                eq(result.getId()), eq(result.getNome()), eq(result.getContato()),
                eq(result.getDominioAplicacao()), eq(result.getDataVencimentoDominio()),
                eq(result.getInformacoesUteis()), eq(result.getValorMensal()),
                eq(result.getAtivo()), isNull(), eq(result.getCreatedAt()));
    }

    @Test
    void atualizar_devePreservarDataOriginal() {
        UUID id = UUID.randomUUID();
        var existente = clienteValido();
        existente.setId(id);
        existente.setCreatedAt("2026-06-01T10:00:00Z");
        var alterado = clienteValido();
        alterado.setCreatedAt("2099-01-01T00:00:00Z");
        when(clienteRepository.findById(id)).thenReturn(Optional.of(existente));

        var result = clienteService.atualizar(id, alterado);

        assertEquals("2026-06-01T10:00:00Z", result.getCreatedAt());
    }

    @Test
    void remover_quandoNaoExistir_deveLancar() {
        UUID id = UUID.randomUUID();
        when(clienteRepository.existsById(id)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> clienteService.remover(id));
    }

    @Test
    void listarResumoDashboard_trataValorNuloComoZero() {
        when(vendedorRepository.findAll()).thenReturn(List.of());
        var c = new Cliente();
        c.setId(UUID.randomUUID());
        c.setNome("N");
        c.setContato("c");
        c.setDominioAplicacao("d");
        c.setDataVencimentoDominio(LocalDate.now());
        c.setInformacoesUteis("observacoes do cliente");
        c.setValorMensal(null);
        c.setAtivo(0);
        c.setVendedorId(null);
        when(clienteRepository.findAll()).thenReturn(List.of(c));
        var resumo = clienteService.listarResumoDashboard();
        assertEquals(1, resumo.size());
        assertEquals(0.0, resumo.get(0).valorMensal());
        assertEquals("observacoes do cliente", resumo.get(0).informacoesUteis());
    }

    @Test
    void criar_quandoVendedorInvalido_deveLancar() {
        UUID vendedorId = UUID.randomUUID();
        var input = new Cliente();
        input.setNome("Nome");
        input.setContato("contato");
        input.setDominioAplicacao("dominio");
        input.setDataVencimentoDominio(LocalDate.of(2026, 1, 1));
        input.setInformacoesUteis("dados do vendedor");
        input.setValorMensal(BigDecimal.valueOf(100));
        input.setAtivo(1);
        input.setVendedorId(vendedorId);

        when(vendedorRepository.existsById(vendedorId)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> clienteService.criar(input));
        verify(vendedorRepository).existsById(vendedorId);
    }

    @Test
    void listarResumoDashboard_incluiNomeVendedor() {
        UUID vendedorId = UUID.randomUUID();
        var vendedor = new com.andreyferraz.gestao.module.vendedor.Vendedor(vendedorId, "Joao", "111", "j@e.com", 1);
        when(vendedorRepository.findAll()).thenReturn(List.of(vendedor));

        var c = new Cliente();
        c.setId(UUID.randomUUID());
        c.setNome("Cliente1");
        c.setContato("cont");
        c.setDominioAplicacao("dom");
        c.setDataVencimentoDominio(LocalDate.now());
        c.setInformacoesUteis("informacao extra");
        c.setValorMensal(BigDecimal.valueOf(50));
        c.setAtivo(1);
        c.setVendedorId(vendedorId);

        when(clienteRepository.findAll()).thenReturn(List.of(c));

        var resumo = clienteService.listarResumoDashboard();
        assertEquals(1, resumo.size());
        assertEquals("Joao", resumo.get(0).vendedorNome());
        assertEquals("informacao extra", resumo.get(0).informacoesUteis());
        assertTrue(resumo.get(0).ativo());
        assertEquals(50.0, resumo.get(0).valorMensal());
    }

    private Cliente clienteValido() {
        var cliente = new Cliente();
        cliente.setNome("Nome");
        cliente.setContato("contato");
        cliente.setDominioAplicacao("dominio");
        cliente.setDataVencimentoDominio(LocalDate.of(2026, 1, 1));
        cliente.setInformacoesUteis("informacoes");
        cliente.setValorMensal(new BigDecimal("100.00"));
        cliente.setAtivo(1);
        cliente.setVendedorId(null);
        return cliente;
    }

}
