package com.jwellkeeper.audit.service;

import com.jwellkeeper.audit.dto.AuditReportResponse;
import com.jwellkeeper.audit.dto.AuditReportSoldRow;
import com.jwellkeeper.audit.dto.AuditReportStockRow;
import com.jwellkeeper.audit.model.AuditItemResolution;
import com.jwellkeeper.audit.model.StockAudit;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.tenant.model.Tenant;
import com.jwellkeeper.tenant.repository.TenantRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class AuditPdfService {

    private final TenantRepository tenantRepository;

    @Value("${app.storage.root:storage}")
    private String storageRoot;

    private static final Color GOLD = new Color(181, 124, 20);
    private static final Color DARK = new Color(34, 31, 25);
    private static final Color LIGHT_GOLD = new Color(255, 248, 230);
    private static final Color TODAY_ADDED = new Color(255, 242, 199);
    private static final Color ALREADY_AVAILABLE = new Color(246, 247, 249);
    private static final Color SOLD_TODAY = new Color(232, 239, 252);
    private static final Color MISSING = new Color(255, 232, 232);
    private static final Color BORDER = new Color(221, 213, 195);

    public byte[] generate(StockAudit audit, AuditReportResponse report) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4.rotate(), 28, 28, 28, 30);
            PdfWriter.getInstance(document, output);
            document.open();

            Tenant tenant = tenantRepository.findById(audit.getTenantId()).orElse(null);
            addShopHeader(document, tenant, "DAILY STOCK AUDIT REPORT");

            Paragraph title = new Paragraph(report.auditName(), new Font(Font.HELVETICA, 17, Font.BOLD, GOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingBefore(10);
            document.add(title);
            Paragraph subtitle = new Paragraph("Audit Date: " + audit.getAuditDate() + "   Run: #" + audit.getRunNumber() + "   Status: " + audit.getStatus(), normalFont());
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(16);
            document.add(subtitle);

            PdfPTable summary = new PdfPTable(4);
            summary.setWidthPercentage(100);
            summary.setWidths(new float[]{1, 1, 1, 1});
            summary.addCell(summaryCell("Before sales stock", Long.toString(report.beforeSalesStock())));
            summary.addCell(summaryCell("After sales stock", Long.toString(report.afterSalesExpectedStock())));
            summary.addCell(summaryCell("Scanned", Long.toString(report.scannedItems())));
            summary.addCell(summaryCell("Missing", Long.toString(report.missingItems())));
            summary.addCell(summaryCell("Items sold today", Long.toString(report.itemsSoldToday())));
            summary.addCell(summaryCell("Sales total today", salesTotals(report)));
            summary.addCell(summaryCell("Manual closure", audit.isManuallyClosed() ? "Yes" : "No"));
            summary.addCell(summaryCell("Closed at", audit.getClosedAt() == null ? "-" : audit.getClosedAt().toString()));
            document.add(summary);
            document.add(new Paragraph(" "));

            addLegend(document);
            addTypeTally(document, report);
            addCurrentStockTable(document, report);
            addSoldTable(document, report);

            if (tenant != null && tenant.getReceiptFooterNote() != null && !tenant.getReceiptFooterNote().isBlank()) {
                Paragraph note = new Paragraph(tenant.getReceiptFooterNote(), new Font(Font.HELVETICA, 8, Font.ITALIC, new Color(107, 94, 68)));
                note.setAlignment(Element.ALIGN_CENTER);
                note.setSpacingBefore(18);
                document.add(note);
            }
            Paragraph footer = new Paragraph("Developed and built by autowhap (autowhap.com)", new Font(Font.HELVETICA, 9, Font.NORMAL, new Color(107, 94, 68)));
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(28);
            document.add(footer);
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate audit PDF");
        }
    }

    private void addLegend(Document document) throws Exception {
        PdfPTable legend = new PdfPTable(3);
        legend.setWidthPercentage(100);
        legend.setSpacingAfter(9);
        legend.addCell(legendCell("TODAY ADDED", "New items created on the audit date", TODAY_ADDED));
        legend.addCell(legendCell("ALREADY AVAILABLE", "Older stock still expected in the shop", ALREADY_AVAILABLE));
        legend.addCell(legendCell("TODAY SOLD", "Items billed on the audit date", SOLD_TODAY));
        document.add(legend);
    }

    private PdfPCell legendCell(String label, String description, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(color);
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Paragraph(label, new Font(Font.HELVETICA, 7, Font.BOLD, DARK)));
        cell.addElement(new Paragraph(description, new Font(Font.HELVETICA, 7, Font.NORMAL, new Color(82, 82, 82))));
        return cell;
    }

    private void addTypeTally(Document document, AuditReportResponse report) throws Exception {
        addSectionTitle(document, "Quick Tally By Jewellery Type");
        PdfPTable table = new PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2.2f, 1f, 1.2f, 1f, 1.2f, 1f, 1.3f});
        addHeader(table, "Type");
        addHeader(table, "Today Added");
        addHeader(table, "Already Available");
        addHeader(table, "Missing");
        addHeader(table, "Current Stock");
        addHeader(table, "Sold Today");
        addHeader(table, "Stock Weight");
        if (report.typeTallies().isEmpty()) {
            addBodyCell(table, "No stock or sales rows for this audit date.", 7, null);
        } else {
            report.typeTallies().forEach(row -> {
                addBodyCell(table, row.typeName());
                addBodyCell(table, Long.toString(row.todayAddedCount()));
                addBodyCell(table, Long.toString(row.alreadyAvailableCount()));
                addBodyCell(table, Long.toString(row.missingCount()));
                addBodyCell(table, Long.toString(row.currentStockCount()));
                addBodyCell(table, Long.toString(row.soldTodayCount()));
                addBodyCell(table, weight(row.currentStockWeight()));
            });
        }
        document.add(table);
    }

    private void addCurrentStockTable(Document document, AuditReportResponse report) throws Exception {
        addSectionTitle(document, "Current Stock Details");
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.65f, 1.5f, 1.8f, 0.8f, 0.9f, 1.25f, 1f, 1.8f, 1f, 1.45f});
        addHeader(table, "#");
        addHeader(table, "Type");
        addHeader(table, "Sub-type / Design");
        addHeader(table, "Karat");
        addHeader(table, "Weight");
        addHeader(table, "Category");
        addHeader(table, "Scanned");
        addHeader(table, "Resolution");
        addHeader(table, "Status");
        addHeader(table, "Jewellery ID");
        if (report.currentStockItems().isEmpty()) {
            addBodyCell(table, "No current stock rows captured for this audit.", 10, null);
        } else {
            int index = 1;
            for (AuditReportStockRow row : report.currentStockItems()) {
                Color shade = stockShade(row);
                addBodyCell(table, Integer.toString(index++), shade);
                addBodyCell(table, row.typeName(), shade);
                addBodyCell(table, blank(row.designName()), shade);
                addBodyCell(table, row.karat(), shade);
                addBodyCell(table, weight(row.weight()), shade);
                addBodyCell(table, categoryLabel(row.category()), shade);
                addBodyCell(table, row.scanned() ? "Yes" : "No", shade);
                addBodyCell(table, resolutionLabel(row.resolution()), shade);
                addBodyCell(table, row.status() == null ? "-" : row.status().name(), shade);
                addBodyCell(table, shortId(row.jewelleryId()), shade);
            }
        }
        document.add(table);
    }

    private void addSoldTable(Document document, AuditReportResponse report) throws Exception {
        addSectionTitle(document, "Today Sold Items");
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.65f, 1.25f, 1.45f, 1.7f, 0.8f, 0.9f, 1.2f, 1f, 1.4f});
        addHeader(table, "#");
        addHeader(table, "Bill No");
        addHeader(table, "Type");
        addHeader(table, "Sub-type / Design");
        addHeader(table, "Karat");
        addHeader(table, "Weight");
        addHeader(table, "Price");
        addHeader(table, "Currency");
        addHeader(table, "Jewellery ID");
        if (report.todaySoldItems().isEmpty()) {
            addBodyCell(table, "No items were sold on this audit date.", 9, null);
        } else {
            int index = 1;
            for (AuditReportSoldRow row : report.todaySoldItems()) {
                addBodyCell(table, Integer.toString(index++), SOLD_TODAY);
                addBodyCell(table, row.billNo(), SOLD_TODAY);
                addBodyCell(table, row.typeName(), SOLD_TODAY);
                addBodyCell(table, blank(row.designName()), SOLD_TODAY);
                addBodyCell(table, row.karat(), SOLD_TODAY);
                addBodyCell(table, weight(row.weight()), SOLD_TODAY);
                addBodyCell(table, money(row.finalPrice()), SOLD_TODAY);
                addBodyCell(table, row.currencyCode(), SOLD_TODAY);
                addBodyCell(table, shortId(row.jewelleryId()), SOLD_TODAY);
            }
        }
        document.add(table);
    }

    private void addSectionTitle(Document document, String title) throws Exception {
        Paragraph paragraph = new Paragraph(title, new Font(Font.HELVETICA, 12, Font.BOLD, DARK));
        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(5);
        document.add(paragraph);
    }

    private void addShopHeader(Document document, Tenant tenant, String documentTitle) throws Exception {
        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{0.55f, 2.3f, 1.15f});

        PdfPCell logoCell = noBorderCell();
        Image logo = logoImage(tenant, 50, 50);
        if (logo != null) {
            logoCell.addElement(logo);
        }
        table.addCell(logoCell);

        PdfPCell shopCell = noBorderCell();
        shopCell.addElement(new Paragraph(tenant == null ? "JwellKeeper" : tenant.getShopName(), new Font(Font.HELVETICA, 17, Font.BOLD, GOLD)));
        addOptional(shopCell, tenant == null ? null : tenant.getShopAddress());
        addOptional(shopCell, tenant == null ? null : joinContact(tenant));
        addOptional(shopCell, tenant == null || tenant.getTaxNumber() == null ? null : "Tax/Reg No: " + tenant.getTaxNumber());
        table.addCell(shopCell);

        PdfPCell titleCell = noBorderCell();
        titleCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph title = new Paragraph(documentTitle, new Font(Font.HELVETICA, 9, Font.BOLD, DARK));
        title.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(title);
        table.addCell(titleCell);
        document.add(table);

        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingBefore(6);
        PdfPCell line = new PdfPCell();
        line.setBackgroundColor(GOLD);
        line.setFixedHeight(3);
        line.setBorder(Rectangle.NO_BORDER);
        divider.addCell(line);
        document.add(divider);
    }

    private void addHeader(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE)));
        cell.setBackgroundColor(GOLD);
        cell.setBorderColor(GOLD);
        cell.setPadding(7);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text) {
        addBodyCell(table, text, null);
    }

    private void addBodyCell(PdfPTable table, String text, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, normalFont()));
        cell.setBorderColor(BORDER);
        cell.setPadding(7);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text, int colspan, Color background) {
        PdfPCell cell = new PdfPCell(new Phrase(text, normalFont()));
        cell.setColspan(colspan);
        cell.setBorderColor(BORDER);
        cell.setPadding(7);
        if (background != null) {
            cell.setBackgroundColor(background);
        }
        table.addCell(cell);
    }

    private PdfPCell summaryCell(String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(LIGHT_GOLD);
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        cell.addElement(new Paragraph(label, new Font(Font.HELVETICA, 8, Font.BOLD, GOLD)));
        cell.addElement(new Paragraph(value, normalFont()));
        return cell;
    }

    private PdfPCell noBorderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        return cell;
    }

    private void addOptional(PdfPCell cell, String text) {
        if (text != null && !text.isBlank()) {
            cell.addElement(new Paragraph(text, new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(100, 116, 139))));
        }
    }

    private String joinContact(Tenant tenant) {
        String phone = tenant.getShopContactNumber();
        String email = tenant.getShopEmail();
        if ((phone == null || phone.isBlank()) && (email == null || email.isBlank())) {
            return null;
        }
        if (phone == null || phone.isBlank()) {
            return email;
        }
        if (email == null || email.isBlank()) {
            return phone;
        }
        return phone + " · " + email;
    }

    private Image logoImage(Tenant tenant, float width, float height) {
        if (tenant == null || tenant.getLogoStorageKey() == null) {
            return null;
        }
        try {
            Path root = Paths.get(storageRoot).toAbsolutePath().normalize();
            Path path = root.resolve(tenant.getLogoStorageKey()).normalize();
            if (!path.startsWith(root) || !Files.exists(path)) {
                return null;
            }
            Image logo = Image.getInstance(Files.readAllBytes(path));
            logo.scaleToFit(width, height);
            return logo;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Font normalFont() {
        return new Font(Font.HELVETICA, 8, Font.NORMAL, DARK);
    }

    private String money(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String salesTotals(AuditReportResponse report) {
        if (report.salesTotalsByCurrency() == null || report.salesTotalsByCurrency().isEmpty()) {
            return "0.00";
        }
        return report.salesTotalsByCurrency().stream()
                .map(total -> total.currencyCode() + " " + money(total.totalAmount()))
                .reduce((left, right) -> left + " / " + right)
                .orElse("0.00");
    }

    private String weight(BigDecimal value) {
        return value == null ? "0.000 g" : value.setScale(3, RoundingMode.HALF_UP).toPlainString() + " g";
    }

    private String blank(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String shortId(java.util.UUID id) {
        return id == null ? "-" : id.toString().substring(0, 8);
    }

    private Color stockShade(AuditReportStockRow row) {
        if ("MISSING_IN_AUDIT".equals(row.category())) {
            return MISSING;
        }
        if ("TODAY_ADDED".equals(row.category())) {
            return TODAY_ADDED;
        }
        return ALREADY_AVAILABLE;
    }

    private String categoryLabel(String category) {
        if ("TODAY_ADDED".equals(category)) {
            return "TODAY ADDED";
        }
        if ("MISSING_IN_AUDIT".equals(category)) {
            return "MISSING";
        }
        return "ALREADY AVAILABLE";
    }

    private String resolutionLabel(AuditItemResolution resolution) {
        return resolution == null ? "-" : resolution.name().replace('_', ' ');
    }
}
