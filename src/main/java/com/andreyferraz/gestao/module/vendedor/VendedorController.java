package com.andreyferraz.gestao.module.vendedor;

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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/vendedores")
@RequiredArgsConstructor
public class VendedorController {

	private final VendedorService vendedorService;

	@PostMapping
	public ResponseEntity<Vendedor> criar(@RequestBody Vendedor vendedor) {
		Vendedor vendedorCriado = vendedorService.criar(vendedor);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(vendedorCriado.getId())
				.toUri();
		return ResponseEntity.created(location).body(vendedorCriado);
	}

	@GetMapping
	public ResponseEntity<List<Vendedor>> listarTodos() {
		return ResponseEntity.ok(vendedorService.listarTodos());
	}

	@GetMapping("/dashboard")
	public ResponseEntity<List<VendedorDashboardDto>> listarDashboard() {
		return ResponseEntity.ok(vendedorService.listarResumoDashboard());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Vendedor> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(vendedorService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Vendedor> atualizar(@PathVariable UUID id, @RequestBody Vendedor vendedor) {
		return ResponseEntity.ok(vendedorService.atualizar(id, vendedor));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable UUID id) {
		vendedorService.remover(id);
		return ResponseEntity.noContent().build();
	}
}