package com.andreyferraz.gestao.module.chamado;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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
@RequestMapping("/chamados")
@RequiredArgsConstructor
public class ChamadoController {

	private final ChamadoService chamadoService;

	@PostMapping
	public ResponseEntity<Chamado> criar(@RequestBody Chamado chamado) {
		Chamado chamadoCriado = chamadoService.criar(chamado);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(chamadoCriado.getId())
				.toUri();
		return ResponseEntity.created(location).body(chamadoCriado);
	}

	@GetMapping
	public ResponseEntity<List<Chamado>> listarTodos() {
		return ResponseEntity.ok(chamadoService.listarTodos());
	}

	@PutMapping("/{id}/status")
	public ResponseEntity<Chamado> atualizarStatus(
			@PathVariable UUID id,
			@RequestParam Chamado.Status status) {
		return ResponseEntity.ok(chamadoService.atualizarStatus(id, status));
	}
}
