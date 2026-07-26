package com.schoolerp.modules.fee.repository;

import com.schoolerp.modules.fee.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    boolean existsByStudentIdAndCollectionMonthAndCollectionYearAndStatus(UUID studentId, int collectionMonth, int collectionYear, String status);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM PaymentTransaction p WHERE p.schoolId = :schoolId AND p.status = 'CONFIRMED' AND p.paymentDate = :date")
    BigDecimal getTotalCollectionToday(UUID schoolId, LocalDate date);

    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) FROM PaymentTransaction p WHERE p.schoolId = :schoolId AND p.status = 'CONFIRMED' AND p.collectionMonth = :month AND p.collectionYear = :year")
    BigDecimal getTotalCollectionForMonth(UUID schoolId, int month, int year);

    @Query("SELECT COUNT(DISTINCT p.studentId) FROM PaymentTransaction p WHERE p.schoolId = :schoolId AND p.status = 'CONFIRMED' AND p.collectionMonth = :month AND p.collectionYear = :year AND p.remainingBalance <= 0")
    long countStudentsPaidForMonth(UUID schoolId, int month, int year);

    @Query("SELECT COUNT(DISTINCT p.studentId) FROM PaymentTransaction p WHERE p.schoolId = :schoolId AND p.status = 'CONFIRMED' AND p.collectionMonth = :month AND p.collectionYear = :year AND p.remainingBalance > 0 AND p.studentId NOT IN (SELECT p2.studentId FROM PaymentTransaction p2 WHERE p2.schoolId = :schoolId AND p2.status = 'CONFIRMED' AND p2.collectionMonth = :month AND p2.collectionYear = :year AND p2.remainingBalance <= 0)")
    long countStudentsPartiallyPaidForMonth(UUID schoolId, int month, int year);

    Page<PaymentTransaction> findBySchoolIdAndStatusOrderByConfirmedAtDesc(UUID schoolId, String status, Pageable pageable);

    Optional<PaymentTransaction> findBySchoolIdAndReceiptNumber(UUID schoolId, String receiptNumber);
}
