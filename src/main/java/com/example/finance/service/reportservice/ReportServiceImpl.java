package com.example.finance.service.reportservice;

import com.example.finance.dto.reportdto.*;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import com.example.finance.enums.ReportType;
import com.example.finance.repo.TransactionRepo;
import com.example.finance.repo.UserRepo;
import com.example.finance.service.fileservice.FileStorageService;
import com.example.finance.utils.EmailGenerator;
import com.example.finance.utils.PdfGenerator;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService{
    private final TransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final FileStorageService fileStorageService;
    private final EmailGenerator emailGenerator;

    @Override
    public ReportCategoryResponse getCategoryReport(CategoryName  type, int month, int year, String email) {

        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email is not found"));

        Map<String, BigDecimal> transactionMap = transactionRepo.findByUserIdAndTypeAndMonthYear(user.getId(), type, month, year)
                .stream().collect(Collectors.groupingBy(transaction -> transaction.getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        BigDecimal total = transactionMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<ReportCategoryResponse.CategoryData> categoryData = transactionMap.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    BigDecimal amount = entry.getValue();

                    BigDecimal percentage = total.compareTo(BigDecimal.ZERO) > 0
                            ? amount.multiply(BigDecimal.valueOf(100))
                            .divide(total, 1, RoundingMode.HALF_UP): BigDecimal.ZERO;

                    return new ReportCategoryResponse.CategoryData(category, amount, percentage);
                })
                .toList();

        return new ReportCategoryResponse(type, total, categoryData);
    }

    @Override
    public ReportMonthlyResponse getMonthlyReport(Integer month, int year, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User is not found"));

        int monthValue = (month == null)
                ? LocalDate.now().getMonthValue()
                : month;

        if (monthValue < 1 || monthValue > 12) {
            throw new IllegalArgumentException("Invalid month: " + monthValue);
        }

        String monthName = Month.of(monthValue).name();

        String[] allMonths = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

        List<Object[]> rawChartData = transactionRepo.getMonthlyChartData(user.getId(), year);

        List<ReportMonthlyResponse.ChartData> chartData = rawChartData
                .stream()
                .map(objects -> {

                    int i = ((Number) objects[0]).intValue() - 1;
                    String mName = (i >= 0 && i < 12) ? allMonths[i] : "UNKNOWN";

                    return new ReportMonthlyResponse.ChartData(
                            mName,
                            objects[1] != null ? new BigDecimal(objects[1].toString()) : BigDecimal.ZERO,
                            objects[2] != null ? new BigDecimal(objects[2].toString()) : BigDecimal.ZERO
                    );
                })
                .toList();

        chartData = fillingMissingData(chartData);

        List<Object[]> summaryList = transactionRepo.getSummary(user.getId(), year, month);


        Object[] rawSummary = summaryList.isEmpty() ? null : summaryList.get(0);

        ReportMonthlyResponse.Summary summary;

        if(rawSummary == null){
            summary = new ReportMonthlyResponse.Summary(monthName + " " + year, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        else {
            summary = new ReportMonthlyResponse.Summary(
                    rawSummary[0] != null ? rawSummary[0].toString() : "",
                    rawSummary[1] != null ? new BigDecimal(rawSummary[1].toString()) : BigDecimal.ZERO,
                    rawSummary[2] != null ? new BigDecimal(rawSummary[2].toString()) : BigDecimal.ZERO
            );
        }

        return new ReportMonthlyResponse(chartData, summary);
    }

    private List<ReportMonthlyResponse.ChartData> fillingMissingData(List<ReportMonthlyResponse.ChartData> chartData) {
        Map<String, ReportMonthlyResponse.ChartData> dataMap = chartData.stream()
                .collect(Collectors.toMap(
                        ReportMonthlyResponse.ChartData::getMonthName,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        String[] allMonths = {"JAN", "FEB", "MAR", "APR", "MAY", "JUN",
                "JUL", "AUG", "SEP", "OCT", "NOV", "DEC"};

        return Arrays.stream(allMonths)
                .map(month -> dataMap.getOrDefault(month,
                        new ReportMonthlyResponse.ChartData(month, BigDecimal.ZERO, BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    @Override
    public ReportPDFResponse getPdfReport(ReportPDFRequest request, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));

        List<Transaction> transactions = transactionRepo.findByUserAndMonthAndYear(user, request.getMonth(), request.getYear());

        ReportData data = calculateData(transactions, user, request);

        byte[] pdfContent = PdfGenerator.generateSummaryPdf(data, request);

        String fileName = String.format("%d_%s_%d_%d.pdf", user.getId(), request.getReportType(), request.getMonth(), request.getYear());
        String downloadUrl = fileStorageService.saveFile(pdfContent, fileName);

        return new ReportPDFResponse(downloadUrl);

    }

    private ReportData calculateData(List<Transaction> transactions, User user, ReportPDFRequest request){
        BigDecimal totalIncome = Optional.ofNullable(transactionRepo.getTotalIncomeByMonthAndYear(user, request.getMonth(), request.getYear())).orElse(BigDecimal.ZERO);
        BigDecimal totalExpense = Optional.ofNullable(transactionRepo.getTotalExpenseByMonthAndYear(user, request.getMonth(), request.getYear())).orElse(BigDecimal.ZERO);

        BigDecimal balance = totalIncome.subtract(totalExpense);
        boolean isExcess = balance.compareTo(BigDecimal.ZERO) > 0;

        List<ReportData.ExpenseItems> expenseItems = transactionRepo.findTopCategoriesByAmount(user, request.getMonth(), request.getYear(), PageRequest.of(0, 3))
                .stream()
                .map(entry -> {
                    return ReportData.ExpenseItems.builder()
                       .amount(entry.getTotalAmount())
                       .category(entry.getCategoryName())
                       .percentage((totalExpense.compareTo(BigDecimal.ZERO) > 0)
                               ? (entry.getTotalAmount().doubleValue() / totalExpense.doubleValue()) * 100
                               : 0.0)
                       .build();
                })
                .toList();

        return new ReportData(
                totalIncome,
                totalExpense,
                isExcess,
                transactions,
                expenseItems
        );
    }

    @Override
    @Async
    public void getEmailReport(ReportEmailRequest request, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));

        List<Transaction> transactions = transactionRepo.findByUserAndMonthAndYear(user, request.getMonth(), request.getYear());
        ReportData data = calculateData(transactions, user, toPdf(request));
        byte[] pdfContent = PdfGenerator.generateSummaryPdf(data, toPdf(request));

        String fileName = String.format("%d_%s_%d_%d.pdf", user.getId(), request.getReportType(), request.getMonth(), request.getYear());
        String filePath =  fileStorageService.saveFile(pdfContent, fileName);

        try{
            emailGenerator.getEmailReport(request.getEmail(), filePath, fileName);
        }catch (MessagingException ex){
            throw new RuntimeException("Error: " + ex.getMessage());
        }
    }

    private ReportPDFRequest toPdf(ReportEmailRequest request){
        return new ReportPDFRequest(
                request.getMonth(),
                request.getYear(),
                request.getIncludeChart(),
                request.getIncludeTopExpenses(),
                ReportType.SUMMARY
                );
    }
}
