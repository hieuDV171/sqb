package com.frozenheart.backend.modules.session.service;

import com.frozenheart.backend.core.config.property.R2Properties;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
public class QuestionImageHash {
    private static final int SIZE = 32;
    private static final int FREQUENCIES = 8;
    private static final int MAX_BYTES = 20 * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000;
    private static final double[][] COSINES = cosines();

    private final MinioClient minioClient;
    private final R2Properties r2Properties;

    public long read(String objectKey) throws Exception {
        var request = GetObjectArgs.builder().bucket(r2Properties.getBucketName()).object(objectKey).build();
        try (var object = minioClient.getObject(request)) {
            byte[] bytes = object.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) throw new IOException("Image exceeds pHash byte limit");
            try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
                var readers = ImageIO.getImageReaders(input);
                if (!readers.hasNext()) throw new IOException("Unsupported image format");
                var reader = readers.next();
                try {
                    reader.setInput(input);
                    if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                        throw new IOException("Image exceeds pHash pixel limit");
                    }
                    return hash(reader.read(0));
                } finally {
                    reader.dispose();
                }
            }
        }
    }

    static long hash(BufferedImage image) {
        var gray = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
        var graphics = gray.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(image, 0, 0, SIZE, SIZE, null);
        } finally {
            graphics.dispose();
        }
        var pixels = gray.getRaster().getSamples(0, 0, SIZE, SIZE, 0, (int[]) null);
        double[] coefficients = new double[FREQUENCIES * FREQUENCIES];
        for (int y = 0; y < SIZE; y++) {
            double[] horizontal = new double[FREQUENCIES];
            for (int u = 0; u < FREQUENCIES; u++) {
                for (int x = 0; x < SIZE; x++) horizontal[u] += COSINES[u][x] * pixels[y * SIZE + x];
            }
            for (int v = 0; v < FREQUENCIES; v++) {
                for (int u = 0; u < FREQUENCIES; u++) coefficients[v * FREQUENCIES + u] += COSINES[v][y] * horizontal[u];
            }
        }
        double[] sorted = Arrays.copyOfRange(coefficients, 1, coefficients.length);
        Arrays.sort(sorted);
        double median = sorted[sorted.length / 2];
        long hash = 0;
        for (int index = 1; index < coefficients.length; index++) {
            if (coefficients[index] > median) hash |= 1L << index;
        }
        return hash;
    }

    private static double[][] cosines() {
        double[][] coefficients = new double[FREQUENCIES][SIZE];
        for (int k = 0; k < FREQUENCIES; k++) {
            for (int x = 0; x < SIZE; x++) {
                coefficients[k][x] = Math.cos((2 * x + 1) * k * Math.PI / (2 * SIZE))
                        * (k == 0 ? Math.sqrt(0.5) : 1.0);
            }
        }
        return coefficients;
    }
}
