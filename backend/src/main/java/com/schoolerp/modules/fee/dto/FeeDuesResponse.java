package com.schoolerp.modules.fee.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class FeeDuesResponse {
    private BigDecimal subtotal;
    private BigDecimal expectedLateFee;
    private BigDecimal expectedConcession;
    private List<LineItem> lineItems;
    private boolean alreadyPaid;
    private UUID paidTransactionId;
    private String paidReceiptNumber;

    @Data
    @Builder
    public static class LineItem {
        private UUID feeCategoryId;
        private String feeCategoryName;
        private BigDecimal amount;
    }
}
