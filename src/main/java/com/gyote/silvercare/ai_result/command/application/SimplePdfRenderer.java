package com.gyote.silvercare.ai_result.command.application;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 모델 출력의 실행 가능한 형식은 제거하고 서버에서 안전한 텍스트 PDF를 만든다. */
final class SimplePdfRenderer {
    private static final float FONT_SIZE = 11;
    private static final float LEADING = 16;
    private static final float MARGIN = 48;

    private SimplePdfRenderer() {
    }

    static byte[] render(String title, String content, String configuredFontPath) throws IOException {
        try (PDDocument pdf = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream();
             InputStream fontStream = openFont(configuredFontPath)) {
            PDType0Font font = PDType0Font.load(pdf, fontStream);
            float maxWidth = PDRectangle.A4.getWidth() - MARGIN * 2;
            List<String> lines = wrap(sanitize(title, content), font, maxWidth);
            PDPageContentStream stream = null;
            float y = 0;
            try {
                for (String line : lines) {
                    if (stream == null || y < MARGIN) {
                        if (stream != null) {
                            stream.endText();
                            stream.close();
                        }
                        PDPage page = new PDPage(PDRectangle.A4);
                        pdf.addPage(page);
                        stream = new PDPageContentStream(pdf, page);
                        stream.beginText();
                        stream.setFont(font, FONT_SIZE);
                        y = PDRectangle.A4.getHeight() - MARGIN;
                        stream.newLineAtOffset(MARGIN, y);
                    }
                    stream.showText(line);
                    stream.newLineAtOffset(0, -LEADING);
                    y -= LEADING;
                }
            } finally {
                if (stream != null) {
                    stream.endText();
                    stream.close();
                }
            }
            pdf.save(output);
            return output.toByteArray();
        }
    }

    private static String sanitize(String title, String content) {
        String heading = title == null || title.isBlank() ? "AI 상세 설명" : title;
        String body = content == null ? "" : content;
        return (heading + "\n\n" + body).replaceAll("[#*_`]", "");
    }

    private static List<String> wrap(String text, PDType0Font font, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String sourceLine : text.split("\\R", -1)) {
            if (sourceLine.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int offset = 0; offset < sourceLine.length();) {
                int codePoint = sourceLine.codePointAt(offset);
                String character = new String(Character.toChars(codePoint));
                String candidate = current + character;
                if (!current.isEmpty() && textWidth(candidate, font) > maxWidth) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                current.append(character);
                offset += Character.charCount(codePoint);
            }
            lines.add(current.toString());
        }
        return lines;
    }

    private static float textWidth(String value, PDType0Font font) throws IOException {
        return font.getStringWidth(value) / 1000 * FONT_SIZE;
    }

    private static InputStream openFont(String configuredFontPath) throws IOException {
        if (configuredFontPath != null && !configuredFontPath.isBlank()) {
            Path configured = Path.of(configuredFontPath);
            if (!Files.isRegularFile(configured)) {
                throw new IOException("설정된 PDF 폰트를 찾을 수 없습니다: " + configured);
            }
            return Files.newInputStream(configured);
        }
        InputStream bundled = SimplePdfRenderer.class.getResourceAsStream("/fonts/NotoSansKR-Regular.ttf");
        if (bundled != null) return bundled;
        for (Path candidate : List.of(
                Path.of("C:/Windows/Fonts/NotoSansKR-VF.ttf"),
                Path.of("C:/Windows/Fonts/malgun.ttf"),
                Path.of("/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc"),
                Path.of("/usr/share/fonts/truetype/noto/NotoSansKR-Regular.ttf"),
                Path.of("/System/Library/Fonts/AppleSDGothicNeo.ttc"))) {
            if (Files.isRegularFile(candidate)) return Files.newInputStream(candidate);
        }
        throw new IOException("한글 PDF 폰트를 찾을 수 없습니다. silvercare.pdf.font-path를 설정해 주세요.");
    }
}
