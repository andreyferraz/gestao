package com.andreyferraz.gestao.module.website.projeto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ProjetoRequest {

    @NotBlank(message = "Título do projeto é obrigatório.")
    private String titulo;

    @NotBlank(message = "Descrição do projeto é obrigatória.")
    private String descricao;

    @NotBlank(message = "Link do projeto é obrigatório.")
    @Pattern(
            regexp = "(?i)^https?://.+",
            message = "Link do projeto deve usar HTTP ou HTTPS.")
    private String link;

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }
}
