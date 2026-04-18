package com.jwellkeeper.billing.service;

import com.jwellkeeper.billing.model.Bill;
import com.jwellkeeper.billing.model.BillItem;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.jewellery.repository.JewelleryRepository;
import com.jwellkeeper.jewellery.service.QrCodeService;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BillPdfService {

    private final JewelleryRepository jewelleryRepository;
    private final QrCodeService qrCodeService;
    private final TenantRepository tenantRepository;

    @Value("${app.storage.root:storage}")
    private String storageRoot;

    // Colour palette
    private static final Color AMBER      = new Color(180, 120, 20);
    private static final Color AMBER_DARK = new Color(120, 78, 10);
    private static final Color AMBER_BG   = new Color(255, 248, 225);
    private static final Color WHITE      = Color.WHITE;
    private static final Color SLATE_900  = new Color(15, 23, 42);
    private static final Color SLATE_500  = new Color(100, 116, 139);
    private static final Color SLATE_100  = new Color(241, 245, 249);
    private static final Color BORDER     = new Color(226, 232, 240);
    private static final Color GREEN      = new Color(22, 163, 74);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] generate(Bill bill) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document doc = new Document(PageSize.A4, 40, 40, 36, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, bill);
            gap(doc);
            addCustomerAndMetaBlock(doc, bill);
            gap(doc);
            addItemsTable(doc, bill);
            gap(doc);
            addTotalsBlock(doc, bill);
            if (bill.getNotes() != null && !bill.getNotes().isBlank()) {
                gap(doc);
                addNotesBlock(doc, bill.getNotes());
            }
            gap(doc);
            addFooter(doc, bill);

            doc.close();
            return out.toByteArray();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate bill PDF: " + ex.getMessage());
        }
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Bill bill) throws Exception {
        Tenant tenant = tenantRepository.findById(bill.getTenantId()).orElse(null);
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1.4f, 1f});

        // Left: brand
        PdfPCell brand = noBorderCell();
        PdfPTable brandInner = new PdfPTable(2);
        brandInner.setWidthPercentage(100);
        brandInner.setWidths(new float[]{0.55f, 1.45f});
        PdfPCell logoCell = noBorderCell();
        logoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setFixedHeight(58);
        Image logo = logoImage(tenant, 52, 52);
        if (logo != null) {
            logoCell.addElement(logo);
        } else {
            PdfPCell placeholder = new PdfPCell();
            placeholder.setFixedHeight(52);
            placeholder.setBorderColor(BORDER);
            placeholder.setBackgroundColor(WHITE);
            placeholder.setPadding(0);
            PdfPTable placeholderTable = new PdfPTable(1);
            placeholderTable.setWidthPercentage(100);
            placeholderTable.addCell(placeholder);
            logoCell.addElement(placeholderTable);
        }
        brandInner.addCell(logoCell);
        PdfPCell textCell = noBorderCell();
        textCell.addElement(para(tenant == null ? "JewellKeeper" : tenant.getShopName(), font(18, Font.BOLD, AMBER)));
        // textCell.addElement(para("Fine Jewellery", font(9, Font.NORMAL, SLATE_500)));
        if (tenant != null) {
            addOptional(textCell, tenant.getShopAddress(), font(8, Font.NORMAL, SLATE_500));
            addOptional(textCell, joinContact(tenant), font(8, Font.NORMAL, SLATE_500));
            addOptional(textCell, tenant.getTaxNumber() == null ? null : "Tax/Reg No: " + tenant.getTaxNumber(), font(8, Font.NORMAL, SLATE_500));
        }
        brandInner.addCell(textCell);
        brand.addElement(brandInner);
        t.addCell(brand);

        // Right: bill meta
        PdfPCell meta = noBorderCell();
        meta.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph billNo = para(bill.getBillNo(), font(16, Font.BOLD, SLATE_900));
        billNo.setAlignment(Element.ALIGN_RIGHT);
        meta.addElement(billNo);
        Paragraph label = para("TAX INVOICE", font(8, Font.BOLD, AMBER));
        label.setAlignment(Element.ALIGN_RIGHT);
        meta.addElement(label);
        Paragraph dateP = para(bill.getBillDate().format(DATE_FMT), font(9, Font.NORMAL, SLATE_500));
        dateP.setAlignment(Element.ALIGN_RIGHT);
        meta.addElement(dateP);
        t.addCell(meta);

        doc.add(t);

        // Amber divider
        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        divider.setSpacingBefore(6);
        PdfPCell line = new PdfPCell();
        line.setBackgroundColor(AMBER);
        line.setFixedHeight(3);
        line.setBorder(Rectangle.NO_BORDER);
        divider.addCell(line);
        doc.add(divider);
    }

    // ── Customer + Bill meta ─────────────────────────────────────────────────

    private void addCustomerAndMetaBlock(Document doc, Bill bill) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{1f, 1f});

        t.addCell(infoBox("BILL TO",
                line(nullSafe(bill.getCustomerName()), font(10, Font.BOLD, SLATE_900)),
                line(nullSafe(bill.getCustomerPhone()), font(9, Font.NORMAL, SLATE_500)),
                line(nullSafe(bill.getCustomerAddress()), font(9, Font.NORMAL, SLATE_500))));

        t.addCell(infoBox("BILL DETAILS",
                para(labelValue("Status", bill.getStatus().name()), font(9, Font.NORMAL, SLATE_500)),
                para(labelValue("Currency", bill.getCurrencyCode()), font(9, Font.NORMAL, SLATE_500)),
                para(labelValue("Payment", nullSafe(bill.getPaymentMethod())), font(9, Font.NORMAL, SLATE_500))));

        doc.add(t);
    }

    // ── Items table ─────────────────────────────────────────────────────────

    private void addItemsTable(Document doc, Bill bill) throws Exception {
        // Columns: # | Item | Weight | Rate/g | Making | Discount | Tax | Amount | QR
        PdfPTable t = new PdfPTable(9);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{0.3f, 2.4f, 0.8f, 1.0f, 1.0f, 1.0f, 0.9f, 1.2f, 1.1f});

        // Section label
        PdfPCell heading = new PdfPCell(new Phrase("ITEMS", font(8, Font.BOLD, AMBER)));
        heading.setColspan(9);
        heading.setBorder(Rectangle.NO_BORDER);
        heading.setPaddingBottom(6);
        t.addCell(heading);

        // Column headers
        String[] headers = {"#", "Item", "Weight", "Rate/g", "Making", "Discount", "Tax", "Amount", "QR"};
        for (String h : headers) addColumnHeader(t, h);

        // Rows
        int idx = 1;
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (BillItem item : bill.getItems()) {
            boolean shade = idx % 2 == 0;
            Color bg = shade ? SLATE_100 : WHITE;

            addBodyCell(t, String.valueOf(idx++), bg, Element.ALIGN_CENTER);
            addItemDescCell(t, item, bg);
            addBodyCell(t, item.getWeight().toPlainString() + " g", bg, Element.ALIGN_RIGHT);
            addBodyCell(t, money(item.getRatePerGram(), bill.getCurrencyCode()), bg, Element.ALIGN_RIGHT);
            addBodyCell(t, money(item.getMakingCharge(), bill.getCurrencyCode()), bg, Element.ALIGN_RIGHT);
            addDiscountCell(t, item.getDiscountAmount(), bill.getCurrencyCode(), bg);
            addBodyCell(t, money(item.getTaxAmount(), bill.getCurrencyCode()), bg, Element.ALIGN_RIGHT);

            PdfPCell amtCell = bodyCell(money(item.getFinalPrice(), bill.getCurrencyCode()), bg);
            amtCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            Font boldAmber = font(9, Font.BOLD, AMBER_DARK);
            amtCell.setPhrase(new Phrase(money(item.getFinalPrice(), bill.getCurrencyCode()), boldAmber));
            t.addCell(amtCell);

            addQrCell(t, item.getJewelleryId(), bg);
            totalWeight = totalWeight.add(item.getWeight());
        }

        // Footer row
        PdfPCell footerLabel = new PdfPCell(new Phrase(bill.getItems().size() + " item(s) · Total weight: " + totalWeight.toPlainString() + " g", font(8, Font.ITALIC, SLATE_500)));
        footerLabel.setColspan(7);
        footerLabel.setBackgroundColor(AMBER_BG);
        footerLabel.setBorderColor(BORDER);
        footerLabel.setPadding(6);
        t.addCell(footerLabel);

        PdfPCell totalCell = new PdfPCell(new Phrase(money(bill.getTotalAmount(), bill.getCurrencyCode()), font(10, Font.BOLD, AMBER_DARK)));
        totalCell.setColspan(2);
        totalCell.setBackgroundColor(AMBER_BG);
        totalCell.setBorderColor(BORDER);
        totalCell.setPadding(6);
        totalCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(totalCell);

        doc.add(t);
    }

    // ── Totals block ──────────────────────────────────────────────────────

    private void addTotalsBlock(Document doc, Bill bill) throws Exception {
        BigDecimal totalMaking  = sum(bill, BillItem::getMakingCharge);
        BigDecimal totalDiscount = sum(bill, BillItem::getDiscountAmount);
        BigDecimal totalTax     = sum(bill, BillItem::getTaxAmount);
        BigDecimal grand        = bill.getTotalAmount();

        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.setWidths(new float[]{1f, 1f});

        PdfPCell blank = noBorderCell();
        outer.addCell(blank);

        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(100);

        if (!totalMaking.equals(BigDecimal.ZERO)) {
            addSummaryRow(summary, "Making charges", money(totalMaking, bill.getCurrencyCode()), false);
        }
        if (!totalDiscount.equals(BigDecimal.ZERO)) {
            addSummaryRow(summary, "Total discount", "−" + money(totalDiscount, bill.getCurrencyCode()), true);
        }
        if (!totalTax.equals(BigDecimal.ZERO)) {
            addSummaryRow(summary, "Tax", money(totalTax, bill.getCurrencyCode()), false);
        }

        // Grand total row
        PdfPCell gtLabel = new PdfPCell(new Phrase("GRAND TOTAL", font(11, Font.BOLD, SLATE_900)));
        gtLabel.setBackgroundColor(AMBER_BG);
        gtLabel.setBorderColor(AMBER);
        gtLabel.setPadding(8);
        summary.addCell(gtLabel);
        PdfPCell gtValue = new PdfPCell(new Phrase(money(grand, bill.getCurrencyCode()), font(12, Font.BOLD, AMBER_DARK)));
        gtValue.setBackgroundColor(AMBER_BG);
        gtValue.setBorderColor(AMBER);
        gtValue.setPadding(8);
        gtValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.addCell(gtValue);

        PdfPCell summaryCell = new PdfPCell(summary);
        summaryCell.setBorder(Rectangle.NO_BORDER);
        outer.addCell(summaryCell);

        doc.add(outer);
    }

    // ── Notes ────────────────────────────────────────────────────────────

    private void addNotesBlock(Document doc, String notes) throws Exception {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(AMBER_BG);
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        cell.addElement(para("Notes", font(8, Font.BOLD, AMBER)));
        cell.addElement(para(notes, font(9, Font.NORMAL, SLATE_900)));
        t.addCell(cell);
        doc.add(t);
    }

    // ── Footer ────────────────────────────────────────────────────────────

    private void addFooter(Document doc, Bill bill) throws Exception {
        Tenant tenant = tenantRepository.findById(bill.getTenantId()).orElse(null);

        if (bill.getCreatedAt() != null) {
            String processedTime = bill.getCreatedAt()
                    .atZone(ZoneId.systemDefault())
                    .format(DATE_TIME_FMT);
            Paragraph processed = para("Processed time: " + processedTime, font(7, Font.NORMAL, SLATE_500));
            processed.setAlignment(Element.ALIGN_RIGHT);
            processed.setSpacingBefore(4);
            processed.setSpacingAfter(2);
            doc.add(processed);
        }

        PdfPTable divider = new PdfPTable(1);
        divider.setWidthPercentage(100);
        PdfPCell line = new PdfPCell();
        line.setBackgroundColor(AMBER);
        line.setFixedHeight(2);
        line.setBorder(Rectangle.NO_BORDER);
        divider.addCell(line);
        doc.add(divider);

        String footerNote = tenant == null || tenant.getReceiptFooterNote() == null || tenant.getReceiptFooterNote().isBlank()
                ? "Thank you for your purchase! Each QR code uniquely identifies the jewellery item — scan to verify."
                : tenant.getReceiptFooterNote();
        Paragraph thanks = para(footerNote, font(8, Font.ITALIC, SLATE_500));
        thanks.setAlignment(Element.ALIGN_CENTER);
        thanks.setSpacingBefore(6);
        doc.add(thanks);

        Paragraph built = para("Powered by JewellKeeper · Developed by Autowhap · https://www.autowhap.com", font(7, Font.NORMAL, new Color(180, 180, 180)));
        built.setAlignment(Element.ALIGN_CENTER);
        built.setSpacingBefore(2);
        doc.add(built);
    }

    // ── Cell helpers ─────────────────────────────────────────────────────

    private void addColumnHeader(PdfPTable t, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font(8, Font.BOLD, WHITE)));
        cell.setBackgroundColor(AMBER);
        cell.setBorderColor(AMBER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(text.equals("#") || text.equals("QR") ? Element.ALIGN_CENTER : Element.ALIGN_LEFT);
        t.addCell(cell);
    }

    private void addItemDescCell(PdfPTable t, BillItem item, Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(para(item.getTypeNameSnapshot(), font(9, Font.BOLD, SLATE_900)));
        if (item.getDesignNameSnapshot() != null && !item.getDesignNameSnapshot().isBlank()) {
            cell.addElement(para(item.getDesignNameSnapshot(), font(8, Font.NORMAL, SLATE_500)));
        }
        cell.addElement(para(item.getKaratSnapshot(), font(8, Font.ITALIC, SLATE_500)));
        t.addCell(cell);
    }

    private void addBodyCell(PdfPTable t, String text, Color bg, int align) {
        PdfPCell cell = bodyCell(text, bg);
        cell.setHorizontalAlignment(align);
        t.addCell(cell);
    }

    private void addDiscountCell(PdfPTable t, BigDecimal amount, String currency, Color bg) {
        String text = (amount != null && amount.compareTo(BigDecimal.ZERO) > 0)
                ? "−" + money(amount, currency)
                : "—";
        PdfPCell cell = new PdfPCell(new Phrase(text, font(9, Font.NORMAL, amount != null && amount.compareTo(BigDecimal.ZERO) > 0 ? GREEN : SLATE_500)));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(cell);
    }

    private void addQrCell(PdfPTable t, UUID jewelleryId, Color bg) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setPadding(4);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try {
            jewelleryRepository.findById(jewelleryId).ifPresent(jewellery -> {
                try {
                    byte[] qrBytes = qrCodeService.generatePngBytes(jewellery.getQrPayloadToken());
                    Image qr = Image.getInstance(qrBytes);
                    qr.scaleToFit(52, 52);
                    cell.addElement(qr);
                } catch (Exception ignored) {
                    cell.addElement(para("—", font(8, Font.NORMAL, SLATE_500)));
                }
            });
        } catch (Exception ignored) {
            cell.addElement(para("—", font(8, Font.NORMAL, SLATE_500)));
        }
        t.addCell(cell);
    }

    private PdfPCell bodyCell(String text, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(nullSafe(text), font(9, Font.NORMAL, SLATE_900)));
        cell.setBackgroundColor(bg);
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell noBorderCell() {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        return cell;
    }

    private PdfPCell infoBox(String title, Paragraph... lines) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(AMBER_BG);
        cell.setBorderColor(BORDER);
        cell.setPadding(8);
        cell.addElement(para(title, font(7, Font.BOLD, AMBER)));
        for (Paragraph p : lines) {
            if (p != null) cell.addElement(p);
        }
        return cell;
    }

    private Paragraph line(String text, Font f) {
        if (text == null || text.isBlank()) return null;
        return para(text, f);
    }

    private String labelValue(String label, String value) {
        return label + ": " + (value == null || value.isBlank() ? "—" : value);
    }

    private void addSummaryRow(PdfPTable t, String label, String value, boolean green) {
        PdfPCell lCell = new PdfPCell(new Phrase(label, font(9, Font.NORMAL, SLATE_500)));
        lCell.setBorderColor(BORDER);
        lCell.setPadding(6);
        t.addCell(lCell);
        PdfPCell vCell = new PdfPCell(new Phrase(value, font(9, Font.NORMAL, green ? GREEN : SLATE_900)));
        vCell.setBorderColor(BORDER);
        vCell.setPadding(6);
        vCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(vCell);
    }

    // ── Utilities ─────────────────────────────────────────────────────────

    private Font font(int size, int style, Color color) {
        return new Font(Font.HELVETICA, size, style, color);
    }

    private Paragraph para(String text, Font f) {
        return new Paragraph(nullSafe(text), f);
    }

    private void gap(Document doc) throws Exception {
        doc.add(new Paragraph(" ", font(5, Font.NORMAL, WHITE)));
    }

    private void addOptional(PdfPCell cell, String text, Font font) {
        if (text != null && !text.isBlank()) {
            cell.addElement(para(text, font));
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
            logo.scaleAbsolute(width, height);
            logo.setAlignment(Element.ALIGN_CENTER);
            return logo;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String money(BigDecimal amount, String currency) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return "—";
        return currency + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String nullSafe(String v) {
        return v == null ? "" : v;
    }

    private BigDecimal sum(Bill bill, java.util.function.Function<BillItem, BigDecimal> fn) {
        return bill.getItems().stream()
                .map(fn)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
