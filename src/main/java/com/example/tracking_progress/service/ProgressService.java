package com.example.tracking_progress.service;

import com.example.tracking_progress.entity.LeadProgress;
import com.example.tracking_progress.entity.MilestoneConfig;
import com.example.tracking_progress.enums.MilestoneStatus;
import com.example.tracking_progress.enums.MilestoneType;
import com.example.tracking_progress.repository.LeadProgressRepository;
import com.example.tracking_progress.repository.MilestoneConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final MilestoneConfigRepository configRepo;
    private final LeadProgressRepository progressRepo;

    // =============================================================
    // FR 4.1 + FR 4.2 - Confirm Package & Generate Flow
    // =============================================================
    @Transactional
    public Map<String, Object> confirmPackage(
            String leadId,
            String packageCode,
            Iterable<String> addons,
            boolean isPaid
    ) {

        int level = packageCode.equalsIgnoreCase("GOI_2") ? 2 : 1;

        // ================================
        // 1) Tạo STEP_CONSULT nếu chưa có
        // ================================
        LeadProgress consult = progressRepo.findByLeadIdAndMilestoneCode(leadId, "STEP_CONSULT");

        if (consult == null) {
            consult = LeadProgress.builder()
                    .leadId(leadId)
                    .milestoneCode("STEP_CONSULT")
                    .status(MilestoneStatus.IN_PROGRESS)
                    .startedAt(LocalDateTime.now())
                    .build();
            progressRepo.save(consult);
        }

        // đánh dấu tư vấn xong
        consult.setStatus(MilestoneStatus.COMPLETED);
        consult.setCompletedAt(LocalDateTime.now());
        progressRepo.save(consult);

        // ================================
        // 2) Load toàn bộ config steps
        // ================================
        List<MilestoneConfig> all = configRepo.findAll();

        List<MilestoneConfig> coreSteps = all.stream()
                .filter(c -> c.getType() == MilestoneType.CORE)
                .filter(c -> c.getMinPackageLevel() <= level)
                .filter(c -> !c.getCode().equals("STEP_CONSULT"))
                .sorted(Comparator.comparingInt(MilestoneConfig::getSequenceOrder))
                .toList();

        List<LeadProgress> created = new ArrayList<>();

        // ================================
        // 3) Tạo core flow
        // ================================
        for (MilestoneConfig cfg : coreSteps) {

            MilestoneStatus status;

            if (cfg.getSequenceOrder() == 2) {
                if (cfg.getPaymentRequired() && !isPaid) {
                    status = MilestoneStatus.WAITING_PAYMENT;
                } else {
                    status = MilestoneStatus.IN_PROGRESS;
                }
            } else {
                status = MilestoneStatus.LOCKED;
            }

            LeadProgress lp = LeadProgress.builder()
                    .leadId(leadId)
                    .milestoneCode(cfg.getCode())
                    .status(status)
                    .startedAt(status == MilestoneStatus.IN_PROGRESS ? LocalDateTime.now() : null)
                    .build();

            progressRepo.save(lp);
            created.add(lp);
        }

        // ================================
        // 4) Tạo ADDON flow
        // ================================
        if (addons != null) {
            for (String addon : addons) {

                String code = "ADDON_" + addon.toUpperCase();

                MilestoneConfig cfg = configRepo.findByCode(code);
                if (cfg == null)
                    throw new RuntimeException("Không tìm thấy addon " + code);

                LeadProgress lp = LeadProgress.builder()
                        .leadId(leadId)
                        .milestoneCode(code)
                        .status(MilestoneStatus.IN_PROGRESS)
                        .startedAt(LocalDateTime.now())
                        .build();

                progressRepo.save(lp);
                created.add(lp);
            }
        }

        return Map.of(
                "lead_id", leadId,
                "steps_created", created
        );
    }

    // =============================================================
    // Update progress (Start / Complete / Fail)
    // =============================================================
    @Transactional
    public Object updateProgress(
            String leadId,
            String milestoneCode,
            String action,
            String proofDocId,
            String note
    ) {

        LeadProgress lp = progressRepo.findByLeadIdAndMilestoneCode(leadId, milestoneCode);
        if (lp == null) throw new RuntimeException("Không tìm thấy milestone!");

        MilestoneConfig cfg = configRepo.findByCode(milestoneCode);
        if (cfg == null) throw new RuntimeException("Milestone config không tồn tại!");

        switch (action.toUpperCase()) {

            case "START" -> {
                if (cfg.getType() == MilestoneType.CORE && !canStartCoreStep(leadId, cfg)) {
                    throw new RuntimeException("Không thể START vì step trước chưa COMPLETE.");
                }
                lp.setStatus(MilestoneStatus.IN_PROGRESS);
                lp.setStartedAt(LocalDateTime.now());
            }

            case "COMPLETE" -> {
                if (cfg.getRequiredProof() &&
                        (proofDocId == null || proofDocId.isBlank())) {
                    throw new RuntimeException("Bước này cần upload proof.");
                }
                lp.setStatus(MilestoneStatus.COMPLETED);
                lp.setCompletedAt(LocalDateTime.now());
                lp.setProofDocId(proofDocId);
                lp.setNote(note);
                unlockNextCoreStep(leadId, cfg);
            }

            case "FAIL" -> {
                lp.setStatus(MilestoneStatus.FAILED);
                lp.setCompletedAt(LocalDateTime.now());
            }

            default -> throw new RuntimeException("Action không hợp lệ!");
        }

        progressRepo.save(lp);
        return lp;
    }

    // =============================================================
    // Check previous CORE step is completed
    // =============================================================
    private boolean canStartCoreStep(String leadId, MilestoneConfig cfg) {

        if (cfg.getSequenceOrder() == 1)
            return true;

        int prev = cfg.getSequenceOrder() - 1;

        MilestoneConfig prevCfg = configRepo.findCoreBySequence(prev);
        if (prevCfg == null) return false;

        LeadProgress prevStep =
                progressRepo.findByLeadIdAndMilestoneCode(leadId, prevCfg.getCode());

        return prevStep != null &&
                prevStep.getStatus() == MilestoneStatus.COMPLETED;
    }

    // =============================================================
    // Unlock next CORE step
    // =============================================================
    private void unlockNextCoreStep(String leadId, MilestoneConfig cfg) {

        int next = cfg.getSequenceOrder() + 1;

        MilestoneConfig nextCfg = configRepo.findCoreBySequence(next);
        if (nextCfg == null) return;

        LeadProgress lp = progressRepo.findByLeadIdAndMilestoneCode(leadId, nextCfg.getCode());
        if (lp == null) return;

        if (lp.getStatus() == MilestoneStatus.LOCKED) {
            lp.setStatus(MilestoneStatus.IN_PROGRESS);
            lp.setStartedAt(LocalDateTime.now());
            progressRepo.save(lp);
        }
    }
}
