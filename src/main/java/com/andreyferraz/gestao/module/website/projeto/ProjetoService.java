package com.andreyferraz.gestao.module.website.projeto;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import com.andreyferraz.gestao.core.service.FileUploadService;

@Service
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final FileUploadService fileUploadService;
    private final ProjetoDescricaoSanitizer descricaoSanitizer;

    public ProjetoService(
            ProjetoRepository projetoRepository,
            FileUploadService fileUploadService,
            ProjetoDescricaoSanitizer descricaoSanitizer) {
        this.projetoRepository = projetoRepository;
        this.fileUploadService = fileUploadService;
        this.descricaoSanitizer = descricaoSanitizer;
    }

    @Transactional
    public Projeto criarProjeto(Projeto projeto, MultipartFile imagem) {
        validarProjeto(projeto);
        validarImagemObrigatoria(imagem);

        String nomeImagem = fileUploadService.salvarImagem(imagem);
        projeto.setId(UUID.randomUUID());
        projeto.setImagemUrl(nomeImagem);
        try {
            projetoRepository.inserir(
                    projeto.getId(), projeto.getTitulo(), projeto.getDescricao(),
                    projeto.getImagemUrl(), projeto.getLink());
            registrarCompensacaoEmCasoDeRollback(nomeImagem);
            return projeto;
        } catch (RuntimeException ex) {
            removerImagemSemOcultarErro(nomeImagem, ex);
            throw ex;
        }
    }

    @Transactional
    public Projeto editarProjeto(UUID id, Projeto projetoAtualizado, MultipartFile imagem) {
        validarId(id);
        validarProjeto(projetoAtualizado);
        Projeto projetoExistente = obterProjeto(id);

        String imagemAnterior = projetoExistente.getImagemUrl();
        boolean possuiNovaImagem = imagem != null && !imagem.isEmpty();
        if (!possuiNovaImagem && !possuiNomeDeImagem(imagemAnterior)) {
            throw new IllegalArgumentException(
                    "Projeto legado sem imagem. Envie uma nova imagem para concluir a edição.");
        }
        String imagemAtual = possuiNovaImagem
                ? fileUploadService.salvarImagem(imagem)
                : imagemAnterior;

        Projeto projetoEditado = new Projeto(
                id,
                projetoAtualizado.getTitulo(),
                projetoAtualizado.getDescricao(),
                imagemAtual,
                projetoAtualizado.getLink());
        try {
            int linhasAtualizadas = projetoRepository.atualizarSeEstadoAtual(
                    id,
                    projetoEditado.getTitulo(),
                    projetoEditado.getDescricao(),
                    projetoEditado.getImagemUrl(),
                    projetoEditado.getLink(),
                    projetoExistente.getTitulo(),
                    projetoExistente.getDescricao(),
                    imagemAnterior,
                    projetoExistente.getLink());
            exigirUmaLinhaAlterada(linhasAtualizadas, id);
        } catch (RuntimeException ex) {
            if (possuiNovaImagem) {
                removerImagemSemOcultarErro(imagemAtual, ex);
            }
            throw ex;
        }
        if (possuiNovaImagem) {
            registrarSubstituicaoDeImagem(imagemAnterior, imagemAtual);
        }
        return projetoEditado;
    }

    @Transactional
    public void deletarProjeto(UUID id) {
        validarId(id);
        Projeto projetoExistente = obterProjeto(id);
        int linhasExcluidas = projetoRepository.deletarSeEstadoAtual(
                id,
                projetoExistente.getTitulo(),
                projetoExistente.getDescricao(),
                projetoExistente.getImagemUrl(),
                projetoExistente.getLink());
        exigirUmaLinhaAlterada(linhasExcluidas, id);
        removerImagemAposConfirmacao(projetoExistente.getImagemUrl());
    }

    @Transactional(readOnly = true)
    public List<Projeto> listarProjetos() {
        return StreamSupport.stream(projetoRepository.findAll().spliterator(), false)
                .toList();
    }

    @Transactional(readOnly = true)
    public Projeto buscarProjetoPorId(UUID id) {
        validarId(id);
        return obterProjeto(id);
    }

    private void validarProjeto(Projeto projeto) {
        if (projeto == null) {
            throw new IllegalArgumentException("Projeto é obrigatório.");
        }
        validarTextoObrigatorio(projeto.getTitulo(), "Título do projeto é obrigatório.");
        projeto.setDescricao(descricaoSanitizer.sanitizar(projeto.getDescricao()));
        validarLink(projeto.getLink());
    }

    private void validarId(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("ID do projeto é obrigatório.");
        }
    }

    private Projeto obterProjeto(UUID id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Projeto não encontrado com o ID: " + id));
    }

    private void exigirUmaLinhaAlterada(int quantidade, UUID id) {
        if (quantidade != 1) {
            throw new OptimisticLockingFailureException(
                    "Projeto alterado ou removido por outra operação. Recarregue os dados e tente novamente: " + id);
        }
    }

    private void validarImagemObrigatoria(MultipartFile imagem) {
        if (imagem == null || imagem.isEmpty()) {
            throw new IllegalArgumentException("Imagem é obrigatória.");
        }
    }

    private void validarTextoObrigatorio(String valor, String mensagem) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensagem);
        }
    }

    private void validarLink(String link) {
        validarTextoObrigatorio(link, "Link do projeto é obrigatório.");
        try {
            URI uri = new URI(link);
            if (!uri.isAbsolute()
                    || uri.isOpaque()
                    || uri.getHost() == null
                    || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException("Link do projeto deve usar HTTP ou HTTPS.");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Link do projeto deve usar HTTP ou HTTPS.", ex);
        }
    }

    private void removerImagemSemOcultarErro(String nomeImagem, RuntimeException erroOriginal) {
        try {
            fileUploadService.removerImagem(nomeImagem);
        } catch (RuntimeException erroRemocao) {
            erroOriginal.addSuppressed(erroRemocao);
        }
    }

    private void registrarCompensacaoEmCasoDeRollback(String nomeImagem) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    removerImagemAposRollback(nomeImagem);
                }
            }
        });
    }

    private void registrarSubstituicaoDeImagem(String imagemAnterior, String imagemNova) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            removerImagemAposCommit(imagemAnterior);
            return;
        }
        try {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    removerImagemAposCommit(imagemAnterior);
                }

                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_ROLLED_BACK) {
                        removerImagemAposRollback(imagemNova);
                    }
                }
            });
        } catch (RuntimeException ex) {
            removerImagemSemOcultarErro(imagemNova, ex);
            throw ex;
        }
    }

    private void removerImagemAposConfirmacao(String nomeImagem) {
        if (!possuiNomeDeImagem(nomeImagem)) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            removerImagemAposCommit(nomeImagem);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                removerImagemAposCommit(nomeImagem);
            }
        });
    }

    private void removerImagemAposCommit(String nomeImagem) {
        if (possuiNomeDeImagem(nomeImagem)) {
            fileUploadService.removerImagem(nomeImagem);
        }
    }

    private boolean possuiNomeDeImagem(String nomeImagem) {
        return nomeImagem != null && !nomeImagem.isBlank();
    }

    private void removerImagemAposRollback(String nomeImagem) {
        try {
            fileUploadService.removerImagem(nomeImagem);
        } catch (RuntimeException ignored) {
            // A falha de compensação não pode ocultar a causa que reverteu a transação.
        }
    }

}
