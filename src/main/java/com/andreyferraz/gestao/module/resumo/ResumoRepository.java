package com.andreyferraz.gestao.module.resumo;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ResumoRepository {

	private static final String INDICADORES_SQL = """
			SELECT
				COUNT(*) AS total_clientes,
				COALESCE(SUM(CASE WHEN ativo = 1 THEN valor_mensal ELSE 0 END), 0)
					AS receita_mensal_ativa,
				COALESCE(SUM(CASE WHEN ativo = 1 THEN 1 ELSE 0 END), 0)
					AS dominios_ativos
			FROM cliente
			""";

	private static final String DISTRIBUICAO_SQL = """
			SELECT
				COALESCE(valor_mensal, 0) AS valor_mensal,
				COUNT(*) AS quantidade_clientes
			FROM cliente
			WHERE ativo = 1
			GROUP BY COALESCE(valor_mensal, 0)
			ORDER BY COALESCE(valor_mensal, 0) ASC
			""";

	private static final String ULTIMOS_CLIENTES_SQL = """
			SELECT id, nome, created_at, COALESCE(valor_mensal, 0) AS valor_mensal, ativo
			FROM cliente
			ORDER BY created_at DESC, id DESC
			LIMIT 5
			""";

	private static final String ULTIMOS_LEADS_SQL = """
			SELECT id, nome, created_at,
				COALESCE(orcamento_manutencao_hospedagem, 0)
					AS orcamento_manutencao_hospedagem
			FROM lead
			ORDER BY created_at DESC, id DESC
			LIMIT 5
			""";

	private final JdbcTemplate jdbcTemplate;

	public ResumoRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public ResumoIndicadoresDto buscarIndicadores() {
		return jdbcTemplate.queryForObject(INDICADORES_SQL, (rs, rowNum) ->
				new ResumoIndicadoresDto(
						rs.getLong("total_clientes"),
						rs.getBigDecimal("receita_mensal_ativa"),
						rs.getLong("dominios_ativos")));
	}

	public List<DistribuicaoValorMensalDto> buscarDistribuicaoValoresMensais() {
		return jdbcTemplate.query(DISTRIBUICAO_SQL, (rs, rowNum) ->
				new DistribuicaoValorMensalDto(
						rs.getBigDecimal("valor_mensal"),
						rs.getLong("quantidade_clientes")));
	}

	public List<ClienteRecenteDto> buscarUltimosClientes() {
		return jdbcTemplate.query(ULTIMOS_CLIENTES_SQL, (rs, rowNum) ->
				new ClienteRecenteDto(
						UUID.fromString(rs.getString("id")),
						rs.getString("nome"),
						rs.getString("created_at"),
						rs.getBigDecimal("valor_mensal"),
						rs.getInt("ativo") == 1));
	}

	public List<LeadRecenteDto> buscarUltimosLeads() {
		return jdbcTemplate.query(ULTIMOS_LEADS_SQL, (rs, rowNum) ->
				new LeadRecenteDto(
						UUID.fromString(rs.getString("id")),
						rs.getString("nome"),
						rs.getString("created_at"),
						rs.getBigDecimal("orcamento_manutencao_hospedagem")));
	}
}
