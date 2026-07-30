package com.andreyferraz.gestao.module.website.projeto;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class ProjetoDescricaoSanitizer {

    private static final String MENSAGEM_OBRIGATORIA =
            "Descrição do projeto é obrigatória.";

    private static final Safelist SAFELIST = new Safelist()
            .addTags(
                    "p", "br", "h2", "h3", "strong", "b", "em", "i", "u",
                    "ol", "ul", "li", "blockquote", "a")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "http", "https")
            .addEnforcedAttribute("a", "target", "_blank")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    private static final Document.OutputSettings OUTPUT_SETTINGS =
            new Document.OutputSettings().prettyPrint(false);

    public String sanitizar(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            throw new IllegalArgumentException(MENSAGEM_OBRIGATORIA);
        }

        String limpa = Jsoup.clean(descricao, "", SAFELIST, OUTPUT_SETTINGS);
        if (!possuiTextoVisivel(limpa)) {
            throw new IllegalArgumentException(MENSAGEM_OBRIGATORIA);
        }
        return limpa;
    }

    private boolean possuiTextoVisivel(String html) {
        String texto = Jsoup.parseBodyFragment(html).text();
        return texto.codePoints().anyMatch(codePoint ->
                !Character.isWhitespace(codePoint)
                        && !Character.isSpaceChar(codePoint)
                        && Character.getType(codePoint) != Character.FORMAT);
    }
}
