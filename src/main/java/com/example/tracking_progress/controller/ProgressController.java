package com.example.tracking_progress.controller;

import com.example.tracking_progress.service.ProgressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/leads")
@RequiredArgsConstructor
public class ProgressController {

    private final ProgressService progressService;
   


     // Giai đoạn 1 — Khởi tạo STEP_CONSULT cho lead mới
    @PostMapping("/{leadId}/init-progress")
    public ResponseEntity<?> initProgress(
            @PathVariable("leadId") String leadId
    ) {
        return ResponseEntity.ok(
                progressService.onLeadCreated(leadId)
        );
    }

     // Giai đoạn 2 — Xác nhận package hoặc chỉ tạo Addon
     
    @PostMapping("/{leadId}/confirm-package")
    public ResponseEntity<?> confirmPackage(
            @PathVariable("leadId") String leadId,
            @RequestBody Map<String, Object> body
    ) {

        // Lấy package_code (KHÔNG BẮT BUỘC)
        String packageCode = body.get("package_code") != null
                ? body.get("package_code").toString()
                : null;

        // Lấy addons nếu có
        List<String> addons = null;
        if (body.containsKey("addons") && body.get("addons") instanceof List<?> raw) {
            addons = raw.stream().map(Object::toString).toList();
        }

        // Lấy trạng thái thanh toán (chỉ dùng khi có package)
        boolean isPaid = body.get("is_paid") != null
                && Boolean.parseBoolean(body.get("is_paid").toString());

        // CASE A: GỬI ADDON ONLY (không có gói)
        if (packageCode == null || packageCode.isBlank()) {
            return ResponseEntity.ok(
                    progressService.confirmPackage(
                            leadId,
                            null,      
                            addons,
                            false       
                    )
            );
        }

        // CASE B: GÓI 1 / GÓI 2
        return ResponseEntity.ok(
                progressService.confirmPackage(
                        leadId,
                        packageCode,
                        addons,
                        isPaid
                )
        );
    }

    
     // Giai đoạn 2 — Update milestone: START / COMPLETE / FAIL
     
    @PostMapping("/{leadId}/progress/{milestoneCode}")
    public ResponseEntity<?> updateProgress(
            @PathVariable("leadId") String leadId,
            @PathVariable("milestoneCode") String milestoneCode,
            @RequestBody Map<String, Object> body
    ) {

        String action = (String) body.get("action");
        String proofDocId = (String) body.get("proof_doc_id");
        String note = (String) body.get("note");

        return ResponseEntity.ok(
                progressService.updateProgress(
                        leadId, milestoneCode, action, proofDocId, note
                )
        );
    }

    
     // Giai đoạn 3 — API lấy toàn bộ step đã tạo của lead
     
    @GetMapping("/{leadId}/progress")
    public ResponseEntity<?> getLeadProgress(
            @PathVariable("leadId") String leadId
    ) {
        return ResponseEntity.ok(
                progressService.getLeadMilestones(leadId)
        );
    }



}
