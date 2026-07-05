package com.artdecor.workforce.application.reports;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class SimplePdf {
    private static final int PAGE_WIDTH = 612;
    private static final int PAGE_HEIGHT = 792;
    private static final int LEFT_MARGIN = 44;
    private static final int TOP_MARGIN = 758;
    private static final int BOTTOM_MARGIN = 44;
    private static final int BODY_FONT_SIZE = 9;
    private static final int BODY_LINE_HEIGHT = 12;

    private SimplePdf() {
    }

    static byte[] render(String title, List<String> lines) {
        List<String> pageStreams = new ArrayList<>();
        StringBuilder content = newPage(title);
        int y = TOP_MARGIN - 34;

        for (String line : lines) {
            if (y < BOTTOM_MARGIN) {
                content.append("ET");
                pageStreams.add(content.toString());
                content = newPage(title);
                y = TOP_MARGIN - 34;
            }
            content.append("(").append(escape(line)).append(") Tj\n0 -").append(BODY_LINE_HEIGHT).append(" Td\n");
            y -= BODY_LINE_HEIGHT;
        }
        content.append("ET");
        pageStreams.add(content.toString());

        List<String> objects = new ArrayList<>();
        objects.add("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        objects.add("2 0 obj << /Type /Pages /Kids " + pageKids(pageStreams.size()) + " /Count " + pageStreams.size() + " >> endobj\n");
        objects.add("3 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >> endobj\n");
        objects.add("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Courier >> endobj\n");

        int nextObject = 5;
        for (String pageStream : pageStreams) {
            int pageObject = nextObject++;
            int contentObject = nextObject++;
            byte[] stream = pageStream.getBytes(StandardCharsets.ISO_8859_1);
            objects.add(pageObject + " 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + PAGE_WIDTH + " " + PAGE_HEIGHT
                    + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "
                    + contentObject + " 0 R >> endobj\n");
            objects.add(contentObject + " 0 obj << /Length " + stream.length + " >> stream\n"
                    + pageStream + "\nendstream endobj\n");
        }

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (String object : objects) {
            offsets.add(pdf.length());
            pdf.append(object);
        }

        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset));
        }
        pdf.append("trailer << /Size ").append(objects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static StringBuilder newPage(String title) {
        return new StringBuilder("BT\n/F1 14 Tf\n")
                .append(LEFT_MARGIN)
                .append(" ")
                .append(TOP_MARGIN)
                .append(" Td\n(")
                .append(escape(title))
                .append(") Tj\n/F2 ")
                .append(BODY_FONT_SIZE)
                .append(" Tf\n0 -24 Td\n");
    }

    private static String pageKids(int pageCount) {
        StringBuilder kids = new StringBuilder("[");
        for (int index = 0; index < pageCount; index++) {
            kids.append(5 + (index * 2)).append(" 0 R ");
        }
        return kids.append("]").toString();
    }

    private static String escape(String value) {
        return value
                .chars()
                .map(character -> character <= 255 ? character : '?')
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString()
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }
}
