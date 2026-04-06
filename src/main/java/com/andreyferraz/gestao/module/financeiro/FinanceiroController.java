package com.andreyferraz.gestao.module.financeiro;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/movimentacoes")
@RequiredArgsConstructor
public class FinanceiroController {

	private final FinanceiroService financeiroService;

	@PostMapping
	public ResponseEntity<Movimentacao> criar(@RequestBody Movimentacao movimentacao) {
		Movimentacao movimentacaoCriada = financeiroService.criar(movimentacao);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(movimentacaoCriada.getId())
				.toUri();
		return ResponseEntity.created(location).body(movimentacaoCriada);
	}

	@GetMapping
	public ResponseEntity<List<Movimentacao>> listar(
			@RequestParam(required = false) UUID clienteId,
			@RequestParam(required = false) Movimentacao.Tipo tipo) {
		if (clienteId != null) {
			return ResponseEntity.ok(financeiroService.listarPorCliente(clienteId));
		}
		if (tipo != null) {
			return ResponseEntity.ok(financeiroService.listarPorTipo(tipo));
		}
		return ResponseEntity.ok(financeiroService.listarTodas());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Movimentacao> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(financeiroService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Movimentacao> atualizar(@PathVariable UUID id, @RequestBody Movimentacao movimentacao) {
		return ResponseEntity.ok(financeiroService.atualizar(id, movimentacao));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable UUID id) {
		financeiroService.remover(id);
		return ResponseEntity.noContent().build();
	}

}
