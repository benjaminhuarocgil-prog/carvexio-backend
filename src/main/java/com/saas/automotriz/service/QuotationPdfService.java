package com.saas.automotriz.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.saas.automotriz.model.Quotation;
import com.saas.automotriz.model.QuotationItem;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class QuotationPdfService {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateReceipt(Quotation quotation) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfDocument pdf = new PdfDocument(new PdfWriter(output));
            Document document = new Document(pdf);

            document.add(new Paragraph("CARVEXIO · BOLETA DE COTIZACIÓN")
                    .setBold().setFontSize(18).setFontColor(ColorConstants.BLACK));
            document.add(new Paragraph("Boleta N° " + quotation.getId())
                    .setBold().setFontSize(13));
            document.add(new Paragraph("Emitida: " + quotation.getApprovedAt().format(DATE_FORMAT)));
            document.add(new Paragraph("Taller: " + quotation.getBusiness().getName()));
            document.add(new Paragraph("Cliente: " + quotation.getClient().getName()
                    + (quotation.getClient().getPhone() == null ? "" : " · " + quotation.getClient().getPhone())));
            if (quotation.getBooking() != null && quotation.getBooking().getVehicle() != null) {
                document.add(new Paragraph("Vehículo: " + quotation.getBooking().getVehicle().getVehicleType()
                        + " · Placa: " + quotation.getBooking().getVehicle().getPlate()));
            }
            document.add(new Paragraph("\nDIAGNÓSTICO").setBold().setFontColor(ColorConstants.BLACK));
            document.add(new Paragraph(quotation.getDiagnosis()));
            document.add(new Paragraph("\nDETALLE DE COTIZACIÓN").setBold().setFontColor(ColorConstants.BLACK));

            Table table = new Table(new float[]{4, 1, 2, 2}).useAllAvailableWidth();
            String[] headers = {"Descripción", "Cant.", "P. unitario", "Subtotal"};
            for (String header : headers) {
                table.addHeaderCell(new Cell().add(new Paragraph(header).setBold()).setBackgroundColor(ColorConstants.LIGHT_GRAY));
            }
            for (QuotationItem item : quotation.getItems()) {
                table.addCell(item.getDescription());
                table.addCell(new Cell().add(new Paragraph(String.valueOf(item.getQuantity()))).setTextAlignment(TextAlignment.CENTER));
                table.addCell("S/ " + String.format("%.2f", item.getUnitPrice()));
                table.addCell("S/ " + String.format("%.2f", item.getSubtotal()));
            }
            document.add(table);
            document.add(new Paragraph("TOTAL APROBADO: S/ " + String.format("%.2f", quotation.getTotalAmount()))
                    .setBold().setFontSize(15).setTextAlignment(TextAlignment.RIGHT).setMarginTop(16));
            document.add(new Paragraph("Cotización aprobada por el cliente. Este documento acredita el detalle y monto acordado.")
                    .setFontSize(9).setFontColor(ColorConstants.BLACK).setMarginTop(20));
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la boleta PDF", exception);
        }
    }
}
