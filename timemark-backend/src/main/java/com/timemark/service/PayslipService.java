package com.timemark.service;

import com.timemark.dto.PayrollResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class PayslipService {

    private static final PDType1Font HEADER_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDType1Font BODY_FONT =
            new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    public byte[] generate(PayrollResponse p) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 760;
                float left = 60;

                y = writeLine(content, left, y, HEADER_FONT, 20, "TimeMark");
                y = writeLine(content, left, y - 4, BODY_FONT, 11, "Payslip for " + p.getMonth());
                y -= 20;

                y = writeLine(content, left, y, HEADER_FONT, 13, p.getEmployeeName());
                y -= 20;

                y = writeRow(content, left, y, "Working days", String.valueOf(p.getWorkingDays()));
                y = writeRow(content, left, y, "Present", String.valueOf(p.getPresentDays()));
                y = writeRow(content, left, y, "Late (included in present)", String.valueOf(p.getLateDays()));
                y = writeRow(content, left, y, "Approved leave", String.valueOf(p.getLeaveDays()));
                y = writeRow(content, left, y, "Absent", String.valueOf(p.getAbsentDays()));
                y -= 10;

                y = writeRow(content, left, y, "Per-day rate", formatCurrency(p.getPerDayRate()));
                y = writeRow(content, left, y, "Deduction", formatCurrency(p.getDeduction()));
                y -= 10;

                y = writeLine(content, left, y, HEADER_FONT, 14,
                        "Net Pay: " + formatCurrency(p.getNetPay()));

                y -= 40;
                writeLine(content, left, y, BODY_FONT, 8,
                        "Generated on " + java.time.LocalDate.now().format(DateTimeFormatter.ISO_DATE)
                                + " - this is a system-generated document.");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to generate payslip PDF", e);
        }
    }

    private float writeRow(PDPageContentStream content, float x, float y, String label, String value) throws IOException {
        writeLine(content, x, y, BODY_FONT, 11, label + ":");
        writeLine(content, x + 220, y, BODY_FONT, 11, value);
        return y - 18;
    }

    private float writeLine(PDPageContentStream content, float x, float y, PDType1Font font, float size, String text) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
        return y - size - 4;
    }

    private String formatCurrency(double amount) {
        return String.format("Rs. %,.2f", amount);
    }
}
