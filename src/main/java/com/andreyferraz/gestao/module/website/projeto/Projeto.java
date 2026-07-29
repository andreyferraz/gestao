package com.andreyferraz.gestao.module.website.projeto;

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
@Table("projeto")
public class Projeto {

    @Id
	private UUID id;

    @Column("titulo")
    private String titulo;

    @Column("descricao")
    private String descricao;

    @Column("imagem_url")
    private String imagemUrl;

    @Column("link")
    private String link;

}
