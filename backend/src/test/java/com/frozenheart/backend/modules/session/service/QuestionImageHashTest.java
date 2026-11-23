package com.frozenheart.backend.modules.session.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class QuestionImageHashTest {
    @Test
    void readsWebpAndRecognizesTheSamePatternAtTwoResolutions() {
        assertTrue(ImageIO.getImageReadersByFormatName("webp").hasNext());
        long small = QuestionImageHash.hash(pattern(32));
        long large = QuestionImageHash.hash(pattern(128));
        assertTrue(Long.bitCount(small ^ large) <= 5);
    }

    private static BufferedImage pattern(int size) {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.WHITE);
            graphics.fillRect(size / 8, size / 8, size / 2, size / 2);
            graphics.setColor(Color.GRAY);
            graphics.fillOval(size / 2, size / 4, size / 4, size / 2);
        } finally {
            graphics.dispose();
        }
        return image;
    }
}
