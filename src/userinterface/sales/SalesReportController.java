package userinterface.sales;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import logic.DAO.SaleDAO;
import logic.DTO.SaleDTO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import logic.DTO.SaleDTO;
import logic.DTO.SaleStatus;

public class SalesReportController {

    private static final String SALES_REPORT_DIR_NAME = "ImperialReports";
    private static final String SALES_REPORT_FILE_PREFIX = "reporte_ventas_";

    private static final float REPORT_MARGIN = 50f;
    private static final float REPORT_LINE_HEIGHT = 16f;
    private static final float REPORT_MIN_Y = 60f;

    private static final DateTimeFormatter FILE_NAME_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private static final DateTimeFormatter GENERATED_AT_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DateTimeFormatter SALE_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    private AnchorPane rootPane;

    @FXML
    private Button btnWeek;

    @FXML
    private Button btnMonth;

    @FXML
    private Button btnYear;

    @FXML
    private Button btnAll;

    private final SaleDAO saleDao = new SaleDAO();

    @FXML
    private void initialize() {
        // Nada especial por ahora
    }

    @FXML
    private void onPrintWeek() {
        try {
            List<SaleDTO> sales = saleDao.getSalesForCurrentWeek();
            if (sales.isEmpty()) {
                showInfo("No hay ventas registradas en la semana actual.");
                return;
            }
            exportSalesReportToPdf(sales, "Ventas de esta semana");
        } catch (SQLException | IOException ex) {
            showError("Error al generar el reporte de la semana: " + ex.getMessage());
        }
    }

    @FXML
    private void onPrintMonth() {
        try {
            List<SaleDTO> sales = saleDao.getSalesForCurrentMonth();
            if (sales.isEmpty()) {
                showInfo("No hay ventas registradas en el mes actual.");
                return;
            }
            exportSalesReportToPdf(sales, "Ventas de este mes");
        } catch (SQLException | IOException ex) {
            showError("Error al generar el reporte del mes: " + ex.getMessage());
        }
    }

    @FXML
    private void onPrintYear() {
        try {
            List<SaleDTO> sales = saleDao.getSalesForCurrentYear();
            if (sales.isEmpty()) {
                showInfo("No hay ventas registradas en el año actual.");
                return;
            }
            exportSalesReportToPdf(sales, "Ventas de este año");
        } catch (SQLException | IOException ex) {
            showError("Error al generar el reporte del año: " + ex.getMessage());
        }
    }

    @FXML
    private void onPrintAll() {
        try {
            List<SaleDTO> sales = saleDao.getAllSales();
            if (sales.isEmpty()) {
                showInfo("No hay ventas registradas.");
                return;
            }
            exportSalesReportToPdf(sales, "Todas las ventas");
        } catch (SQLException | IOException ex) {
            showError("Error al generar el reporte de todas las ventas: " + ex.getMessage());
        }
    }

    private void exportSalesReportToPdf(List<SaleDTO> sales, String periodLabel) throws IOException {

        Path baseDir = Paths.get(System.getProperty("user.home"), SALES_REPORT_DIR_NAME);
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }

