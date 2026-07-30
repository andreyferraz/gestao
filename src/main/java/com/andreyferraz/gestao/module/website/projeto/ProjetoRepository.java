package com.andreyferraz.gestao.module.website.projeto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Modifying;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProjetoRepository extends CrudRepository<Projeto, UUID> {

	default void inserir(
			UUID id,
			String titulo,
			String descricao,
			String imagemUrl,
			String link) {
		inserirComAtualizacao(
				id, titulo, descricao, imagemUrl, link, Instant.now().toString());
	}

	@Modifying
	@Query("""
			INSERT INTO projeto (
				id, titulo, descricao, imagem_url, link, updated_at
			)
			VALUES (
				:id, :titulo, :descricao, :imagemUrl, :link, :updatedAt
			)
			""")
	void inserirComAtualizacao(
			UUID id,
			String titulo,
			String descricao,
			String imagemUrl,
			String link,
			String updatedAt);

	default void atualizar(
			UUID id,
			String titulo,
			String descricao,
			String imagemUrl,
			String link) {
		atualizarComData(
				id, titulo, descricao, imagemUrl, link, Instant.now().toString());
	}

	@Modifying
	@Query("""
			UPDATE projeto
			SET titulo = :titulo,
				descricao = :descricao,
				imagem_url = :imagemUrl,
				link = :link,
				updated_at = :updatedAt
			WHERE id = :id
			""")
	void atualizarComData(
			UUID id,
			String titulo,
			String descricao,
			String imagemUrl,
			String link,
			String updatedAt);

	default int atualizarSeEstadoAtual(
			UUID id,
			String titulo,
			String descricao,
			String imagemUrl,
			String link,
			String tituloAnterior,
			String descricaoAnterior,
			String imagemAnterior,
			String linkAnterior) {
		return atualizarSeEstadoAtualComData(
				id,
				titulo,
				descricao,
				imagemUrl,
				link,
				tituloAnterior,
				descricaoAnterior,
				imagemAnterior,
				linkAnterior,
				Instant.now().toString());
	}

	@Modifying
	@Query("""
			UPDATE projeto
			SET titulo = :titulo,
				descricao = :descricao,
				imagem_url = :imagemUrl,
				link = :link,
				updated_at = :updatedAt
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
	int atualizarSeEstadoAtualComData(
			UUID id,
			String titulo,
			String descricao,
			String imagemUrl,
			String link,
			String tituloAnterior,
			String descricaoAnterior,
			String imagemAnterior,
			String linkAnterior,
			String updatedAt);

	@Query("""
			SELECT id, titulo, descricao, imagem_url, link
			FROM projeto
			ORDER BY updated_at DESC, id DESC
			""")
	List<Projeto> findAllOrderByAtualizacaoRecente();

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
