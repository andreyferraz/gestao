package com.andreyferraz.gestao.module.cliente;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClienteRepository extends CrudRepository<Cliente, UUID> {

	@Modifying
	@Query("INSERT INTO cliente (id, nome, contato, dominio_aplicacao, data_vencimento_dominio, valor_mensal, ativo) VALUES (:id, :nome, :contato, :dominioAplicacao, :dataVencimentoDominio, :valorMensal, :ativo)")
	void inserir(
			UUID id,
			String nome,
			String contato,
			String dominioAplicacao,
			LocalDate dataVencimentoDominio,
			BigDecimal valorMensal,
			Integer ativo);

	@Modifying
	@Query("UPDATE cliente SET nome = :nome, contato = :contato, dominio_aplicacao = :dominioAplicacao, data_vencimento_dominio = :dataVencimentoDominio, valor_mensal = :valorMensal, ativo = :ativo WHERE id = :id")
	void atualizar(
			UUID id,
			String nome,
			String contato,
			String dominioAplicacao,
			LocalDate dataVencimentoDominio,
			BigDecimal valorMensal,
			Integer ativo);

}