        String timestamp = LocalDateTime.now().format(FILE_NAME_TIMESTAMP_FORMATTER);
        String defaultFileName = SALES_REPORT_FILE_PREFIX + timestamp + ".pdf";

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte de ventas");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF (*.pdf)", "*.pdf"));

        File initialDir = baseDir.toFile();
        if (initialDir.exists() && initialDir.isDirectory()) {
            fileChooser.setInitialDirectory(initialDir);
        }

        fileChooser.setInitialFileName(defaultFileName);

        Window window = rootPane != null && rootPane.getScene() != null
                ? rootPane.getScene().getWindow()
                : null;

        File selectedFile = fileChooser.showSaveDialog(window);
        if (selectedFile == null) {
            showInfo("Guardado cancelado.");
            return;
        }

        Path outputPath = selectedFile.toPath();

        try (PDDocument document = new PDDocument()) {

            // Página en horizontal (landscape) sin usar rotate()
            PDRectangle baseSize = PDRectangle.LETTER;
            PDRectangle landscape = new PDRectangle(baseSize.getHeight(), baseSize.getWidth());
            PDPage page = new PDPage(landscape);
            document.addPage(page);

            PDRectangle mediaBox = page.getMediaBox();
            float pageWidth = mediaBox.getWidth();
            float pageHeight = mediaBox.getHeight();

            try (PDPageContentStream contentStream =
                         new PDPageContentStream(document, page)) {

                float y = pageHeight - REPORT_MARGIN;

                // Título
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
                contentStream.beginText();
                contentStream.newLineAtOffset(REPORT_MARGIN, y);
                contentStream.showText("Imperial Motors - Reporte de ventas");
                contentStream.endText();

                y -= REPORT_LINE_HEIGHT * 2;

                // Fecha de generación
                contentStream.setFont(PDType1Font.HELVETICA, 10);
                String nowText = "Generado: " + LocalDateTime.now().format(GENERATED_AT_FORMATTER);
                contentStream.beginText();
                contentStream.newLineAtOffset(REPORT_MARGIN, y);
                contentStream.showText(nowText);
                contentStream.endText();

                y -= REPORT_LINE_HEIGHT;

                // Periodo
                contentStream.beginText();
                contentStream.newLineAtOffset(REPORT_MARGIN, y);
                contentStream.showText("Periodo: " + periodLabel);
                contentStream.endText();

                y -= REPORT_LINE_HEIGHT * 2;

                // Definición de columnas (más anchas para folio y fecha)
                final float colFolio = REPORT_MARGIN;
                final float colCliente = colFolio + 160f;
                final float colVehiculo = colCliente + 140f;
                final float colTotal = colVehiculo + 140f;
                final float colEstado = colTotal + 100f;
                final float colFecha = colEstado + 100f;

                final float endLineX = pageWidth - REPORT_MARGIN;

                // Encabezados
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);

                contentStream.beginText();
                contentStream.newLineAtOffset(colFolio, y);
                contentStream.showText("Folio");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(colCliente, y);
                contentStream.showText("Cliente");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(colVehiculo, y);
                contentStream.showText("Vehículo");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(colTotal, y);
                contentStream.showText("Total");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(colEstado, y);
                contentStream.showText("Estado");
                contentStream.endText();

                contentStream.beginText();
                contentStream.newLineAtOffset(colFecha, y);
                contentStream.showText("Fecha");
                contentStream.endText();

                y -= REPORT_LINE_HEIGHT;

                // Línea horizontal
                contentStream.moveTo(REPORT_MARGIN, y);
                contentStream.lineTo(endLineX, y);
                contentStream.stroke();

                y -= REPORT_LINE_HEIGHT;

                // Datos
                contentStream.setFont(PDType1Font.HELVETICA, 9);

                BigDecimal totalGeneral = BigDecimal.ZERO;

                for (SaleDTO sale : sales) {
                    if (y < REPORT_MIN_Y) {
                        break; // por simplicidad: no hacemos multi-página todavía
                    }

                    String folio = safe(sale.getFolio()); // sin truncar
                    String customer = truncate(safe(sale.getCostumerNumber()), 25);

                    String vehicle = (sale.getVehicleId() == null)
                            ? ""
                            : "ID " + sale.getVehicleId();
                    vehicle = truncate(vehicle, 20);

                    String total = (sale.getTotal() == null)
                            ? ""
                            : sale.getTotal().toPlainString();

                    String status = (sale.getStatus() == null)
                            ? ""
                            : sale.getStatus().name();

                    LocalDateTime createdAt = sale.getCreatedAt();
                    String dateText = createdAt == null
                            ? ""
                            : createdAt.format(SALE_DATETIME_FORMATTER);

                    // 👇 Solo sumar si NO está anulada
                    if (sale.getTotal() != null
                            && sale.getStatus() != null
                            && sale.getStatus() != SaleStatus.ANULADA) {
                        totalGeneral = totalGeneral.add(sale.getTotal());
                    }

                    contentStream.beginText();
                    contentStream.newLineAtOffset(colFolio, y);
                    contentStream.showText(folio);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(colCliente, y);
                    contentStream.showText(customer);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(colVehiculo, y);
                    contentStream.showText(vehicle);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(colTotal, y);
                    contentStream.showText(total);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(colEstado, y);
                    contentStream.showText(status);
                    contentStream.endText();

                    contentStream.beginText();
                    contentStream.newLineAtOffset(colFecha, y);
                    contentStream.showText(dateText);
                    contentStream.endText();

                    y -= REPORT_LINE_HEIGHT;
                }

                y -= REPORT_LINE_HEIGHT;

                // Totales
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                contentStream.beginText();
                contentStream.newLineAtOffset(REPORT_MARGIN, y);
                contentStream.showText("Total de ventas listadas: " + sales.size());
                contentStream.endText();

                y -= REPORT_LINE_HEIGHT;

                contentStream.beginText();
                contentStream.newLineAtOffset(REPORT_MARGIN, y);
                contentStream.showText("Suma total: " + totalGeneral.toPlainString());
                contentStream.endText();
            }

            document.save(outputPath.toFile());
        }

        showInfo("Reporte generado en:\n" + outputPath.toAbsolutePath());
    }

    private String truncate(String value, int maxLength) {
        String safeValue = safe(value);
        if (safeValue.length() <= maxLength) {
            return safeValue;
        }
        return safeValue.substring(0, maxLength - 3) + "...";
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private void showError(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Error");
        a.setHeaderText(null);
        a.setContentText(text);
        a.showAndWait();
    }

    private void showInfo(String text) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("Información");
        a.setHeaderText(null);
        a.setContentText(text);
        a.showAndWait();
    }
}
