package com.schoolerp.modules.fee;

import com.schoolerp.modules.fee.dto.CollectFeeRequest;
import com.schoolerp.modules.fee.dto.FeeDuesResponse;
import com.schoolerp.modules.fee.service.FeeCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/fees/collection")
@RequiredArgsConstructor
public class FeeCollectionController {

    private final FeeCollectionService feeCollectionService;

    @GetMapping("/dues")
    public ResponseEntity<FeeDuesResponse> getDues(
            @RequestParam UUID studentId,
            @RequestParam int month,
            @RequestParam int year) {
        
        FeeDuesResponse response = feeCollectionService.getDues(studentId, month, year);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> collectFee(@RequestBody CollectFeeRequest request) {
        UUID collectedByUserId = getCurrentUserId();
        if (collectedByUserId == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }
        
        try {
            var transaction = feeCollectionService.collectFee(request, collectedByUserId);
            return ResponseEntity.ok(transaction);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            try { return UUID.fromString(auth.getName()); } catch (Exception ignored) {}
        }
        return null;
    }
}
