package com.andreyferraz.gestao.module.lead;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends CrudRepository<Lead, UUID> {

	@Modifying
	@Query("INSERT INTO lead (id, nome, telefone, orcamento_desenvolvimento, orcamento_manutencao_hospedagem, observacoes) VALUES (:id, :nome, :telefone, :orcamentoDesenvolvimento, :orcamentoManutencaoHospedagem, :observacoes)")
	void inserir(
			UUID id,
			String nome,
			String telefone,
			java.math.BigDecimal orcamentoDesenvolvimento,
			java.math.BigDecimal orcamentoManutencaoHospedagem,
			String observacoes);
}
