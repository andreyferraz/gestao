package com.andreyferraz.gestao.module.lead;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("lead")
public class Lead {

	@Id
	private UUID id;

	private String nome;

	private String telefone;

	@Column("orcamento_desenvolvimento")
	private BigDecimal orcamentoDesenvolvimento;

	@Column("orcamento_manutencao_hospedagem")
	private BigDecimal orcamentoManutencaoHospedagem;

	private String observacoes;
}
