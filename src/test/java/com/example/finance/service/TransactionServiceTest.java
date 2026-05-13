package com.example.finance.service;

import com.example.finance.dto.transactiondto.TransactionRequest;
import com.example.finance.dto.transactiondto.TransactionResponse;
import com.example.finance.entity.Category;
import com.example.finance.entity.CategoryIcon;
import com.example.finance.entity.Transaction;
import com.example.finance.entity.User;
import com.example.finance.enums.CategoryName;
import com.example.finance.exception.ResourceNotFoundException;
import com.example.finance.repo.CategoryRepo;
import com.example.finance.repo.TransactionRepo;
import com.example.finance.repo.UserRepo;
import com.example.finance.service.transactionservice.TransactionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
    @Nested
    @DisplayName("TransactionService – Business Logic")
    class TransactionServiceTest {

        @Mock
        CategoryRepo categoryRepository;
        @Mock
        TransactionRepo transactionRepository;
        @Mock
        UserRepo userRepo;

        @InjectMocks
        TransactionServiceImpl transactionService;

        private User user;
        private Category expenseCategory;

        @BeforeEach
        void setUp() {
            user = new User();
            user.setId(1L);

            CategoryIcon icon = new CategoryIcon();
            icon.setEmoji("🏠");
            icon.setIconUrl("https://cdn.example.com/housing.png");

            expenseCategory = new Category();
            expenseCategory.setId(3L);
            expenseCategory.setName("Housing");
            expenseCategory.setType(CategoryName.EXPENSE);
            expenseCategory.setCategoryIcon(icon);

            expenseCategory.setUser(user);
        }

        /**
         * TC-TXN-CREATE-01 – Tạo transaction thành công, trả về đủ fields
         */
        @Test
        @DisplayName("TC-TXN-CREATE-01: Tạo transaction thành công trả về đủ id, amount, category, date")
        void createTransaction_valid_returnsFullResponse() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(categoryRepository.findById(3L)).thenReturn(Optional.of(expenseCategory));

            when(transactionRepository.save(any())).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setId(101L);
                return t;
            });

            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(400.00), "Paid monthly rent", 3L, "2024-04-28");
            TransactionResponse res = transactionService.add(req, user.getEmail());

            assertThat(res.getId()).isEqualTo(101L);
            assertThat(res.getAmount()).isEqualTo(BigDecimal.valueOf(400.00));
            assertThat(res.getDate()).isEqualTo("2024-04-28");
            assertThat(res.getCategory().getId()).isEqualTo(3L);
            assertThat(res.getCategory().getName()).isEqualTo("Housing");
            assertThat(res.getCategory().getIcon()).isEqualTo("🏠");
            assertThat(res.getCategory().getIconUrl()).isEqualTo("https://cdn.example.com/housing.png");
        }

        /**
         * TC-TXN-CREATE-02 – type được suy ra từ category, KHÔNG cần client truyền
         */
        @Test
        @DisplayName("TC-TXN-CREATE-02: type EXPENSE được suy ra từ category, không cần client truyền")
        void createTransaction_typeDerivedFromCategory() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(categoryRepository.findById(3L)).thenReturn(Optional.of(expenseCategory));

            when(transactionRepository.save(any())).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(400.00), null, 3L, "2024-04-28");
            TransactionResponse res = transactionService.add(req, user.getEmail());

            verify(transactionRepository).save(argThat(t ->
                    t.getCategory().getType().equals(CategoryName.EXPENSE)
            ));
        }

        /**
         * TC-TXN-CREATE-05 – categoryId không tồn tại → ResourceNotFoundException
         */
        @Test
        @DisplayName("TC-TXN-CREATE-05: categoryId không tồn tại ném ResourceNotFoundException")
        void createTransaction_invalidCategoryId_throwsNotFound() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(categoryRepository.findById(9999L)).thenReturn(Optional.empty());

            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(100.00), null, 9999L, "2024-04-28");

            assertThatThrownBy(() -> transactionService.add(req, user.getEmail()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        /**
         * TC-TXN-CREATE-10 – Transaction được gắn với user đang đăng nhập
         */
        @Test
        @DisplayName("TC-TXN-CREATE-10: Transaction được gắn đúng user_id")
        void createTransaction_savedWithCorrectUserId() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(categoryRepository.findById(3L)).thenReturn(Optional.of(expenseCategory));
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(100.00), null, 3L, "2024-04-28");
            transactionService.add(req, user.getEmail());

            verify(transactionRepository).save(argThat(t ->
                    t.getUser().getId().equals(1L)
            ));
        }

        /**
         * TC-TXN-CREATE-12 – note là optional, không truyền vẫn lưu được
         */
        @Test
        @DisplayName("TC-TXN-CREATE-12: note null/optional vẫn tạo transaction thành công")
        void createTransaction_withoutNote_savesSuccessfully() {
            when(userRepo.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(categoryRepository.findById(3L)).thenReturn(Optional.of(expenseCategory));
            when(transactionRepository.save(any())).thenAnswer(inv -> {
                Transaction t = inv.getArgument(0);
                t.setId(1L);
                return t;
            });

            TransactionRequest req = new TransactionRequest(BigDecimal.valueOf(100.0), null, 3L, "2024-04-28");
            TransactionResponse res = transactionService.add(req, user.getEmail());

            assertThat(res).isNotNull();
            assertThat(res.getNote()).isNull();
        }
    }