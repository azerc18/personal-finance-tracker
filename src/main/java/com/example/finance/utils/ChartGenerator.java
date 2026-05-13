package com.example.finance.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

public class ChartGenerator {

    //Bar Chart Income compare to Expense (for Summary)
    public static byte[] generateIncomeAndExpenseChart(BigDecimal income, BigDecimal expense) throws IOException {
        //Create dataset
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        //Add value
        dataset.addValue(income, "Amount", "Income");
        dataset.addValue(expense, "Amount", "Expense");

        JFreeChart chart = ChartFactory.createBarChart(
                "Income - Expense","Type", "Amount",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ChartUtilities.writeChartAsJPEG(bos, chart, 400, 300);
        return bos.toByteArray();
    }

    //Horizontal Chart for Top Expense(for Summary)
    public static byte[] generateHorizonChart(Map<String, BigDecimal> topExpense, String title) throws IOException{
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        topExpense.forEach((category, amount) -> {
            dataset.addValue(amount.doubleValue(), "Expense", category);
        });

        JFreeChart chart = ChartFactory.createBarChart(
                title,
                "Type",
                "Amount",
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                false
        );

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ChartUtilities.writeChartAsJPEG(bos, chart, 500, 300);
        return bos.toByteArray();
    }

    //Line chart (for Monthly)
        public static byte[] generateLineChart(Map<Integer, BigDecimal> monthlyIncome,
                                               Map<Integer, BigDecimal> monthlyExpense,
                                               int year) throws IOException{
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        for(int month = 1; month <= 12; month++){
            String monthLabel = "Month" + month;

            BigDecimal income = monthlyIncome.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal expense = monthlyExpense.getOrDefault(month, BigDecimal.ZERO);

            dataset.addValue(income.doubleValue(), "Income", monthLabel);
            dataset.addValue(expense.doubleValue(), "Expense", monthLabel);
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Monthly Income and Outcome year" + year,
                "Month",
                "Amount",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ChartUtilities.writeChartAsJPEG(bos, chart, 600, 400);
        return bos.toByteArray();
    }

    //Pie Chart (for Category)
    public static byte[] generatePieChart(Map<String, BigDecimal> data, String title) throws IOException{
        DefaultPieDataset dataset = new DefaultPieDataset();

        data.forEach(dataset::setValue);

        JFreeChart chart = ChartFactory.createPieChart(
                title, dataset);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ChartUtilities.writeChartAsJPEG(bos, chart, 400, 300);
        return bos.toByteArray();
    }
}
