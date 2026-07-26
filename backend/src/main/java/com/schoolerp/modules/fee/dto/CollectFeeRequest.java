package com.schoolerp.modules.fee.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class CollectFeeRequest {
    private UUID studentId;
    private int month;
    private int year;
    private BigDecimal concessionAmount;
    private BigDecimal lateFee;
    private String paymentMethod;
    private String paymentReference;
    private String remarks;
}
