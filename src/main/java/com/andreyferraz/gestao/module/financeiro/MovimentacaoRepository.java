package com.andreyferraz.gestao.module.financeiro;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimentacaoRepository extends CrudRepository<Movimentacao, UUID> {

	List<Movimentacao> findByClienteId(UUID clienteId);

	List<Movimentacao> findByTipo(Movimentacao.Tipo tipo);

}
