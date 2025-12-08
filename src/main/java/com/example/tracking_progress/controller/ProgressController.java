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

    /**
     * Ultra xác nhận package → sinh milestone flow (core + addon)
     */
    @PostMapping("/{leadId}/confirm-package")
    public ResponseEntity<?> confirmPackage(
            @PathVariable("leadId") String leadId,
            @RequestBody Map<String, Object> body
    ) {

        String packageCode = (String) body.get("package_code");

        List<String> addons = null;
        if (body.containsKey("addons") && body.get("addons") instanceof List) {
            addons = (List<String>) body.get("addons");
        }

        boolean isPaid = body.get("is_paid") != null &&
                Boolean.TRUE.equals(body.get("is_paid"));

        return ResponseEntity.ok(
                progressService.confirmPackage(leadId, packageCode, addons, isPaid)
        );
    }

    /**
     * Update milestone: START / COMPLETE / FAIL
     */
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
}
