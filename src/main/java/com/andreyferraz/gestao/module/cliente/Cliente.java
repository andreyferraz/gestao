package com.andreyferraz.gestao.module.cliente;

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
@Table("cliente")
public class Cliente {

	@Id
	private UUID id;

	private String nome;

	@Column("dominio_aplicacao")
	private String dominioAplicacao;

	@Column("data_vencimento_dominio")
	private LocalDate dataVencimentoDominio;

	private Boolean ativo;

}
