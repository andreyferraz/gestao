package com.andreyferraz.gestao.core.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.andreyferraz.gestao.core.exception.FileUploadException;

import java.awt.image.BufferedImage;

@Service
public class FileUploadService {

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @Value("${upload.webp.quality:0.75}")
    private float webpQuality;

    /**
     * Salva uma imagem no diretório configurado e retorna o nome do arquivo gerado.
        * Sempre converte para WebP.
     *
     * @param imagemFile O arquivo MultipartFile a ser salvo
     * @return O nome do arquivo gerado
     * @throws FileUploadException Se não conseguir salvar o arquivo
     */
    public String salvarImagem(MultipartFile imagemFile) {
        try {
            // Cria o diretório de upload se ele não existir
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Gera um nome de arquivo único para evitar colisões
            String nomeOriginal = imagemFile.getOriginalFilename();
            if (nomeOriginal == null || nomeOriginal.isEmpty()) {
                throw new FileUploadException("Nome do arquivo não pode ser nulo ou vazio");
            }

            String nomeArquivoBase = UUID.randomUUID().toString();
            String nomeArquivoWebp = nomeArquivoBase + ".webp";
            Path caminhoArquivoWebp = uploadPath.resolve(nomeArquivoWebp);
            converterESalvarWebpComFallback(imagemFile, caminhoArquivoWebp);
            return nomeArquivoWebp;

        } catch (IOException e) {
            throw new FileUploadException("Não foi possível salvar a imagem. Erro: " + e.getMessage(), e);
        } catch (FileUploadException e) {
            // Preserve our domain exception so caller sees the detailed cause
            throw e;
        } catch (RuntimeException e) {
            throw new FileUploadException("Falha ao processar imagem para WebP: " + e.getMessage(), e);
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
        try {
            if (nomeArquivo != null && !nomeArquivo.isEmpty()) {
                Path caminhoArquivo = Paths.get(uploadDir, nomeArquivo);
                Files.deleteIfExists(caminhoArquivo);
            }
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
        if (nomeArquivo == null || nomeArquivo.isEmpty()) {
            return false;
        }
        Path caminhoArquivo = Paths.get(uploadDir, nomeArquivo);
        return Files.exists(caminhoArquivo);
    }

    /**
     * Retorna o caminho completo para um arquivo no diretório de upload.
     * 
     * @param nomeArquivo O nome do arquivo
     * @return O caminho completo como Path
     */
    public Path getCaminhoCompleto(String nomeArquivo) {
        return Paths.get(uploadDir, nomeArquivo);
    }

    private void converterESalvarWebp(MultipartFile imagemFile, Path caminhoArquivo) throws IOException {
        // Garante que plugins de ImageIO sejam descobertos no runtime.
        ImageIO.scanForPlugins();

        BufferedImage bufferedImage;
        try (InputStream inputStream = imagemFile.getInputStream()) {
            bufferedImage = ImageIO.read(inputStream);
        }

        if (bufferedImage == null) {
            // Alguns JPEGs (CMYK/progressivos) podem não ser lidos pelo ImageIO.
            // Tentar fallback usando o utilitário cwebp via CLI, que suporta mais formatos.
            converterESalvarWebpViaCli(imagemFile, caminhoArquivo, new RuntimeException("ImageIO failed to read image, using CLI fallback"));
            return;
        }

        var optWriter = obterWriterWebp();
        if (optWriter.isEmpty()) {
            // If no ImageIO WebP writer, try CLI fallback (cwebp)
            converterESalvarWebpViaCli(imagemFile, caminhoArquivo,
                new RuntimeException("No ImageIO WebP writer available. Writers detected: " + listarWritersDisponiveis()));
            return;
        }
        ImageWriter writer = optWriter.get();

        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            if (writeParam.getCompressionTypes() != null && writeParam.getCompressionTypes().length > 0) {
                writeParam.setCompressionType(writeParam.getCompressionTypes()[0]);
            }
            writeParam.setCompressionQuality(Math.max(0.0f, Math.min(1.0f, webpQuality)));
        }

        try (ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(Files.newOutputStream(caminhoArquivo))) {
            writer.setOutput(imageOutputStream);
            writer.write(null, new IIOImage(bufferedImage, null, null), writeParam);
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError ex) {
            converterESalvarWebpViaCli(imagemFile, caminhoArquivo, ex);
        } finally {
            writer.dispose();
        }
    }

    private void converterESalvarWebpComFallback(MultipartFile imagemFile, Path caminhoArquivo) throws IOException {
        try {
            converterESalvarWebp(imagemFile, caminhoArquivo);
        } catch (UnsatisfiedLinkError | ExceptionInInitializerError | NoClassDefFoundError ex) {
            converterESalvarWebpViaCli(imagemFile, caminhoArquivo, ex);
        }
    }

    private void converterESalvarWebpViaCli(MultipartFile imagemFile, Path caminhoArquivo, Throwable causaOriginal)
            throws IOException {
        // Preserve original file extension for cwebp detection when creating temp file
        String originalName = imagemFile.getOriginalFilename();
        String suffix = ".img";
        if (originalName != null && originalName.contains(".")) {
            suffix = originalName.substring(originalName.lastIndexOf('.'));
        }

        Path arquivoTemporarioEntrada = Files.createTempFile("upload-webp-in-", suffix);
        try {
            imagemFile.transferTo(arquivoTemporarioEntrada);

            String qualidade = String.valueOf(Math.round(Math.max(0.0f, Math.min(1.0f, webpQuality)) * 100));
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "cwebp",
                    "-q",
                    qualidade,
                    arquivoTemporarioEntrada.toString(),
                    "-o",
                    caminhoArquivo.toString());
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            String saida = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            int exitCode = aguardarProcesso(process);

            if (exitCode != 0 || !Files.exists(caminhoArquivo)) {
                throw new FileUploadException(
                        "Falha ao converter imagem para WebP. O encoder Java falhou por incompatibilidade de arquitetura e o cwebp tambem falhou. "
                                + "Saida do cwebp: " + saida,
                        causaOriginal);
            }
        } catch (IOException ioEx) {
            if (!comandoDisponivel("cwebp")) {
                throw new FileUploadException(
                        "Falha ao converter imagem para WebP por incompatibilidade de arquitetura do encoder Java. "
                                + "Instale o cwebp no macOS com: brew install webp",
                        causaOriginal);
            }
            throw ioEx;
        } finally {
            try { Files.deleteIfExists(arquivoTemporarioEntrada); } catch (Exception e) { /* ignore */ }
        }
    }

    private boolean comandoDisponivel(String comando) {
        try {
            Process process = new ProcessBuilder("sh", "-c", "command -v " + comando).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (IOException ex) {
            return false;
        }
    }

    private int aguardarProcesso(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FileUploadException("Conversao para WebP interrompida.", e);
        }
    }

    private Optional<ImageWriter> obterWriterWebp() {
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

    private String listarWritersDisponiveis() {
        return Stream.of(ImageIO.getWriterFormatNames())
                .map(String::toLowerCase)
                .distinct()
                .sorted()
                .collect(Collectors.joining(", "));
    }

}
