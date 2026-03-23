package itmo.code.service;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelExportService {

    public byte[] generateWorkbook(List<AnalyzedTranslation> rows, double average) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet resultsSheet = workbook.createSheet("Results");
            fillResultsSheet(resultsSheet, rows, average);

            Sheet scaleSheet = workbook.createSheet("Scale");
            fillScaleSheet(workbook, scaleSheet);

            List<SummaryRow> summaryRows = buildSummaryRows(rows);

            Sheet chartSheet = workbook.createSheet("Chart");
            addChartAsImage(workbook, chartSheet, summaryRows);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void fillResultsSheet(Sheet sheet, List<AnalyzedTranslation> rows, double average) {
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("№");
        header.createCell(1).setCellValue("Назначенный уровень");
        header.createCell(2).setCellValue("Перевод");
        header.createCell(3).setCellValue("level_number");
        header.createCell(4).setCellValue("level_comment");
        header.createCell(5).setCellValue("Попадание в диапазон");

        for (int i = 0; i < rows.size(); i++) {
            AnalyzedTranslation item = rows.get(i);
            Row row = sheet.createRow(i + 1);

            row.createCell(0).setCellValue(i + 1);
            row.createCell(1).setCellValue(item.targetLevel());
            row.createCell(2).setCellValue(item.translatedText());
            row.createCell(3).setCellValue(item.levelNumber());
            row.createCell(4).setCellValue(item.levelComment());
            row.createCell(5).setCellValue(item.inRange() ? "Да" : "Нет");
        }

        Row avgRow = sheet.createRow(rows.size() + 2);
        avgRow.createCell(0).setCellValue("Среднее арифметическое");
        avgRow.createCell(1).setCellValue(average);

        sheet.setColumnWidth(0, 2500);
        sheet.setColumnWidth(1, 5000);
        sheet.setColumnWidth(2, 26000);
        sheet.setColumnWidth(3, 4500);
        sheet.setColumnWidth(4, 14000);
        sheet.setColumnWidth(5, 6000);
    }

    private void fillScaleSheet(XSSFWorkbook workbook, Sheet sheet) {
        CellStyle redStyle = workbook.createCellStyle();
        Font redFont = workbook.createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redStyle.setFont(redFont);

        Object[][] rows = {
            {"Комментарий", "Минимальное значение", "Максимальное значение"},
            {"A1. Элементарный уровень.", 0.00, 1.00},
            {"Начало A2. Базовый уровень.", 1.01, 1.29},
            {"Середина A2. Базовый уровень.", 1.30, 1.57},
            {"Конец A2. Базовый уровень.", 1.58, 1.79},
            {"Начало B1. I сертификационный уровень.", 1.80, 1.93},
            {"Середина B1. I сертификационный уровень.", 1.94, 2.50},
            {"Конец B1. I сертификационный уровень.", 2.51, 3.21},
            {"Начало B2. II сертификационный уровень.", 3.22, 3.93},
            {"Середина B2. II сертификационный уровень.", 3.94, 4.29},
            {"Конец B2. II сертификационный уровень.", 4.30, 5.00},
            {"Начало C1. III сертификационный уровень.", 5.01, 5.14},
            {"Середина C1. III сертификационный уровень.", 5.15, 5.29},
            {"Конец C1. III сертификационный уровень.", 5.30, 5.36},
            {"C2, уровень носителя. IV сертификационный уровень.", 5.37, 6.07},
            {"C2+, Этот текст сложный даже для носителя", 6.08, "-"}
        };

        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i);
            for (int j = 0; j < rows[i].length; j++) {
                Cell cell = row.createCell(j);
                Object value = rows[i][j];

                if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                } else {
                    cell.setCellValue(String.valueOf(value));
                }
            }
        }

        // границы B1 и B2
        sheet.getRow(5).getCell(1).setCellStyle(redStyle);   // 1.80
        sheet.getRow(7).getCell(2).setCellStyle(redStyle);   // 3.21
        sheet.getRow(8).getCell(1).setCellStyle(redStyle);   // 3.22
        sheet.getRow(10).getCell(2).setCellStyle(redStyle);  // 5.00

        sheet.setColumnWidth(0, 22000);
        sheet.setColumnWidth(1, 9000);
        sheet.setColumnWidth(2, 9000);
    }

    private List<SummaryRow> buildSummaryRows(List<AnalyzedTranslation> rows) {
        Map<String, List<AnalyzedTranslation>> grouped = new LinkedHashMap<>();

        for (AnalyzedTranslation row : rows) {
            grouped.computeIfAbsent(row.targetLevel(), k -> new ArrayList<>()).add(row);
        }

        List<SummaryRow> result = new ArrayList<>();
        List<String> orderedLevels = List.of("B1", "B2");

        for (String level : orderedLevels) {
            if (!grouped.containsKey(level)) {
                continue;
            }

            List<AnalyzedTranslation> groupRows = grouped.get(level);
            long total = groupRows.size();
            long adapted = groupRows.stream().filter(AnalyzedTranslation::inRange).count();

            double adaptedPercent = total == 0 ? 0.0 : adapted * 100.0 / total;
            double notAdaptedPercent = 100.0 - adaptedPercent;

            result.add(new SummaryRow(level, adaptedPercent, notAdaptedPercent));
        }

        for (Map.Entry<String, List<AnalyzedTranslation>> entry : grouped.entrySet()) {
            if (orderedLevels.contains(entry.getKey())) {
                continue;
            }

            List<AnalyzedTranslation> groupRows = entry.getValue();
            long total = groupRows.size();
            long adapted = groupRows.stream().filter(AnalyzedTranslation::inRange).count();

            double adaptedPercent = total == 0 ? 0.0 : adapted * 100.0 / total;
            double notAdaptedPercent = 100.0 - adaptedPercent;

            result.add(new SummaryRow(entry.getKey(), adaptedPercent, notAdaptedPercent));
        }

        return result;
    }

    private void addChartAsImage(XSSFWorkbook workbook, Sheet chartSheet, List<SummaryRow> summaryRows) throws IOException {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for (SummaryRow row : summaryRows) {
            dataset.addValue(row.adaptedPercent(), "Адаптирован на назначенный уровень", row.category());
            dataset.addValue(row.notAdaptedPercent(), "Неадаптирован на назначенный уровень", row.category());
        }

        JFreeChart chart = ChartFactory.createStackedBarChart(
            "Процентное соотношение соответствия сложности адаптации",
            "",
            "Процентное соотношение (%)",
            dataset
        );

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setInsets(new RectangleInsets(10, 10, 10, 10));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setRange(0, 100);
        rangeAxis.setNumberFormatOverride(new DecimalFormat("0' %'"));

        StackedBarRenderer renderer = new StackedBarRenderer(false);
        renderer.setSeriesPaint(0, new Color(31, 119, 180));   // синий
        renderer.setSeriesPaint(1, new Color(255, 127, 14));   // оранжевый
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelGenerator(
            new StandardCategoryItemLabelGenerator("{2}%", NumberFormat.getNumberInstance())
        );
        renderer.setDefaultItemLabelFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 12));
        renderer.setMaximumBarWidth(0.25);

        plot.setRenderer(renderer);

        BufferedImage image = chart.createBufferedImage(1200, 700);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos);
        byte[] bytes = bos.toByteArray();

        int pictureIdx = workbook.addPicture(bytes, XSSFWorkbook.PICTURE_TYPE_PNG);

        XSSFDrawing drawing = (XSSFDrawing) chartSheet.createDrawingPatriarch();
        XSSFClientAnchor anchor = new XSSFClientAnchor();
        anchor.setCol1(0);
        anchor.setRow1(0);
        anchor.setCol2(14);
        anchor.setRow2(30);

        drawing.createPicture(anchor, pictureIdx);
    }

    public record AnalyzedTranslation(
        String targetLevel,
        String translatedText,
        double levelNumber,
        String levelComment,
        boolean inRange
    ) {
    }

    public record SummaryRow(
        String category,
        double adaptedPercent,
        double notAdaptedPercent
    ) {
    }
}