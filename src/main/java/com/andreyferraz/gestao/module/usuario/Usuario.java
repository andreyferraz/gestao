package com.andreyferraz.gestao.module.usuario;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("usuarios")
public class Usuario {

	@Id
	private UUID id;

	private String username;

	private String senha;

	private Integer ativo;

	private String role;
}
