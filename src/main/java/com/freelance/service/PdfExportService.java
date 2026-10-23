package com.freelance.service;

import com.freelance.model.Client;
import com.freelance.model.Invoice;
import com.freelance.model.Project;
import com.freelance.model.WorkLog;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PdfExportService {
    private static final Logger logger = LoggerFactory.getLogger(PdfExportService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    // Ledger Color Tokens
    private static final Color COLOR_INK = new Color(0x1F, 0x2A, 0x24);
    private static final Color COLOR_PAPER = new Color(0xF7, 0xF4, 0xEC);
    private static final Color COLOR_LINE = new Color(0xD8, 0xD0, 0xBE);
    private static final Color COLOR_GOLD = new Color(0xB8, 0x86, 0x2F);
    private static final Color COLOR_GREEN = new Color(0x2F, 0x5D, 0x50);
    private static final Color COLOR_RED = new Color(0xA6, 0x3D, 0x33);

    public static File exportInvoiceToPdf(Invoice invoice, List<WorkLog> workLogs, File destinationFile) throws Exception {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(destinationFile));

        document.open();

        Project project = invoice.getProject();
        Client client = project != null ? project.getClient() : null;

        // Fonts
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_INK);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_GOLD);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10, COLOR_INK);
        Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_INK);
        Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, COLOR_PAPER);
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_GOLD);

        // Header Section
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{60, 40});

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.addElement(new Paragraph("INVOICE", titleFont));
        leftCell.addElement(new Paragraph(invoice.getFormattedInvoiceNumber(), subtitleFont));
        leftCell.addElement(new Paragraph("Project: " + (project != null ? project.getProjectName() : "N/A"), bodyFont));

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph datesPara = new Paragraph();
        datesPara.setAlignment(Element.ALIGN_RIGHT);
        datesPara.add(new Chunk("Issue Date: ", boldFont));
        datesPara.add(new Chunk(invoice.getIssueDate().format(DATE_FORMAT) + "\n", bodyFont));
        datesPara.add(new Chunk("Due Date: ", boldFont));
        datesPara.add(new Chunk(invoice.getDueDate().format(DATE_FORMAT) + "\n", bodyFont));

        Color statusColor;
        switch (invoice.getEffectiveStatus()) {
            case PAID -> statusColor = COLOR_GREEN;
            case OVERDUE -> statusColor = COLOR_RED;
            default -> statusColor = COLOR_INK;
        }
        Font statusFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, statusColor);
        datesPara.add(new Chunk("Status: ", boldFont));
        datesPara.add(new Chunk(invoice.getEffectiveStatus().getDisplayName().toUpperCase(), statusFont));
        rightCell.addElement(datesPara);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);

        document.add(new Paragraph("\n"));

        // Client Info Box
        if (client != null) {
            PdfPTable clientTable = new PdfPTable(1);
            clientTable.setWidthPercentage(100);
            PdfPCell cCell = new PdfPCell();
            cCell.setBackgroundColor(COLOR_PAPER);
            cCell.setBorderColor(COLOR_LINE);
            cCell.setPadding(10);
            cCell.addElement(new Paragraph("BILLED TO:", boldFont));
            cCell.addElement(new Paragraph(client.getName(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, COLOR_INK)));
            if (client.getEmail() != null && !client.getEmail().isBlank()) {
                cCell.addElement(new Paragraph("Email: " + client.getEmail(), bodyFont));
            }
            if (client.getPhone() != null && !client.getPhone().isBlank()) {
                cCell.addElement(new Paragraph("Phone: " + client.getPhone(), bodyFont));
            }
            clientTable.addCell(cCell);
            document.add(clientTable);
        }

        document.add(new Paragraph("\n"));

        // Work Logs Table
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{18, 42, 12, 13, 15});

        String[] headers = {"Date", "Description", "Hours", "Rate", "Amount"};
        for (String h : headers) {
            PdfPCell hCell = new PdfPCell(new Phrase(h, tableHeaderFont));
            hCell.setBackgroundColor(COLOR_INK);
            hCell.setPadding(8);
            hCell.setHorizontalAlignment(h.equals("Description") || h.equals("Date") ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(hCell);
        }

        BigDecimal hourlyRate = project != null ? project.getHourlyRate() : BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        if (workLogs != null && !workLogs.isEmpty()) {
            for (WorkLog log : workLogs) {
                BigDecimal lineAmount = log.getHoursWorked().multiply(hourlyRate).setScale(2, RoundingMode.HALF_UP);
                grandTotal = grandTotal.add(lineAmount);

                PdfPCell dateCell = new PdfPCell(new Phrase(log.getWorkDate().format(DATE_FORMAT), bodyFont));
                PdfPCell descCell = new PdfPCell(new Phrase(log.getDescription() != null ? log.getDescription() : "-", bodyFont));
                PdfPCell hoursCell = new PdfPCell(new Phrase(log.getHoursWorked().toString(), bodyFont));
                PdfPCell rateCell = new PdfPCell(new Phrase("INR " + hourlyRate.toString(), bodyFont));
                PdfPCell amountCell = new PdfPCell(new Phrase("INR " + lineAmount.toString(), bodyFont));

                hoursCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                rateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

                for (PdfPCell cell : new PdfPCell[]{dateCell, descCell, hoursCell, rateCell, amountCell}) {
                    cell.setPadding(6);
                    cell.setBorderColor(COLOR_LINE);
                }

                table.addCell(dateCell);
                table.addCell(descCell);
                table.addCell(hoursCell);
                table.addCell(rateCell);
                table.addCell(amountCell);
            }
        } else {
            PdfPCell emptyCell = new PdfPCell(new Phrase("No work logs attached to this invoice.", bodyFont));
            emptyCell.setColspan(5);
            emptyCell.setPadding(10);
            emptyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(emptyCell);
            grandTotal = invoice.getTotalAmount();
        }

        document.add(table);

        document.add(new Paragraph("\n"));

        if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
            Paragraph notes = new Paragraph();
            notes.add(new Chunk("Notes: ", boldFont));
            notes.add(new Chunk(invoice.getNotes(), bodyFont));
            document.add(notes);
            document.add(new Paragraph("\n"));
        }

        // The final total includes the invoice-level discount and tax configuration.
        grandTotal = invoice.calculateFinalAmount();

        // Summary Total Box
        PdfPTable totalTable = new PdfPTable(2);
        totalTable.setWidthPercentage(100);
        totalTable.setWidths(new float[]{60, 40});

        PdfPCell blankCell = new PdfPCell();
        blankCell.setBorder(Rectangle.NO_BORDER);

        PdfPCell sumCell = new PdfPCell();
        sumCell.setBorderColor(COLOR_LINE);
        sumCell.setBackgroundColor(COLOR_PAPER);
        sumCell.setPadding(10);

        Paragraph totalPara = new Paragraph();
        totalPara.setAlignment(Element.ALIGN_RIGHT);
        totalPara.add(new Chunk("TOTAL DUE:  ", boldFont));
        totalPara.add(new Chunk("INR " + grandTotal.setScale(2, RoundingMode.HALF_UP).toString(), totalFont));
        sumCell.addElement(totalPara);

        totalTable.addCell(blankCell);
        totalTable.addCell(sumCell);
        document.add(totalTable);

        document.close();
        logger.info("Exported PDF invoice to: {}", destinationFile.getAbsolutePath());
        return destinationFile;
    }
}
