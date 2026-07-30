package com.andreyferraz.gestao.core.service;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.andreyferraz.gestao.core.exception.FileUploadException;

@Service
public class FileUploadService {

    private static final Pattern NOME_IMAGEM = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.webp$");
    private static final Set<String> EXTENSOES_SUPORTADAS =
            Set.of(".png", ".jpg", ".jpeg", ".webp");

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${upload.webp.quality:0.75}")
    private float webpQuality;

    private final Supplier<UUID> uuidSupplier;

    public FileUploadService() {
        this(UUID::randomUUID);
    }

    FileUploadService(Supplier<UUID> uuidSupplier) {
        this.uuidSupplier = uuidSupplier;
    }

    /**
     * Salva uma imagem no diretório configurado e retorna o nome do arquivo gerado.
        * Sempre converte para WebP.
     *
     * @param imagemFile O arquivo MultipartFile a ser salvo
     * @return O nome do arquivo gerado
     * @throws FileUploadException Se não conseguir salvar o arquivo
     */
    public String salvarImagem(MultipartFile imagemFile) {
        validarArquivo(imagemFile);

        BufferedImage imagem = decodificarImagem(imagemFile);
        String nomeArquivo = uuidSupplier.get() + ".webp";

        try {
            Path raiz = raizUpload();
            Files.createDirectories(raiz);
            Path destino = resolverArquivoSeguro(nomeArquivo);
            Path temporario = Files.createTempFile(raiz, ".upload-webp-", ".tmp");
            converterEPromover(imagem, temporario, destino);
            return nomeArquivo;
        } catch (IOException e) {
            throw new FileUploadException(
                    "Não foi possível salvar a imagem. Erro: " + e.getMessage(), e);
        } catch (FileUploadException e) {
            throw e;
        } catch (LinkageError e) {
            throw new FileUploadException(
                    "Falha ao inicializar o encoder WebP.", e);
        } catch (RuntimeException e) {
            throw new FileUploadException(
                    "Falha ao processar imagem para WebP: " + e.getMessage(), e);
        }
    }

    /**
     * Remove uma imagem do diretório de upload.
     *
     * @param nomeArquivo O nome do arquivo a ser removido
     * @throws FileUploadException Se não conseguir remover o arquivo
     */
    public void removerImagem(String nomeArquivo) {
        removerArquivo(nomeArquivo);
    }

    /**
     * Remove um arquivo do diretório de upload.
     *
     * @param nomeArquivo O nome do arquivo a ser removido
     * @throws FileUploadException Se não conseguir remover o arquivo
     */
    public void removerArquivo(String nomeArquivo) {
        Path caminhoArquivo = resolverArquivoSeguro(nomeArquivo);
        try {
            Files.deleteIfExists(caminhoArquivo);
        } catch (IOException e) {
            throw new FileUploadException("Não foi possível remover a imagem. Erro: " + e.getMessage(), e);
        }
    }

