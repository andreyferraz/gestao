package com.andreyferraz.gestao.module.vendedor;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("vendedor")
public class Vendedor {
	@Id
	private UUID id;

	private String nome;

	private String telefone;

	private String email;

	private Integer ativo;

}