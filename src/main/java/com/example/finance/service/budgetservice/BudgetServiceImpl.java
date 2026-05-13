package com.example.finance.service.budgetservice;

import com.example.finance.dto.budgetdto.BudgetRequest;
import com.example.finance.dto.budgetdto.BudgetResponse;
import com.example.finance.dto.budgetdto.UpdateAmountRequest;
import com.example.finance.entity.Budget;
import com.example.finance.entity.Category;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import com.example.finance.exception.BusinessConflictException;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.BudgetRepo;
import com.example.finance.repo.CategoryRepo;
import com.example.finance.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetServiceImpl implements BudgetService{
    private final BudgetRepo budgetRepo;
    private final UserRepo userRepo;
    private final CategoryRepo categoryRepo;

    @Override
    public BudgetResponse create(BudgetRequest request, String email) {
        User user = getUser(email);

        Category category = categoryRepo.findByIdAndUser(request.getCategoryId(), user)
                .orElseThrow(()-> new ResourceNotFoundException("Category ID is not exists"));

        if(!CategoryName.EXPENSE.equals(category.getType())){
            throw new BusinessConflictException("Category type must be EXPENSE");
        }

        Budget budget = budgetRepo.findByCategoryIdAndMonthAndYearAndUser(
                        category.getId(), request.getMonth(), request.getYear(), user)
                .orElseGet(() -> {
                    Budget newBudget = new Budget();
                    newBudget.setCategory(category);
                    newBudget.setUser(user);
                    newBudget.setMonth(request.getMonth());
                    newBudget.setYear(request.getYear());
                    return newBudget;
                });

        budget.setAmount(request.getAmount());

        Budget saved = budgetRepo.save(budget);

        return toResponse(saved);
    }

    @Override
    public BudgetResponse update(Long id, UpdateAmountRequest request, String email) {
        User user = getUser(email);

        Budget budget = budgetRepo.findByIdAndUser(id, user)
                .orElseThrow(()-> new ResourceNotFoundException("Budget not found"));

        budget.setAmount(request.getAmount());

        Budget saved = budgetRepo.save(budget);

        return toResponse(saved);
    }

    @Override
    public List<BudgetResponse> getAll(Integer month, Integer year, String email) {
        User user = getUser(email);

        List<BudgetResponse> responses = budgetRepo.findByMonthAndYearAndUser(month, year, user)
                .stream()
                .map(this::toResponse)
                .toList();

        return responses;
    }

    @Override
    public void delete(Long id, String email) {
        User user = getUser(email);

        Budget budget = budgetRepo.findByIdAndUser(id, user)
                        .orElseThrow(()-> new ResourceNotFoundException("Budget not found"));

        budgetRepo.delete(budget);
    }

    private BudgetResponse toResponse(Budget budget){
        return new BudgetResponse(budget.getId(),
                new BudgetResponse.CategoryData(
                        budget.getCategory().getId(),
                        budget.getCategory().getName(),
                        budget.getCategory().getCategoryIcon().getEmoji(),
                        budget.getCategory().getCategoryIcon().getIconUrl()),
                budget.getAmount(),
                budget.getMonth(),
                budget.getYear()
        );
    }

    private UpdateAmountRequest toAmount(BudgetRequest request){
        return new UpdateAmountRequest(request.getAmount());
    }

    private User getUser(String email){
        return userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Email is not exist"));
    }

}
