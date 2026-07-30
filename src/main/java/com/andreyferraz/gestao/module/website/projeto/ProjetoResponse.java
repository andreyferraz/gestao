package com.andreyferraz.gestao.module.website.projeto;

import java.util.UUID;

public record ProjetoResponse(
        UUID id,
        String titulo,
        String descricao,
        String imagemUrl,
        String link) {
}
