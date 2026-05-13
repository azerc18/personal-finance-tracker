package com.example.finance.utils;

import com.example.finance.dto.reportdto.ReportData;
import com.example.finance.dto.reportdto.ReportPDFRequest;
import com.example.finance.entity.Transaction;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class PdfGenerator {
    public static byte[] generateSummaryPdf(ReportData data, ReportPDFRequest request){
        Document document = new Document(PageSize.A4);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try{
            PdfWriter.getInstance(document, bos);
            document.open();

            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            document.add(new Paragraph("SUMMARY REPORT - " + request.getYear(),fontHeader));
            document.add(new Paragraph());

            //Table A
            PdfPTable tableA = new PdfPTable(2);
            tableA.addCell("Item");
            tableA.addCell("Amount");
            tableA.addCell("Income");
            tableA.addCell("$" + data.getTotalIncome());
            tableA.addCell("Expenses");
            tableA.addCell("$" + data.getTotalExpense());
            tableA.addCell("Balance");
            tableA.addCell("$" + data.getTotalIncome().subtract(data.getTotalExpense()));
            tableA.addCell("Is Excess");
            tableA.addCell((data.isExcess()) ? "Yes" :  "No" );
            document.add(tableA);

            //Table B
            if(request.getIncludeTopExpenses() && !data.getTopExpenseList().isEmpty()){
                document.add(new Paragraph("\n Top 3 Expenses:"));
                PdfPTable tableB = new PdfPTable(3);
                tableB.addCell("Category");
                tableB.addCell("Amount");
                tableB.addCell("Percentage");

                data.getTopExpenseList().forEach(item
                        -> {
                    tableB.addCell(item.getCategory());
                    tableB.addCell("$" + item.getAmount());
                    tableB.addCell(item.getPercentage() + "%");
                });

                document.add(tableB);
            }

            //Chart
            if(request.getIncludeChart() != null && request.getIncludeChart()){
                switch (request.getReportType()){
                    case SUMMARY -> {
                        byte[] chartImgA = ChartGenerator.generateIncomeAndExpenseChart(
                                data.getTotalIncome(),
                                data.getTotalExpense());

                        Map<String, BigDecimal> expenseMap = data.getTopExpenseList()
                                .stream()
                                .collect(Collectors.toMap(
                                        ReportData.ExpenseItems::getCategory,
                                        ReportData.ExpenseItems::getAmount,
                                        (v1,v2) -> v1,
                                        LinkedHashMap::new
                                ));

                        byte[] chartImgB = ChartGenerator.generateHorizonChart(expenseMap, "Top 3 Expense");

                        Image imgA = Image.getInstance(chartImgA);
                        Image imgB = Image.getInstance(chartImgB);

                        imgA.setAlignment(Element.ALIGN_CENTER);
                        imgB.setAlignment(Element.ALIGN_CENTER);

                        document.add(imgA);
                        document.add(imgB);
                    }

                    case MONTHLY -> {
                        Map<Integer, BigDecimal> monthlyIncome = data.getTransactions()
                                .stream()
                                .filter(t -> "INCOME".equalsIgnoreCase(t.getCategory().getType().name()))
                                .collect(Collectors.groupingBy(
                                        t -> t.getDate().getMonthValue(),
                                        Collectors.mapping(
                                                Transaction::getAmount,
                                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                                        )
                                ));

                        Map<Integer, BigDecimal> monthlyExpense = data.getTransactions()
                                .stream()
                                .filter(t -> "EXPENSE".equalsIgnoreCase(t.getCategory().getType().name()))
                                .collect(Collectors.groupingBy(
                                        t -> t.getDate().getMonthValue(),
                                        Collectors.mapping(
                                                Transaction::getAmount,
                                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add)
                                        )
                                ));

                        byte[] chartImg = ChartGenerator.generateLineChart(
                                monthlyIncome,
                                monthlyExpense,
                                request.getYear());

                        Image image = Image.getInstance(chartImg);
                        image.setAlignment(Element.ALIGN_CENTER);
                        document.add(image);
                    }

                    case CATEGORY -> {
                        Map<String, BigDecimal> pieDataByIncome = data.getTransactions()
                                .stream()
                                .filter(t-> "INCOME".equalsIgnoreCase(t.getCategory().getType().name()))
                                .collect(Collectors.groupingBy(
                                        t -> t.getCategory().getName(),
                                        Collectors.mapping(
                                                Transaction::getAmount,
                                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                                ));

                        Map<String, BigDecimal> pieDataByExpense = data.getTransactions()
                                .stream()
                                .filter(t-> "EXPENSE".equalsIgnoreCase(t.getCategory().getType().name()))
                                .collect(Collectors.groupingBy(
                                        t -> t.getCategory().getName(),
                                        Collectors.mapping(
                                                Transaction::getAmount,
                                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                                ));

                        byte[] chartImgA = ChartGenerator.generatePieChart(pieDataByIncome, "Pie Chart by Income");
                        byte[] chartImgB = ChartGenerator.generatePieChart(pieDataByExpense, "Pie Chart by Expense");
                        Image imageA = Image.getInstance(chartImgA);
                        Image imageB = Image.getInstance(chartImgB);

                        imageA.setAlignment(Element.ALIGN_CENTER);
                        imageB.setAlignment(Element.ALIGN_CENTER);

                        document.add(imageA);
                        document.add(imageB);
                    }
                }
            }

            if (data.getTotalExpense().equals(BigDecimal.ZERO) && data.getTotalIncome().equals(BigDecimal.ZERO)) {
                document.add(new Paragraph("No transactions available for this period."));
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        document.close();
        return bos.toByteArray();
    }
}
