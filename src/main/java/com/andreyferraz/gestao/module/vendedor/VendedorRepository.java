package com.andreyferraz.gestao.module.vendedor;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VendedorRepository extends CrudRepository<Vendedor, UUID> {
	@Modifying
	@Query("INSERT INTO vendedor (id, nome, telefone, email, ativo) VALUES (:id, :nome, :telefone, :email, :ativo)")
	void inserir(
			UUID id,
			String nome,
			String telefone,
			String email,
			Integer ativo);

	@Modifying
	@Query("UPDATE vendedor SET nome = :nome, telefone = :telefone, email = :email, ativo = :ativo WHERE id = :id")
	void atualizar(
			UUID id,
			String nome,
			String telefone,
			String email,
			Integer ativo);
}