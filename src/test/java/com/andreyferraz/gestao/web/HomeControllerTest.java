package com.andreyferraz.gestao.web;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@WithMockUser(username = "admin")
	void dashboard_deveAbrirComResumoAtivoEIndicadoresDentroDaAba() throws Exception {
		String html = mockMvc.perform(get("/dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("home/dashboard"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertTrue(html.matches("(?s).*<button[^>]*class=\"[^\"]*active[^\"]*\""
				+ "[^>]*data-tab-target=\"tab-resumo\"[^>]*>.*"),
				"O botão da aba Resumo deve estar ativo.");

		int inicioResumo = html.indexOf("<article id=\"tab-resumo\" class=\"panel tab-panel active\">");
		int inicioClientes = html.indexOf("<article id=\"tab-clientes\"");
		assertTrue(inicioResumo >= 0 && inicioClientes > inicioResumo,
				"O painel Resumo ativo deve ser o primeiro painel.");

		String painelResumo = html.substring(inicioResumo, inicioClientes);
		assertTrue(painelResumo.contains("<p id=\"kpi-clientes\">"));
		assertTrue(painelResumo.contains("<canvas id=\"resumo-grafico\""));
		assertTrue(painelResumo.contains("<ul id=\"resumo-clientes-lista\""));
		assertTrue(painelResumo.contains("<ul id=\"resumo-leads-lista\""));
	}

	@Test
	@WithMockUser(username = "admin")
	void dashboard_deveExporAdministracaoDeProjetosEmPainelUnico() throws Exception {
		String html = mockMvc.perform(get("/dashboard"))
				.andExpect(status().isOk())
				.andExpect(view().name("home/dashboard"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		assertTrue(html.contains("data-tab-target=\"tab-projetos\""));

		int inicio = html.indexOf("<article id=\"tab-projetos\"");
		int fim = html.indexOf("</article>", inicio);
		assertTrue(inicio >= 0 && fim > inicio, "O painel Projetos deve existir.");

		String painel = html.substring(inicio, fim);
		assertTrue(painel.contains("id=\"projeto-form\""));
		assertTrue(painel.contains("id=\"projeto-titulo\""));
		assertTrue(painel.contains("id=\"projeto-descricao\""));
		assertTrue(painel.contains("id=\"projeto-link\""));
		assertTrue(painel.contains("id=\"projeto-imagem\""));
		assertTrue(painel.contains("id=\"projeto-salvar\""));
		assertTrue(painel.contains("id=\"projeto-cancelar\""));
		assertTrue(painel.contains("id=\"projetos-lista\""));
		assertTrue(html.contains("/js/projetos.js"));
	}
}
