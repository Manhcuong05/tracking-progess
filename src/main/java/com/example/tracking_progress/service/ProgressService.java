package com.example.tracking_progress.service;

import com.example.tracking_progress.dto.LeadMilestoneDto;
import com.example.tracking_progress.entity.LeadProgress;
import com.example.tracking_progress.entity.MilestoneConfig;
import com.example.tracking_progress.enums.MilestoneStatus;
import com.example.tracking_progress.enums.MilestoneType;
import com.example.tracking_progress.enums.PaymentStatus;
import com.example.tracking_progress.entity.Order;
import com.example.tracking_progress.repository.LeadProgressRepository;
import com.example.tracking_progress.repository.MilestoneConfigRepository;
import com.example.tracking_progress.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProgressService {

    private final MilestoneConfigRepository configRepo;
    private final LeadProgressRepository progressRepo;
    private final OrderRepository orderRepo;

    // =============================================================
    // FR 4.1 – Tạo STEP_CONSULT khi tạo lead
    // =============================================================
    @Transactional
    public Map<String, Object> onLeadCreated(String leadId) {

        // không tạo trùng
        if (progressRepo.existsByLeadIdAndMilestoneCode(leadId, "STEP_CONSULT")) {
            return Map.of("message", "Lead đã có STEP_CONSULT");
        }

        LeadProgress lp = LeadProgress.builder()
                .leadId(leadId)
                .milestoneCode("STEP_CONSULT")
                .status(MilestoneStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        progressRepo.save(lp);

        return Map.of(
                "lead_id", leadId,
                "created", lp
        );
    }

    // =============================================================
    // FR 4.2 – Confirm package
    // =============================================================
    @Transactional
    public Map<String, Object> confirmPackage(
            String leadId,
            String packageCode,
            Iterable<String> addons,
            boolean isPaid
    ) {

        int level = packageCode.equalsIgnoreCase("GOI_2") ? 2 : 1;

        // 1) Đảm bảo STEP_CONSULT tồn tại
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

        // đánh dấu tư vấn hoàn thành
        consult.setStatus(MilestoneStatus.COMPLETED);
        consult.setCompletedAt(LocalDateTime.now());
        progressRepo.save(consult);

        // ============================
        // TẠO ORDER (nếu chưa có)
        // ============================
        Order order = orderRepo.findByLeadId(leadId);
        if (order == null) {
            order = Order.builder()
                    .leadId(leadId)
                    .packageCode(packageCode)
                    .amount(level == 2 ? 1_999_000L : 999_000L) // có thể lấy từ config
                    .paymentStatus(isPaid ? PaymentStatus.PAID : PaymentStatus.PENDING)
                    .paidAt(isPaid ? LocalDateTime.now() : null)
                    .build();
            orderRepo.save(order);
        }

        // 2) Load config
        List<MilestoneConfig> all = configRepo.findAll();

        List<MilestoneConfig> coreSteps = all.stream()
                .filter(c -> c.getType() == MilestoneType.CORE)
                .filter(c -> c.getMinPackageLevel() <= level)
                .filter(c -> !c.getCode().equals("STEP_CONSULT"))
                .sorted(Comparator.comparingInt(MilestoneConfig::getSequenceOrder))
                .toList();

        List<LeadProgress> created = new ArrayList<>();

        // 3) Tạo core steps
        for (MilestoneConfig cfg : coreSteps) {

            // Nếu đã có → bỏ qua (chống duplicate)
            if (progressRepo.existsByLeadIdAndMilestoneCode(leadId, cfg.getCode())) {
                continue;
            }

            MilestoneStatus status;

            if (cfg.getSequenceOrder() == 2) {
                status = cfg.getPaymentRequired() && !isPaid
                        ? MilestoneStatus.WAITING_PAYMENT
                        : MilestoneStatus.IN_PROGRESS;
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

        // 4) Tạo ADDON
        if (addons != null) {
            for (String addon : addons) {

                String code = "ADDON_" + addon.toUpperCase();

                MilestoneConfig cfg = configRepo.findByCode(code);
                if (cfg == null) throw new RuntimeException("Không tồn tại addon " + code);

                if (progressRepo.existsByLeadIdAndMilestoneCode(leadId, code))
                    continue;

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
                "order_id", order.getId(),
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
        if (cfg == null) throw new RuntimeException("Config không tồn tại!");

        switch (action.toUpperCase()) {

            case "START" -> {
                if (cfg.getType() == MilestoneType.CORE
                        && !canStartCoreStep(leadId, cfg)) {
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

                if (cfg.getType() == MilestoneType.CORE) {
                    unlockNextCoreStep(leadId, cfg);
                }
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

        MilestoneConfig prevCfg = configRepo.findCoreBySequence(cfg.getSequenceOrder() - 1);
        if (prevCfg == null) return false;

        LeadProgress prev =
                progressRepo.findByLeadIdAndMilestoneCode(leadId, prevCfg.getCode());

        return prev != null && prev.getStatus() == MilestoneStatus.COMPLETED;
    }

    // =============================================================
    // Unlock next CORE step
    // =============================================================
    private void unlockNextCoreStep(String leadId, MilestoneConfig cfg) {

        MilestoneConfig nextCfg = configRepo.findCoreBySequence(cfg.getSequenceOrder() + 1);
        if (nextCfg == null) return;

        LeadProgress lp =
                progressRepo.findByLeadIdAndMilestoneCode(leadId, nextCfg.getCode());
        if (lp == null) return;

        if (lp.getStatus() == MilestoneStatus.LOCKED) {
            lp.setStatus(MilestoneStatus.IN_PROGRESS);
            lp.setStartedAt(LocalDateTime.now());
            progressRepo.save(lp);
        }
    }

    // =============================================================
    // Get all milestones for a lead
    // =============================================================
    public List<LeadMilestoneDto> getLeadMilestones(String leadId) {

        var configs = configRepo.findAll();
        var map = configs.stream()
                .collect(Collectors.toMap(MilestoneConfig::getCode, c -> c));

        return progressRepo.findByLeadIdOrderByCreatedAtAsc(leadId).stream()
                .map(lp -> {
                    MilestoneConfig cfg = map.get(lp.getMilestoneCode());

                    return LeadMilestoneDto.builder()
                            .milestoneCode(lp.getMilestoneCode())
                            .milestoneName(cfg != null ? cfg.getName() : null)
                            .milestoneType(cfg != null ? cfg.getType().name() : null)
                            .status(lp.getStatus())
                            .sequenceOrder(cfg != null ? cfg.getSequenceOrder() : null)
                            .requiredProof(cfg != null ? cfg.getRequiredProof() : null)
                            .paymentRequired(cfg != null ? cfg.getPaymentRequired() : null)
                            .slaHours(cfg != null ? cfg.getSlaHours() : null)
                            .startedAt(lp.getStartedAt())
                            .completedAt(lp.getCompletedAt())
                            .createdAt(lp.getCreatedAt())
                            .updatedAt(lp.getUpdatedAt())
                            .proofDocId(lp.getProofDocId())
                            .note(lp.getNote())
                            .build();
                })
                .toList();
    }
}
