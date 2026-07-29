package com.andreyferraz.gestao.module.resumo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResumoService {

	private final ResumoRepository resumoRepository;

	@Transactional(readOnly = true)
	public ResumoDashboardDto obterResumo() {
		return new ResumoDashboardDto(
				resumoRepository.buscarIndicadores(),
				resumoRepository.buscarDistribuicaoValoresMensais(),
				resumoRepository.buscarUltimosClientes(),
				resumoRepository.buscarUltimosLeads());
	}
}
