package com.centerflow.finance.expense.repository;

import com.centerflow.finance.expense.domain.Expense;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepository
        extends JpaRepository<Expense, UUID>,
        JpaSpecificationExecutor<Expense> {

    boolean existsByExternalReference(
            String externalReference
    );

    Optional<Expense> findByExpenseNumber(
            String expenseNumber
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT expense
            FROM Expense expense
            WHERE expense.id = :expenseId
            """)
    Optional<Expense> findByIdForUpdate(
            @Param("expenseId")
            UUID expenseId
    );
}