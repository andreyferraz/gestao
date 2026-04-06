package com.andreyferraz.gestao.module.cliente;

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
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

	private final ClienteService clienteService;

	@PostMapping
	public ResponseEntity<Cliente> criar(@RequestBody Cliente cliente) {
		Cliente clienteCriado = clienteService.criar(cliente);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(clienteCriado.getId())
				.toUri();
		return ResponseEntity.created(location).body(clienteCriado);
	}

	@GetMapping
	public ResponseEntity<List<Cliente>> listarTodos() {
		return ResponseEntity.ok(clienteService.listarTodos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Cliente> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(clienteService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Cliente> atualizar(@PathVariable UUID id, @RequestBody Cliente cliente) {
		return ResponseEntity.ok(clienteService.atualizar(id, cliente));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable UUID id) {
		clienteService.remover(id);
		return ResponseEntity.noContent().build();
	}

}
