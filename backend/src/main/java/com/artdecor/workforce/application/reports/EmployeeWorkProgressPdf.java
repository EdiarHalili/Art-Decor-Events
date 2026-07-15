package com.artdecor.workforce.application.reports;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class EmployeeWorkProgressPdf {
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int LEFT = 30;
    private static final int RIGHT = 565;
    private static final int TOP = 812;
    private static final int BOTTOM = 34;
    private static final int TABLE_ROW_HEIGHT = 15;
    private static final int SMALL_ROW_HEIGHT = 13;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Report report;
    private final List<String> objects = new ArrayList<>();
    private final List<Integer> pageObjects = new ArrayList<>();

    private EmployeeWorkProgressPdf(Report report) {
        this.report = report;
    }

    static byte[] render(Report report) {
        return new EmployeeWorkProgressPdf(report).render();
    }

    private byte[] render() {
        Page page = newPage();
        header(page);
        tableHeader(page);

        for (AttendanceLine line : report.lines()) {
            if (page.y < BOTTOM + TABLE_ROW_HEIGHT + 120) {
                footer(page);
                finishPage(page);
                page = newPage();
                tableHeader(page);
            }
            attendanceRow(page, line);
        }

        int requiredSummaryHeight = 22 + (4 * SMALL_ROW_HEIGHT) + 16 + gpsHeight(report.gpsLines()) + 28;
        if (page.y < BOTTOM + requiredSummaryHeight) {
            footer(page);
            finishPage(page);
            page = newPage();
        }
        summary(page);
        page = gpsSection(page);
        footer(page);
        finishPage(page);
        return documentBytes();
    }

    private Page newPage() {
        return new Page(TOP);
    }

    private void header(Page page) {
        text(page, "Përmbledhje mujore e punës", LEFT, page.y, 13, true);
        page.y -= 20;
        String left = "Kompania: " + report.company();
        String right = "Punëtori: " + report.employeeName();
        text(page, left, LEFT, page.y, 8, false);
        text(page, right, 300, page.y, 8, false);
        page.y -= 12;
        text(page, "Kodi: " + report.employeeCode(), LEFT, page.y, 8, false);
        text(page, "Periudha: " + report.periodLabel(), 300, page.y, 8, false);
        page.y -= 16;
    }

    private void tableHeader(Page page) {
        line(page, LEFT, page.y + 6, RIGHT, page.y + 6);
        text(page, "Data", 34, page.y, 8, true);
        text(page, "Hyrja", 103, page.y, 8, true);
        text(page, "Dalja", 178, page.y, 8, true);
        text(page, "Orët e punës", 300, page.y, 8, true);
        text(page, "Statusi", 397, page.y, 8, true);
        page.y -= 9;
        line(page, LEFT, page.y + 3, RIGHT, page.y + 3);
        page.y -= 7;
    }

    private void attendanceRow(Page page, AttendanceLine line) {
        text(page, line.date(), 34, page.y, 8, false);
        text(page, line.checkIn(), 103, page.y, 8, false);
        text(page, line.checkOut(), 178, page.y, 8, false);
        text(page, line.workedHours(), 300, page.y, 8, false);
        text(page, line.status(), 397, page.y, 8, false);
        page.y -= TABLE_ROW_HEIGHT;
    }

    private void summary(Page page) {
        page.y -= 4;
        line(page, LEFT, page.y + 6, RIGHT, page.y + 6);
        text(page, "Totali i ditëve të punuara", LEFT, page.y, 8, true);
        text(page, String.valueOf(report.totals().workedDays()), 220, page.y, 8, false);
        page.y -= SMALL_ROW_HEIGHT;
        text(page, "Totali i orëve normale", LEFT, page.y, 8, true);
        text(page, minutesLabel(report.totals().normalMinutes()), 220, page.y, 8, false);
        page.y -= SMALL_ROW_HEIGHT;
        text(page, "Totali i orëve shtesë", LEFT, page.y, 8, true);
        text(page, minutesLabel(report.totals().overtimeMinutes()), 220, page.y, 8, false);
        page.y -= SMALL_ROW_HEIGHT;
        text(page, "Totali i përgjithshëm i orëve", LEFT, page.y, 8, true);
        text(page, minutesLabel(report.totals().totalMinutes()), 220, page.y, 8, false);
        page.y -= 16;
    }

    private Page gpsSection(Page page) {
        if (report.gpsLines().isEmpty()) {
            return page;
        }
        text(page, "GPS", LEFT, page.y, 8, true);
        page.y -= SMALL_ROW_HEIGHT;
        for (GpsLine gps : report.gpsLines()) {
            if (page.y < BOTTOM + 50) {
                footer(page);
                finishPage(page);
                page = newPage();
                text(page, "GPS", LEFT, page.y, 8, true);
                page.y -= SMALL_ROW_HEIGHT;
            }
            text(page, gps.date(), LEFT, page.y, 8, false);
            int x = 104;
            if (gps.checkInUrl() != null) {
                linkText(page, "Vendndodhja e hyrjes: Shiko në hartë", x, page.y, gps.checkInUrl());
                x = 315;
            }
            if (gps.checkOutUrl() != null) {
                linkText(page, "Vendndodhja e daljes: Shiko në hartë", x, page.y, gps.checkOutUrl());
            }
            page.y -= SMALL_ROW_HEIGHT;
        }
        page.y -= 3;
        return page;
    }

    private void footer(Page page) {
        line(page, LEFT, 28, RIGHT, 28);
        text(page, "Raporti u gjenerua automatikisht nga Art Decor Events Workforce", LEFT, 18, 7, false);
        text(page, "Gjeneruar: " + report.generatedAtLabel(), 410, 18, 7, false);
    }

    private void finishPage(Page page) {
        String annots = annotations(page);
        int contentObject = objects.size() + 5;
        int pageObject = objects.size() + 6;
        byte[] stream = page.content.toString().getBytes(StandardCharsets.ISO_8859_1);
        objects.add(contentObject + " 0 obj << /Length " + stream.length + " >> stream\n"
                + page.content + "\nendstream endobj\n");
        objects.add(pageObject + " 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                + PAGE_WIDTH + " " + PAGE_HEIGHT
                + "] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Annots "
                + annots + " /Contents " + contentObject + " 0 R >> endobj\n");
        pageObjects.add(pageObject);
    }

    private String annotations(Page page) {
        if (page.links.isEmpty()) {
            return "[]";
        }
        StringBuilder refs = new StringBuilder("[");
        for (Link link : page.links) {
            int objectNumber = objects.size() + 5;
            objects.add(objectNumber + " 0 obj << /Type /Annot /Subtype /Link /Rect ["
                    + link.x1 + " " + link.y1 + " " + link.x2 + " " + link.y2
                    + "] /Border [0 0 0] /A << /S /URI /URI (" + escape(link.url) + ") >> >> endobj\n");
            refs.append(objectNumber).append(" 0 R ");
        }
        return refs.append("]").toString();
    }

    private byte[] documentBytes() {
        List<String> allObjects = new ArrayList<>();
        allObjects.add("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        allObjects.add("2 0 obj << /Type /Pages /Kids " + pageKids() + " /Count " + pageObjects.size() + " >> endobj\n");
        allObjects.add("3 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >> endobj\n");
        allObjects.add("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >> endobj\n");
        allObjects.addAll(objects);

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (String object : allObjects) {
            offsets.add(pdf.length());
            pdf.append(object);
        }

        int xref = pdf.length();
        pdf.append("xref\n0 ").append(allObjects.size() + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format(Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer << /Size ").append(allObjects.size() + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xref).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private String pageKids() {
        StringBuilder kids = new StringBuilder("[");
        for (Integer pageObject : pageObjects) {
            kids.append(pageObject).append(" 0 R ");
        }
        return kids.append("]").toString();
    }

    private void text(Page page, String value, int x, int y, int size, boolean bold) {
        page.content.append("BT /").append(bold ? "F1" : "F2").append(" ").append(size)
                .append(" Tf ").append(x).append(" ").append(y).append(" Td (")
                .append(escape(value)).append(") Tj ET\n");
    }

    private void linkText(Page page, String value, int x, int y, String url) {
        text(page, value, x, y, 8, false);
        page.content.append("0.14 0.31 0.63 RG ")
                .append(x).append(" ").append(y - 1).append(" m ")
                .append(x + textWidth(value)).append(" ").append(y - 1).append(" l S 0 G\n");
        page.links.add(new Link(x, y - 2, x + textWidth(value), y + 9, url));
    }

    private void line(Page page, int x1, int y1, int x2, int y2) {
        page.content.append("0.75 w 0.78 G ").append(x1).append(" ").append(y1).append(" m ")
                .append(x2).append(" ").append(y2).append(" l S 0 G\n");
    }

    private int textWidth(String value) {
        return Math.min(value.length() * 4, 190);
    }

    private int gpsHeight(List<GpsLine> gpsLines) {
        if (gpsLines.isEmpty()) {
            return 0;
        }
        return SMALL_ROW_HEIGHT + (gpsLines.size() * SMALL_ROW_HEIGHT);
    }

    private static String minutesLabel(int minutes) {
        int safeMinutes = Math.max(minutes, 0);
        int hours = safeMinutes / 60;
        int remainingMinutes = safeMinutes % 60;
        if (remainingMinutes == 0) {
            return hours + "h";
        }
        return hours + "h " + remainingMinutes + "min";
    }

    static String date(LocalDate date) {
        return DATE.format(date);
    }

    static String time(Instant value, ZoneId zone) {
        return value == null ? "-" : TIME.format(value.atZone(zone));
    }

    static String checkOut(AttendanceReportRow row, ZoneId zone) {
        if (row.checkedOutAt() == null) {
            return "-";
        }
        String value = time(row.checkedOutAt(), zone);
        if (isNextDay(row, zone)) {
            value += " (+1 ditë)";
        }
        if ("AUTO_CHECKED_OUT".equals(row.checkoutType()) || row.autoCheckout()) {
            value += " Dalje automatike";
        }
        return value;
    }

    private static boolean isNextDay(AttendanceReportRow row, ZoneId zone) {
        if (row.checkedInAt() == null || row.checkedOutAt() == null) {
            return false;
        }
        return row.checkedOutAt().atZone(zone).toLocalDate().isAfter(row.checkedInAt().atZone(zone).toLocalDate());
    }

    static String generatedAtLabel(Instant generatedAt, ZoneId zone) {
        ZonedDateTime value = generatedAt.atZone(zone);
        return DATE.format(value) + " " + TIME.format(value);
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

    record Report(
            String company,
            String employeeName,
            String employeeCode,
            String periodLabel,
            String generatedAtLabel,
            List<AttendanceLine> lines,
            List<GpsLine> gpsLines,
            Totals totals
    ) {
    }

    record AttendanceLine(String date, String checkIn, String checkOut, String workedHours, String status) {
    }

    record GpsLine(String date, String checkInUrl, String checkOutUrl) {
    }

    record Totals(int workedDays, int normalMinutes, int overtimeMinutes, int totalMinutes) {
    }

    private static final class Page {
        private final StringBuilder content = new StringBuilder();
        private final List<Link> links = new ArrayList<>();
        private int y;

        private Page(int y) {
            this.y = y;
        }
    }

    private record Link(int x1, int y1, int x2, int y2, String url) {
    }
}
