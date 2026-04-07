package com.andreyferraz.gestao.module.chamado;

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
@Table("chamado")
public class Chamado {

	public enum Status {
		ABERTO,
		RESOLVIDO
	}

	@Id
	private UUID id;

	@Column("cliente_id")
	private UUID clienteId;

	@Column("descricao_problema")
	private String descricaoProblema;

	private Status status;
}
