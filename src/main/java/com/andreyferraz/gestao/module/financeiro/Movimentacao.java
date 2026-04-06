package com.andreyferraz.gestao.module.financeiro;

import java.math.BigDecimal;
import java.time.LocalDate;
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
@Table("movimentacao")
public class Movimentacao {

	public enum Tipo {
		ENTRADA,
		SAIDA
	}

	@Id
	private UUID id;

	private Tipo tipo;

	private BigDecimal valor;

	@Column("data_ocorrencia")
	private LocalDate dataOcorrencia;

	private String descricao;

	@Column("cliente_id")
	private UUID clienteId;

}