    /**
     * Verifica se um arquivo existe no diretório de upload.
     *
     * @param nomeArquivo O nome do arquivo a ser verificado
     * @return true se o arquivo existir, false caso contrário
     */
    public boolean arquivoExiste(String nomeArquivo) {
        Path caminhoArquivo = resolverArquivoSeguro(nomeArquivo);
        return Files.isRegularFile(caminhoArquivo, LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * Retorna o caminho completo para um arquivo no diretório de upload.
     *
     * @param nomeArquivo O nome do arquivo
     * @return O caminho completo como Path
     */
    public Path getCaminhoCompleto(String nomeArquivo) {
        return resolverArquivoSeguro(nomeArquivo);
    }

    private void validarArquivo(MultipartFile imagemFile) {
        if (imagemFile == null || imagemFile.isEmpty()) {
            throw new IllegalArgumentException("A imagem não pode estar vazia.");
        }

        String nomeOriginal = imagemFile.getOriginalFilename();
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new IllegalArgumentException("O nome original da imagem é obrigatório.");
        }

        int inicioExtensao = nomeOriginal.lastIndexOf('.');
        if (inicioExtensao <= 0 || inicioExtensao == nomeOriginal.length() - 1) {
            throw new IllegalArgumentException("A extensão da imagem é obrigatória.");
        }

        String extensao = nomeOriginal.substring(inicioExtensao).toLowerCase(Locale.ROOT);
        if (!EXTENSOES_SUPORTADAS.contains(extensao)) {
            throw new IllegalArgumentException("Extensão de imagem não suportada.");
        }
    }

    private BufferedImage decodificarImagem(MultipartFile imagemFile) {
        ImageIO.scanForPlugins();

        byte[] conteudo;
        try {
            conteudo = imagemFile.getBytes();
        } catch (IOException e) {
            throw new FileUploadException("Não foi possível ler a imagem enviada.", e);
        }

        BufferedImage imagem;
        try {
            imagem = lerImagem(conteudo);
        } catch (IOException | RuntimeException e) {
            throw new IllegalArgumentException("O arquivo enviado não é uma imagem válida.", e);
        }
        if (imagem == null) {
            throw new IllegalArgumentException("O arquivo enviado não é uma imagem válida.");
        }
        return imagem;
    }

    BufferedImage lerImagem(byte[] conteudo) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(conteudo)) {
            return ImageIO.read(inputStream);
        }
    }

    private void converterEPromover(BufferedImage imagem, Path temporario, Path destino) throws IOException {
        Throwable falha = null;
        try {
            salvarComoWebp(imagem, temporario);
            Files.move(temporario, destino);
        } catch (IOException | RuntimeException | Error e) {
            falha = e;
            throw e;
        } finally {
            removerTemporario(temporario, falha);
        }
    }

    private void salvarComoWebp(BufferedImage imagem, Path caminhoArquivo) throws IOException {
        var optWriter = obterWriterWebp();
        if (optWriter.isEmpty()) {
            throw new FileUploadException("Nenhum encoder WebP está disponível no runtime.");
        }
        ImageWriter writer = optWriter.get();

        Throwable falha = null;
        try {
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                if (writeParam.getCompressionTypes() != null && writeParam.getCompressionTypes().length > 0) {
                    writeParam.setCompressionType(writeParam.getCompressionTypes()[0]);
                }
                writeParam.setCompressionQuality(Math.max(0.0f, Math.min(1.0f, webpQuality)));
            }

            try (OutputStream outputStream = Files.newOutputStream(
                    caminhoArquivo,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    LinkOption.NOFOLLOW_LINKS);
                    ImageOutputStream imageOutputStream =
                            ImageIO.createImageOutputStream(outputStream)) {
                if (imageOutputStream == null) {
                    throw new FileUploadException("Não foi possível abrir o destino da imagem.");
                }
                writer.setOutput(imageOutputStream);
                writer.write(null, new IIOImage(imagem, null, null), writeParam);
            }
        } catch (IOException | RuntimeException | Error e) {
            falha = e;
            throw e;
        } finally {
            descartarWriter(writer, falha);
        }
    }

    private void descartarWriter(ImageWriter writer, Throwable falhaOriginal) {
        try {
            writer.dispose();
        } catch (RuntimeException | Error falhaDispose) {
            if (falhaOriginal != null) {
                falhaOriginal.addSuppressed(falhaDispose);
                return;
            }
            throw falhaDispose;
        }
    }

    private void removerTemporario(Path temporario, Throwable falhaOriginal) throws IOException {
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException | RuntimeException falhaLimpeza) {
            if (falhaOriginal != null) {
                falhaOriginal.addSuppressed(falhaLimpeza);
                return;
            }
            throw falhaLimpeza;
        }
    }

    Optional<ImageWriter> obterWriterWebp() {
        Iterator<ImageWriter> byFormat = ImageIO.getImageWritersByFormatName("webp");
        if (byFormat.hasNext()) {
            return Optional.of(byFormat.next());
        }

        Iterator<ImageWriter> bySuffix = ImageIO.getImageWritersBySuffix("webp");
        if (bySuffix.hasNext()) {
            return Optional.of(bySuffix.next());
        }

        Iterator<ImageWriter> byMime = ImageIO.getImageWritersByMIMEType("image/webp");
        if (byMime.hasNext()) {
            return Optional.of(byMime.next());
        }

        return Optional.empty();
    }

    private Path resolverArquivoSeguro(String nomeArquivo) {
        if (nomeArquivo == null || !NOME_IMAGEM.matcher(nomeArquivo).matches()) {
            throw new IllegalArgumentException("Nome de imagem inválido.");
        }

        Path raiz = raizUpload();
        Path candidato = raiz.resolve(nomeArquivo).normalize();
        if (!candidato.startsWith(raiz)) {
            throw new IllegalArgumentException("Caminho de imagem inválido.");
        }
        return candidato;
    }

    private Path raizUpload() {
        return Paths.get(uploadDir).toAbsolutePath().normalize();
    }

}
