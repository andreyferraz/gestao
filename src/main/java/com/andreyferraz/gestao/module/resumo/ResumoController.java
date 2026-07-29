package com.andreyferraz.gestao.module.resumo;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/resumo")
@RequiredArgsConstructor
public class ResumoController {

	private final ResumoService resumoService;

	@GetMapping
	public ResponseEntity<ResumoDashboardDto> obterResumo() {
		return ResponseEntity.ok(resumoService.obterResumo());
	}
}
