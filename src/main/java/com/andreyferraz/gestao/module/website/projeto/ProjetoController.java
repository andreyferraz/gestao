package com.andreyferraz.gestao.module.website.projeto;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.andreyferraz.gestao.core.service.FileUploadService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projetos")
public class ProjetoController {

    private static final MediaType IMAGE_WEBP = MediaType.parseMediaType("image/webp");

    private final ProjetoService projetoService;
    private final FileUploadService fileUploadService;
    private final ProjetoDescricaoSanitizer descricaoSanitizer;

    public ProjetoController(
            ProjetoService projetoService,
            FileUploadService fileUploadService,
            ProjetoDescricaoSanitizer descricaoSanitizer) {
        this.projetoService = projetoService;
        this.fileUploadService = fileUploadService;
        this.descricaoSanitizer = descricaoSanitizer;
    }

    @GetMapping
    public ResponseEntity<List<ProjetoResponse>> listar() {
        List<ProjetoResponse> projetos = projetoService.listarProjetos().stream()
                .map(this::paraResponse)
                .toList();
        return ResponseEntity.ok(projetos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjetoResponse> buscarPorId(@PathVariable UUID id) {
        return ResponseEntity.ok(paraResponse(projetoService.buscarProjetoPorId(id)));
    }

    @GetMapping("/imagens/{arquivo}")
    public ResponseEntity<Resource> obterImagem(@PathVariable String arquivo) {
        Path caminho = fileUploadService.getCaminhoCompleto(arquivo);
        if (!Files.isRegularFile(caminho, LinkOption.NOFOLLOW_LINKS)) {
            throw new NoSuchElementException("Imagem não encontrada.");
        }
        return ResponseEntity.ok()
                .contentType(IMAGE_WEBP)
                .body(new PathResource(caminho));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjetoResponse> criar(
            @Valid @ModelAttribute ProjetoRequest request,
            @RequestParam(name = "imagem", required = false) MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new IllegalArgumentException("Imagem é obrigatória.");
        }
        Projeto criado = projetoService.criarProjeto(paraProjeto(request), imagem);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/projetos/{id}")
                .buildAndExpand(criado.getId())
                .toUri();
        return ResponseEntity.created(location).body(paraResponse(criado));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjetoResponse> editar(
            @PathVariable UUID id,
            @Valid @ModelAttribute ProjetoRequest request,
            @RequestParam(name = "imagem", required = false) MultipartFile imagem) {
        Projeto editado = projetoService.editarProjeto(id, paraProjeto(request), imagem);
        return ResponseEntity.ok(paraResponse(editado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable UUID id) {
        projetoService.deletarProjeto(id);
        return ResponseEntity.noContent().build();
    }

    private Projeto paraProjeto(ProjetoRequest request) {
        return new Projeto(
                null,
                request.getTitulo(),
                request.getDescricao(),
                null,
                request.getLink());
    }

    private ProjetoResponse paraResponse(Projeto projeto) {
        String imagemUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/projetos/imagens/{arquivo}")
                .buildAndExpand(projeto.getImagemUrl())
                .toUriString();
        return new ProjetoResponse(
                projeto.getId(),
                projeto.getTitulo(),
                descricaoSanitizer.sanitizar(projeto.getDescricao()),
                imagemUrl,
                projeto.getLink());
    }
}
