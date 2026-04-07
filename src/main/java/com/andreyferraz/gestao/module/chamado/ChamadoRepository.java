package com.andreyferraz.gestao.module.chamado;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChamadoRepository extends CrudRepository<Chamado, UUID> {

	@Query("SELECT id, cliente_id, descricao_problema, status FROM chamado ORDER BY rowid DESC")
	List<Chamado> findAllOrderByRecente();
}
