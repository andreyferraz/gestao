package com.andreyferraz.gestao.module.lead;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeadRepository extends CrudRepository<Lead, UUID> {

	@Modifying
	@Query("""
			INSERT INTO lead (
				id, nome, telefone, orcamento_desenvolvimento,
				orcamento_manutencao_hospedagem, observacoes, created_at, updated_at
			) VALUES (
				:id, :nome, :telefone, :orcamentoDesenvolvimento,
				:orcamentoManutencaoHospedagem, :observacoes, :createdAt,
				STRFTIME('%Y-%m-%d %H:%M:%f', 'now')
			)
			""")
	void inserir(
			UUID id,
			String nome,
			String telefone,
			java.math.BigDecimal orcamentoDesenvolvimento,
			java.math.BigDecimal orcamentoManutencaoHospedagem,
			String observacoes,
			String createdAt);

	@Modifying
	@Query("UPDATE lead SET nome = :nome, telefone = :telefone, orcamento_desenvolvimento = :orcamentoDesenvolvimento, orcamento_manutencao_hospedagem = :orcamentoManutencaoHospedagem, observacoes = :observacoes, updated_at = STRFTIME('%Y-%m-%d %H:%M:%f', 'now') WHERE id = :id")
	void atualizar(
			UUID id,
			String nome,
			String telefone,
			java.math.BigDecimal orcamentoDesenvolvimento,
			java.math.BigDecimal orcamentoManutencaoHospedagem,
			String observacoes);

	@Query("SELECT id, nome, telefone, orcamento_desenvolvimento, orcamento_manutencao_hospedagem, observacoes, created_at, updated_at FROM lead ORDER BY updated_at DESC, id DESC")
	List<Lead> findAllOrderByAtualizacaoRecente();
}
