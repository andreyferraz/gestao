package com.andreyferraz.gestao.core.service;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.andreyferraz.gestao.core.exception.FileUploadException;

class FileUploadServiceTest {

    private static final UUID UUID_FIXO = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    private static final String NOME_VALIDO = UUID_FIXO + ".webp";

    @TempDir
    Path tempDir;

    private FileUploadService service;

    @BeforeEach
    void setUp() {
        service = new FileUploadService();
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "webpQuality", 0.75f);
    }

    @Test
    void getCaminhoCompleto_quandoNomeEscaparDoDiretorio_deveRejeitar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getCaminhoCompleto("../../senha.txt"));
    }

    @Test
    void getCaminhoCompleto_quandoNomeNaoForUuidWebp_deveRejeitar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.getCaminhoCompleto("imagem.webp"));
        assertThrows(IllegalArgumentException.class,
                () -> service.getCaminhoCompleto("123e4567-e89b-12d3-a456-426614174000.png"));
        assertThrows(IllegalArgumentException.class,
                () -> service.getCaminhoCompleto("123e4567-e89b-12d3-a456-426614174000.webp/arquivo"));
    }

    @Test
    void getCaminhoCompleto_quandoNomeForUuidWebp_deveRetornarCaminhoNormalizadoNaRaiz() {
        Path caminho = service.getCaminhoCompleto(NOME_VALIDO);

        assertEquals(tempDir.toAbsolutePath().normalize().resolve(NOME_VALIDO), caminho);
    }

    @Test
    void arquivoExiste_quandoNomeForInvalido_deveRejeitar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.arquivoExiste("../" + NOME_VALIDO));
        assertThrows(IllegalArgumentException.class,
                () -> service.arquivoExiste(null));
    }

    @Test
    void removerImagem_quandoNomeForInvalido_deveRejeitar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.removerImagem("../" + NOME_VALIDO));
        assertThrows(IllegalArgumentException.class,
                () -> service.removerImagem(""));
    }

    @Test
    void salvarImagem_quandoArquivoForVazio_deveRejeitarSemCriarArquivo() throws IOException {
        MockMultipartFile arquivo = new MockMultipartFile(
                "imagem", "imagem.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> service.salvarImagem(arquivo));
        assertDiretorioVazio();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "imagem", "imagem.", "imagem.txt"})
    void salvarImagem_quandoNomeOriginalForInvalido_deveRejeitarAntesDeCriarArquivo(
            String nomeOriginal) throws IOException {
        MockMultipartFile arquivo = new MockMultipartFile(
                "imagem", nomeOriginal, "image/png", pngValido().getBytes());

        assertThrows(IllegalArgumentException.class,
                () -> service.salvarImagem(arquivo));
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoConteudoNaoForImagem_deveRejeitarSemCriarArquivo() throws IOException {
        MockMultipartFile arquivo = new MockMultipartFile(
                "imagem", "arquivo.png", "image/png", "nao e imagem".getBytes(UTF_8));

        assertThrows(IllegalArgumentException.class,
                () -> service.salvarImagem(arquivo));
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoPngEstiverTruncado_deveRejeitarSemCriarArquivo() throws IOException {
        byte[] assinaturaPngTruncada = {
                (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a
        };
        MockMultipartFile arquivo = new MockMultipartFile(
                "imagem", "imagem.png", "image/png", assinaturaPngTruncada);

        assertThrows(IllegalArgumentException.class,
                () -> service.salvarImagem(arquivo));
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoDecoderLancarRuntimeException_deveRejeitarComoConteudoInvalido() throws IOException {
        IllegalStateException falhaDecoder = new IllegalStateException("falha do decoder simulada");
        service = new FileUploadService(() -> UUID_FIXO) {
            @Override
            BufferedImage lerImagem(byte[] conteudo) {
                throw falhaDecoder;
            }
        };
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "webpQuality", 0.75f);

        IllegalArgumentException lancada = assertThrows(IllegalArgumentException.class,
                () -> service.salvarImagem(pngValido()));

        assertEquals(falhaDecoder, lancada.getCause());
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoPngValido_deveConverterParaWebp() throws IOException {
        MockMultipartFile arquivo = pngValido();

        String nome = service.salvarImagem(arquivo);

        assertTrue(nome.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.webp"));
        Path salvo = service.getCaminhoCompleto(nome);
        assertTrue(Files.isRegularFile(salvo));
        assertNotNull(ImageIO.read(salvo.toFile()));
        byte[] cabecalho = Files.readAllBytes(salvo);
        assertTrue(cabecalho.length >= 12);
        assertEquals("RIFF", new String(cabecalho, 0, 4, UTF_8));
        assertEquals("WEBP", new String(cabecalho, 8, 4, UTF_8));
    }

    @Test
    void salvarImagem_quandoDestinoJaExistir_devePreservarArquivoEExcluirSomenteTemporario() throws IOException {
        service = serviceComUuidFixo();
        Path destinoExistente = tempDir.resolve(NOME_VALIDO);
        Files.writeString(destinoExistente, "conteudo preservado");

        assertThrows(FileUploadException.class,
                () -> service.salvarImagem(pngValido()));

        assertEquals("conteudo preservado", Files.readString(destinoExistente));
        assertQuantidadeArquivosNaRaiz(1);
    }

    @Test
    void salvarImagem_quandoDestinoForLinkSimbolico_devePreservarLinkEAlvoExterno() throws IOException {
        service = serviceComUuidFixo();
        Path alvoExterno = Files.createTempFile(tempDir.getParent(), "imagem-externa-", ".txt");
        Files.writeString(alvoExterno, "conteudo externo preservado");
        Path linkDestino = tempDir.resolve(NOME_VALIDO);
        Files.createSymbolicLink(linkDestino, alvoExterno);

        try {
            assertThrows(FileUploadException.class,
                    () -> service.salvarImagem(pngValido()));

            assertTrue(Files.isSymbolicLink(linkDestino));
            assertEquals("conteudo externo preservado", Files.readString(alvoExterno));
            assertQuantidadeArquivosNaRaiz(1);
        } finally {
            Files.deleteIfExists(alvoExterno);
        }
    }

    @Test
    void salvarImagem_quandoEncoderLancarError_deveExcluirArquivoDaOperacao() throws IOException {
        AssertionError falhaEncoder = new AssertionError("falha nativa simulada");
        AtomicBoolean writerDescartado = new AtomicBoolean();
        service = new FileUploadService(() -> UUID_FIXO) {
            @Override
            Optional<ImageWriter> obterWriterWebp() {
                return Optional.of(writerComFalhaNaEscrita(falhaEncoder, writerDescartado));
            }
        };
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "webpQuality", 0.75f);

        AssertionError lancada = assertThrows(AssertionError.class,
                () -> service.salvarImagem(pngValido()));

        assertEquals(falhaEncoder, lancada);
        assertTrue(writerDescartado.get());
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoConfiguracaoDoWriterFalhar_deveDescartarWriterELimparTemporario() throws IOException {
        IllegalStateException falhaConfiguracao = new IllegalStateException("configuracao invalida simulada");
        AtomicBoolean writerDescartado = new AtomicBoolean();
        service = new FileUploadService(() -> UUID_FIXO) {
            @Override
            Optional<ImageWriter> obterWriterWebp() {
                return Optional.of(writerComFalhaNaConfiguracao(falhaConfiguracao, writerDescartado));
            }
        };
        ReflectionTestUtils.setField(service, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(service, "webpQuality", 0.75f);

        assertThrows(FileUploadException.class,
                () -> service.salvarImagem(pngValido()));

        assertTrue(writerDescartado.get());
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoConfiguracaoEDisposeFalharem_devePreservarFalhaPrincipal() throws IOException {
        IllegalStateException falhaConfiguracao = new IllegalStateException("configuracao invalida simulada");
        IllegalArgumentException falhaDispose = new IllegalArgumentException("dispose falhou");
        service = serviceComWriter(
                writerComFalhaNaConfiguracaoEDispose(falhaConfiguracao, falhaDispose));

        FileUploadException lancada = assertThrows(FileUploadException.class,
                () -> service.salvarImagem(pngValido()));

        assertEquals(falhaConfiguracao, lancada.getCause());
        assertEquals(1, falhaConfiguracao.getSuppressed().length);
        assertEquals(falhaDispose, falhaConfiguracao.getSuppressed()[0]);
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoWriteEDisposeLancaremError_devePreservarFalhaPrincipal() throws IOException {
        AssertionError falhaWrite = new AssertionError("write falhou");
        AssertionError falhaDispose = new AssertionError("dispose falhou");
        service = serviceComWriter(writerComFalhaNaEscritaEDispose(falhaWrite, falhaDispose));

        AssertionError lancada = assertThrows(AssertionError.class,
                () -> service.salvarImagem(pngValido()));

        assertEquals(falhaWrite, lancada);
        assertEquals(1, falhaWrite.getSuppressed().length);
        assertEquals(falhaDispose, falhaWrite.getSuppressed()[0]);
        assertDiretorioVazio();
    }

    @Test
    void salvarImagem_quandoSomenteDisposeFalhar_devePropagarFalhaELimparTemporario() throws IOException {
        IllegalStateException falhaDispose = new IllegalStateException("dispose falhou");
        service = serviceComWriter(writerComFalhaSomenteNoDispose(falhaDispose));

        FileUploadException lancada = assertThrows(FileUploadException.class,
                () -> service.salvarImagem(pngValido()));

        assertEquals(falhaDispose, lancada.getCause());
        assertDiretorioVazio();
    }

    @Test
    void arquivoExisteERemoverImagem_quandoArquivoForValido_deveUsarMesmoCaminhoSeguro() throws IOException {
        Path arquivo = tempDir.resolve(NOME_VALIDO);
        Files.writeString(arquivo, "conteudo");

        assertTrue(service.arquivoExiste(NOME_VALIDO));

        service.removerImagem(NOME_VALIDO);

        assertFalse(service.arquivoExiste(NOME_VALIDO));
        assertFalse(Files.exists(arquivo));
    }

    private MockMultipartFile pngValido() throws IOException {
        BufferedImage original = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        original.setRGB(0, 0, Color.RED.getRGB());
        original.setRGB(1, 0, Color.GREEN.getRGB());
        original.setRGB(0, 1, Color.BLUE.getRGB());
        original.setRGB(1, 1, Color.WHITE.getRGB());

        ByteArrayOutputStream png = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(original, "png", png));
        return new MockMultipartFile("imagem", "imagem.png", "image/png", png.toByteArray());
    }

    private FileUploadService serviceComUuidFixo() {
        FileUploadService serviceComUuid = new FileUploadService(() -> UUID_FIXO);
        ReflectionTestUtils.setField(serviceComUuid, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(serviceComUuid, "webpQuality", 0.75f);
        return serviceComUuid;
    }

    private FileUploadService serviceComWriter(ImageWriter writer) {
        FileUploadService serviceComWriter = new FileUploadService(() -> UUID_FIXO) {
            @Override
            Optional<ImageWriter> obterWriterWebp() {
                return Optional.of(writer);
            }
        };
        ReflectionTestUtils.setField(serviceComWriter, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(serviceComWriter, "webpQuality", 0.75f);
        return serviceComWriter;
    }

    private ImageWriter writerComFalhaNaEscrita(
            AssertionError falhaEncoder,
            AtomicBoolean writerDescartado) {
        return new WriterTeste() {
            @Override
            public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) {
                throw falhaEncoder;
            }

            @Override
            public void dispose() {
                writerDescartado.set(true);
                super.dispose();
            }
        };
    }

    private ImageWriter writerComFalhaNaConfiguracao(
            IllegalStateException falhaConfiguracao,
            AtomicBoolean writerDescartado) {
        return new WriterTeste() {
            @Override
            public ImageWriteParam getDefaultWriteParam() {
                throw falhaConfiguracao;
            }

            @Override
            public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) {
                throw new AssertionError("write nao deveria ser chamado");
            }

            @Override
            public void dispose() {
                writerDescartado.set(true);
                super.dispose();
            }
        };
    }

    private ImageWriter writerComFalhaNaConfiguracaoEDispose(
            IllegalStateException falhaConfiguracao,
            IllegalArgumentException falhaDispose) {
        return new WriterTeste() {
            @Override
            public ImageWriteParam getDefaultWriteParam() {
                throw falhaConfiguracao;
            }

            @Override
            public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) {
                throw new AssertionError("write nao deveria ser chamado");
            }

            @Override
            public void dispose() {
                throw falhaDispose;
            }
        };
    }

    private ImageWriter writerComFalhaNaEscritaEDispose(
            AssertionError falhaWrite,
            AssertionError falhaDispose) {
        return new WriterTeste() {
            @Override
            public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) {
                throw falhaWrite;
            }

            @Override
            public void dispose() {
                throw falhaDispose;
            }
        };
    }

    private ImageWriter writerComFalhaSomenteNoDispose(IllegalStateException falhaDispose) {
        return new WriterTeste() {
            @Override
            public void write(IIOMetadata streamMetadata, IIOImage image, ImageWriteParam param) {
                // Escrita simulada sem falha; o comportamento sob teste é o dispose.
            }

            @Override
            public void dispose() {
                throw falhaDispose;
            }
        };
    }

    private abstract static class WriterTeste extends ImageWriter {

        WriterTeste() {
            super(null);
        }

        @Override
        public IIOMetadata getDefaultStreamMetadata(ImageWriteParam param) {
            return null;
        }

        @Override
        public IIOMetadata getDefaultImageMetadata(ImageTypeSpecifier imageType, ImageWriteParam param) {
            return null;
        }

        @Override
        public IIOMetadata convertStreamMetadata(IIOMetadata inData, ImageWriteParam param) {
            return null;
        }

        @Override
        public IIOMetadata convertImageMetadata(
                IIOMetadata inData,
                ImageTypeSpecifier imageType,
                ImageWriteParam param) {
            return null;
        }
    }

    private void assertDiretorioVazio() throws IOException {
        assertQuantidadeArquivosNaRaiz(0);
    }

    private void assertQuantidadeArquivosNaRaiz(long quantidadeEsperada) throws IOException {
        try (Stream<Path> arquivos = Files.list(tempDir)) {
            assertEquals(quantidadeEsperada, arquivos.count());
        }
    }
}
