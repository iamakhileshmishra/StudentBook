package com.schoolerp.modules.fee.dto;

import com.schoolerp.modules.fee.entity.PaymentTransaction;
import com.schoolerp.modules.student.dto.StudentSummaryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FullReceiptDto {
    private PaymentTransaction receipt;
    private StudentSummaryDto student;
    private FeeDuesResponse dues;
    private BigDecimal lateFee;
    private BigDecimal concession;
    private BigDecimal grandTotal;
}
