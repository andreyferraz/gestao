package com.andreyferraz.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:forwarded-headers-cors",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.sql.init.mode=always",
                "server.servlet.context-path=/gestao",
                "app.bootstrap.admin.password=senha-de-teste"
        })
@ActiveProfiles("prod")
class ForwardedHeadersCorsIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void postDoMesmoDominioEncaminhadoPeloNginx_deveUltrapassarCors()
            throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://127.0.0.1:" + port + "/gestao/api/projetos"))
                .header("Origin", "https://andreyferraz.com.br")
                .header("X-Forwarded-Host", "andreyferraz.com.br")
                .header("X-Forwarded-Proto", "https")
                .header("X-Forwarded-Port", "443")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                request,
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate"))
                .contains("Basic realm=\"gestao\"");
    }
}
