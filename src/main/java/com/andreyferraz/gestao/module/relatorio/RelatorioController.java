package com.andreyferraz.gestao.module.relatorio;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/relatorios")
@RequiredArgsConstructor
public class RelatorioController {

	private final RelatorioService relatorioService;

	@GetMapping("/mensal")
	@ResponseBody
	public ResponseEntity<RelatorioMensalDto> obterRelatorioMensal(
			@RequestParam int ano,
			@RequestParam int mes) {
		return ResponseEntity.ok(relatorioService.gerarRelatorioMensal(ano, mes));
	}

	@GetMapping("/anual")
	@ResponseBody
	public ResponseEntity<RelatorioAnualDto> obterRelatorioAnual(@RequestParam int ano) {
		return ResponseEntity.ok(relatorioService.gerarRelatorioAnual(ano));
	}

	@GetMapping("/mensal/pdf")
	public String gerarRelatorioMensalPdf(
			@RequestParam int ano,
			@RequestParam int mes,
			Model model) {
		RelatorioMensalDto relatorio = relatorioService.gerarRelatorioMensal(ano, mes);
		model.addAttribute("relatorio", relatorio);
		return "relatorio/mensal-pdf";
	}

}
