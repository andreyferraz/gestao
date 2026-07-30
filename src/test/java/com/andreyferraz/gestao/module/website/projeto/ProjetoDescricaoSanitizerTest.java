package com.andreyferraz.gestao.module.website.projeto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

class ProjetoDescricaoSanitizerTest {

    private final ProjetoDescricaoSanitizer sanitizer =
            new ProjetoDescricaoSanitizer();

    @Test
    void sanitizar_devePreservarSomenteFormatacaoAprovada() {
        String html = """
                <h2>Objetivo</h2><h3>Detalhes</h3>
                <p>Texto <strong>forte</strong> <em>ênfase</em> <u>sublinhado</u></p>
                <ol><li>Primeiro</li></ol><ul><li>Item</li></ul>
                <blockquote>Citação</blockquote>
                """;

        String limpo = sanitizer.sanitizar(html);
        Document documento = Jsoup.parseBodyFragment(limpo);

        assertEquals("Objetivo", documento.selectFirst("h2").text());
        assertEquals("Detalhes", documento.selectFirst("h3").text());
        assertEquals("forte", documento.selectFirst("strong").text());
        assertEquals("ênfase", documento.selectFirst("em").text());
        assertEquals("sublinhado", documento.selectFirst("u").text());
        assertEquals(1, documento.select("ol > li").size());
        assertEquals(1, documento.select("ul > li").size());
        assertEquals("Citação", documento.selectFirst("blockquote").text());
    }

    @Test
    void sanitizar_deveRemoverScriptEventosEstilosClassesEMidia() {
        String html = """
                <p class="ql-align-center" style="color:red" onclick="alert(1)">
                  Texto<script>alert(2)</script><img src="x"><video src="x"></video>
                </p>
                """;

        String limpo = sanitizer.sanitizar(html);

        assertFalse(limpo.contains("script"));
        assertFalse(limpo.contains("onclick"));
        assertFalse(limpo.contains("style"));
        assertFalse(limpo.contains("class"));
        assertFalse(limpo.contains("img"));
        assertFalse(limpo.contains("video"));
        assertTrue(limpo.contains("Texto"));
    }

    @Test
    void sanitizar_deveAceitarSomenteLinksHttpEAdicionarAtributosSeguros() {
        String limpo = sanitizer.sanitizar("""
                <p>
                  <a href="https://example.com">Seguro</a>
                  <a href="javascript:alert(1)">Perigoso</a>
                  <a href="data:text/html,abc">Data</a>
                </p>
                """);
        Document documento = Jsoup.parseBodyFragment(limpo);
        Element seguro = documento.select("a").get(0);
        Element perigoso = documento.select("a").get(1);
        Element data = documento.select("a").get(2);

        assertEquals("https://example.com", seguro.attr("href"));
        assertEquals("_blank", seguro.attr("target"));
        assertEquals("noopener noreferrer", seguro.attr("rel"));
        assertFalse(perigoso.hasAttr("href"));
        assertFalse(data.hasAttr("href"));
    }

    @Test
    void sanitizar_deveRejeitarHtmlSemTextoVisivel() {
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> sanitizer.sanitizar("<p><br></p><script>alert(1)</script>"));

        assertEquals("Descrição do projeto é obrigatória.", erro.getMessage());
    }

    @Test
    void sanitizar_deveRejeitarEspacosNaoVisiveis() {
        IllegalArgumentException erro = assertThrows(
                IllegalArgumentException.class,
                () -> sanitizer.sanitizar("<p>&nbsp;\u200B\uFEFF</p>"));

        assertEquals("Descrição do projeto é obrigatória.", erro.getMessage());
    }

    @Test
    void sanitizar_deveManterDescricaoAntigaEmTextoPuro() {
        assertEquals(
                "Descrição antiga sem HTML",
                sanitizer.sanitizar("Descrição antiga sem HTML"));
    }
}
