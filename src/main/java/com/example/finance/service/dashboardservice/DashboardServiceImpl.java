package com.example.finance.service.dashboardservice;

import com.example.finance.dto.dashboarddto.DashboardResponse;
import com.example.finance.entity.User;
import com.example.finance.repo.TransactionRepo;
import com.example.finance.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService{
    private final TransactionRepo transactionRepo;
    private final UserRepo userRepo;

    @Override
    public DashboardResponse getDashboard(String userEmail) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));


        BigDecimal totalIncome = transactionRepo.getTotalIncome(user);
        totalIncome = (totalIncome != null) ? totalIncome : BigDecimal.ZERO;

        BigDecimal totalExpense = transactionRepo.getTotalExpense(user);
        totalExpense = (totalExpense != null) ? totalExpense : BigDecimal.ZERO;

        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<DashboardResponse.pieChartData> pieChart = transactionRepo.getPieChart(user)
                .stream()
                .map(pieChartData -> new DashboardResponse.pieChartData(
                        pieChartData.getCategory(),
                        pieChartData.getAmount(),
                        pieChartData.getType()
                ))
                .toList();


        List<DashboardResponse.recentTransactionsData> recentTransactions = transactionRepo.findTop3ByUserOrderByDateDesc(user)
                .stream()
                .map(transaction -> new DashboardResponse.recentTransactionsData(
                        transaction.getId(),
                        transaction.getCategory().getName(),
                        transaction.getCategory().getCategoryIcon().getEmoji(),
                        transaction.getAmount(),
                        transaction.getDate(),
                        transaction.getCategory().getType(),
                        transaction.getCategory().getCategoryIcon().getIconUrl()
                ))
                .toList();



        return new DashboardResponse(totalIncome, totalExpense, balance ,pieChart, recentTransactions);

    }
}
