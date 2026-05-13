package com.example.finance.service.transactionservice;

import com.example.finance.dto.categorydto.CategoryResponse;
import com.example.finance.dto.transactiondto.TransactionRequest;
import com.example.finance.dto.transactiondto.TransactionResponse;
import com.example.finance.entity.Category;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.CategoryRepo;
import com.example.finance.repo.TransactionRepo;
import com.example.finance.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService{
    private final TransactionRepo transactionRepo;
    private final CategoryRepo categoryRepo;
    private final UserRepo userRepo;

    @Override
    public TransactionResponse add(TransactionRequest request, String email) {
        LocalDate parsedDate = LocalDate.parse(request.getDate());

        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Email is not found"));

        Category category = categoryRepo.findById(request.getCategoryId())
                .orElseThrow(()-> new ResourceNotFoundException("Category ID is not found"));

        if(!category.getUser().getId().equals(user.getId())){
            throw new ResourceNotFoundException("User does not have this category. Please add.");
        }

        Transaction transaction = new Transaction();

        transaction.setAmount(request.getAmount());
        transaction.setNote(request.getNote() != null ? request.getNote() : null);
        transaction.setCategory(category);
        transaction.setDate(request.getDate() != null ? parsedDate : LocalDate.now());
        transaction.setUser(user);

        Transaction saved = transactionRepo.save(transaction);

        return toResponse(saved);
    }


    @Override
    public List<TransactionResponse> getByHistory(String email, CategoryName type, LocalDate startDate, LocalDate endDate) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(()-> new UsernameNotFoundException("Email is not found"));

        if(startDate.isAfter(endDate)){
            throw new IllegalArgumentException("Start date must be before end date");
        }

        List<TransactionResponse> trans = transactionRepo.findTransactionsHistory(user, type, startDate, endDate)
                .stream()
                .sorted((t1,t2) -> t2.getDate().compareTo(t1.getDate()))
                .map(this::toResponse)
                .toList();

        return trans;
    }

    private TransactionResponse toResponse(Transaction transaction){
        CategoryResponse.CategoryData categoryResponseData = new CategoryResponse.CategoryData();

        categoryResponseData.setId(transaction.getCategory().getId());
        categoryResponseData.setName(transaction.getCategory().getName());
        categoryResponseData.setType(transaction.getCategory().getType());
        categoryResponseData.setIcon(String.valueOf(transaction.getCategory().getCategoryIcon().getEmoji()));
        categoryResponseData.setIconUrl(String.valueOf(transaction.getCategory().getCategoryIcon().getIconUrl()));


        return new TransactionResponse(transaction.getId(),
                transaction.getAmount(),
                transaction.getNote(),
                categoryResponseData,
                transaction.getDate());
    }
}
