package com.andreyferraz.gestao.module.website.projeto;

import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjetoRepository extends CrudRepository<Projeto, UUID> {

	@Modifying
	@Query("""
			INSERT INTO projeto (id, titulo, descricao, imagem_url, link)
			VALUES (:id, :titulo, :descricao, :imagemUrl, :link)
			""")
	void inserir(UUID id, String titulo, String descricao, String imagemUrl, String link);

	@Modifying
	@Query("""
			UPDATE projeto
			SET titulo = :titulo,
				descricao = :descricao,
				imagem_url = :imagemUrl,
				link = :link
			WHERE id = :id
			""")
	void atualizar(UUID id, String titulo, String descricao, String imagemUrl, String link);

	@Modifying
	@Query("""
			UPDATE projeto
			SET titulo = :titulo,
				descricao = :descricao,
				imagem_url = :imagemUrl,
				link = :link
			WHERE id = :id
				AND (titulo = :tituloAnterior
					OR (titulo IS NULL AND :tituloAnterior IS NULL))
				AND (descricao = :descricaoAnterior
					OR (descricao IS NULL AND :descricaoAnterior IS NULL))
				AND (imagem_url = :imagemAnterior
					OR (imagem_url IS NULL AND :imagemAnterior IS NULL))
				AND (link = :linkAnterior
					OR (link IS NULL AND :linkAnterior IS NULL))
			""")
	int atualizarSeEstadoAtual(
			UUID id,
			String titulo,
			String descricao,
			String imagemUrl,
			String link,
			String tituloAnterior,
			String descricaoAnterior,
			String imagemAnterior,
			String linkAnterior);

	@Modifying
	@Query("""
			DELETE FROM projeto
			WHERE id = :id
				AND (titulo = :tituloAtual
					OR (titulo IS NULL AND :tituloAtual IS NULL))
				AND (descricao = :descricaoAtual
					OR (descricao IS NULL AND :descricaoAtual IS NULL))
				AND (imagem_url = :imagemAtual
					OR (imagem_url IS NULL AND :imagemAtual IS NULL))
				AND (link = :linkAtual
					OR (link IS NULL AND :linkAtual IS NULL))
			""")
	int deletarSeEstadoAtual(
			UUID id,
			String tituloAtual,
			String descricaoAtual,
			String imagemAtual,
			String linkAtual);

}
