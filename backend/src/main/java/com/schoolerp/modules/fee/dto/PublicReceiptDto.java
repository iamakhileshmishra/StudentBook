package com.schoolerp.modules.fee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A receipt as shown to a parent through the public link. Contains only what is printed on the receipt. */
public record PublicReceiptDto(
        School school,
        String receiptNumber,
        String status,
        int collectionMonth,
        int collectionYear,
        LocalDate paymentDate,
        String paymentMethod,
        Student student,
        List<LineItem> lineItems,
        BigDecimal lateFee,
        BigDecimal concession,
        BigDecimal grandTotal) {

    public record School(String name, String addressLine1, String addressLine2, String city, String state,
                         String pincode, String phone, String email, String logoUrl) {}

    public record Student(String firstName, String lastName, String admissionNumber, String fatherName,
                          String className, String sectionName) {}

    public record LineItem(String name, BigDecimal amount) {}
}
