package com.andreyferraz.gestao.module.chamado;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChamadoRepository extends CrudRepository<Chamado, UUID> {

	@Modifying
	@Query("INSERT INTO chamado (id, cliente_id, descricao_problema, status) VALUES (:id, :clienteId, :descricaoProblema, :status)")
	void inserir(UUID id, UUID clienteId, String descricaoProblema, Chamado.Status status);

	@Modifying
	@Query("UPDATE chamado SET status = :status WHERE id = :id")
	void atualizarStatus(UUID id, Chamado.Status status);

	@Modifying
	@Query("UPDATE chamado SET descricao_problema = :descricaoProblema WHERE id = :id")
	void atualizarDescricao(UUID id, String descricaoProblema);

	@Query("SELECT id, cliente_id, descricao_problema, status FROM chamado ORDER BY rowid DESC")
	List<Chamado> findAllOrderByRecente();
}
