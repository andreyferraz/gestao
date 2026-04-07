package com.andreyferraz.gestao.module.lead;

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
@RequestMapping("/leads")
@RequiredArgsConstructor
public class LeadController {

	private final LeadService leadService;

	@PostMapping
	public ResponseEntity<Lead> criar(@RequestBody Lead lead) {
		Lead leadCriado = leadService.criar(lead);
		URI location = ServletUriComponentsBuilder
				.fromCurrentRequest()
				.path("/{id}")
				.buildAndExpand(leadCriado.getId())
				.toUri();
		return ResponseEntity.created(location).body(leadCriado);
	}

	@GetMapping
	public ResponseEntity<List<Lead>> listarTodos() {
		return ResponseEntity.ok(leadService.listarTodos());
	}

	@GetMapping("/{id}")
	public ResponseEntity<Lead> buscarPorId(@PathVariable UUID id) {
		return ResponseEntity.ok(leadService.buscarPorId(id));
	}

	@PutMapping("/{id}")
	public ResponseEntity<Lead> atualizar(@PathVariable UUID id, @RequestBody Lead lead) {
		return ResponseEntity.ok(leadService.atualizar(id, lead));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> remover(@PathVariable UUID id) {
		leadService.remover(id);
		return ResponseEntity.noContent().build();
	}
}
