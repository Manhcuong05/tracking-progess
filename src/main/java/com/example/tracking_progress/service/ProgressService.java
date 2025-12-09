package com.example.tracking_progress.service;

import com.example.tracking_progress.dto.LeadMilestoneDto;
import com.example.tracking_progress.entity.LeadProgress;
import com.example.tracking_progress.entity.MilestoneConfig;
import com.example.tracking_progress.entity.Order;
import com.example.tracking_progress.enums.MilestoneStatus;
import com.example.tracking_progress.enums.MilestoneType;
import com.example.tracking_progress.enums.PaymentStatus;
import com.example.tracking_progress.repository.LeadProgressRepository;
import com.example.tracking_progress.repository.MilestoneConfigRepository;
import com.example.tracking_progress.repository.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final MilestoneConfigRepository configRepo;
    private final LeadProgressRepository progressRepo;
    private final OrderRepository orderRepo;

    // =============================================================
    // FR 4.1 - Tạo STEP_CONSULT khi lead mới được tạo
    // =============================================================
    @Transactional
    public LeadProgress onLeadCreated(String leadId) {

        LeadProgress exist = progressRepo.findByLeadIdAndMilestoneCode(leadId, "STEP_CONSULT");
        if (exist != null) return exist;

        LeadProgress lp = LeadProgress.builder()
                .leadId(leadId)
                .milestoneCode("STEP_CONSULT")
                .status(MilestoneStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        progressRepo.save(lp);
        log.info("✔ Created STEP_CONSULT for lead {}", leadId);
        return lp;
    }

    // =============================================================
    // FR 4.2 - Confirm Package & Generate Steps
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
            consult = onLeadCreated(leadId);
        }

        // Đánh dấu STEP_CONSULT hoàn thành
        consult.setStatus(MilestoneStatus.COMPLETED);
        consult.setCompletedAt(LocalDateTime.now());
        progressRepo.save(consult);

        // 1b) Tạo ORDER nếu chưa có
        Order order = orderRepo.findByLeadId(leadId);
        if (order == null) {
            order = Order.builder()
                    .leadId(leadId)
                    .packageCode(packageCode)
                    .amount((int) (level == 2 ? 1_999_000L : 999_000L))
                    .paymentStatus(isPaid ? PaymentStatus.PAID : PaymentStatus.PENDING)
                    .paidAt(isPaid ? LocalDateTime.now() : null)
                    .build();
            orderRepo.save(order);
        }

        // 2) Load toàn bộ configs
        List<MilestoneConfig> allConfigs = configRepo.findAll();

        List<MilestoneConfig> coreSteps = allConfigs.stream()
                .filter(c -> c.getType() == MilestoneType.CORE)
                .filter(c -> c.getMinPackageLevel() <= level)
                .filter(c -> !c.getCode().equals("STEP_CONSULT"))
                .sorted(Comparator.comparingInt(MilestoneConfig::getSequenceOrder))
                .toList();

        List<LeadProgress> created = new ArrayList<>();

        // 3) Tạo CORE flow
        for (MilestoneConfig cfg : coreSteps) {

            // Check nếu milestone đã tồn tại → bỏ qua (fix duplicate)
            LeadProgress exist = progressRepo.findByLeadIdAndMilestoneCode(leadId, cfg.getCode());
            if (exist != null) continue;

            MilestoneStatus status;

            // Bước 2 — check payment
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

        // 4) Tạo ADDON
        if (addons != null) {
            for (String addon : addons) {
                String code = "ADDON_" + addon.toUpperCase();

                MilestoneConfig cfg = configRepo.findByCode(code);
                if (cfg == null) throw new RuntimeException("Không tìm thấy addon " + code);

                // check tồn tại
                LeadProgress exist = progressRepo.findByLeadIdAndMilestoneCode(leadId, code);
                if (exist != null) continue;

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
    // FR 4.3 + 4.4 + 4.5 + 4.6 - Start / Complete / Fail
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
                if (cfg.getType() == MilestoneType.CORE && !canStartCoreStep(leadId, cfg)) {
                    throw new RuntimeException("Không thể START vì step trước chưa COMPLETE.");
                }
                lp.setStatus(MilestoneStatus.IN_PROGRESS);
                lp.setStartedAt(LocalDateTime.now());
            }

            case "COMPLETE" -> {
                // Check proof
                if (cfg.getRequiredProof() && (proofDocId == null || proofDocId.isBlank())) {
                    throw new RuntimeException("Bước này cần upload proof.");
                }

                lp.setStatus(MilestoneStatus.COMPLETED);
                lp.setCompletedAt(LocalDateTime.now());
                lp.setProofDocId(proofDocId);
                lp.setNote(note);

                // Unlock next CORE step
                if (cfg.getType() == MilestoneType.CORE) {
                    unlockNextCoreStep(leadId, cfg);
                }
            }

            case "FAIL" -> {
                lp.setStatus(MilestoneStatus.FAILED);
                lp.setCompletedAt(LocalDateTime.now());
            }

            default -> throw new RuntimeException("Action không hợp lệ: " + action);
        }

        progressRepo.save(lp);
        return lp;
    }

    // =============================================================
    // CORE logic: check step trước đã complete
    // =============================================================
    private boolean canStartCoreStep(String leadId, MilestoneConfig cfg) {

        if (cfg.getSequenceOrder() == 1)
            return true;

        int prevSeq = cfg.getSequenceOrder() - 1;

        MilestoneConfig prevCfg = configRepo.findCoreBySequence(prevSeq);
        if (prevCfg == null) return false;

        LeadProgress prev = progressRepo.findByLeadIdAndMilestoneCode(leadId, prevCfg.getCode());

        return prev != null && prev.getStatus() == MilestoneStatus.COMPLETED;
    }

    // =============================================================
    // Mở khoá step tiếp theo (CORE)
    // =============================================================
    private void unlockNextCoreStep(String leadId, MilestoneConfig cfg) {

        int nextSeq = cfg.getSequenceOrder() + 1;

        MilestoneConfig nextCfg = configRepo.findCoreBySequence(nextSeq);
        if (nextCfg == null) return;

        LeadProgress nextStep =
                progressRepo.findByLeadIdAndMilestoneCode(leadId, nextCfg.getCode());

        if (nextStep == null) return;

        if (nextStep.getStatus() == MilestoneStatus.LOCKED
                || nextStep.getStatus() == MilestoneStatus.WAITING_PAYMENT) {

            nextStep.setStatus(MilestoneStatus.IN_PROGRESS);
            nextStep.setStartedAt(LocalDateTime.now());

            progressRepo.save(nextStep);
        }
    }

    // =============================================================
    // API lấy toàn bộ milestones của lead
    // =============================================================
    public List<LeadMilestoneDto> getLeadMilestones(String leadId) {

        var configs = configRepo.findAll();
        var configMap = configs.stream()
                .collect(Collectors.toMap(MilestoneConfig::getCode, c -> c));

        return progressRepo.findByLeadIdOrderByCreatedAtAsc(leadId).stream()
                .map(lp -> {
                    MilestoneConfig cfg = configMap.get(lp.getMilestoneCode());

                    return LeadMilestoneDto.builder()
                            .milestoneCode(lp.getMilestoneCode())
                            .milestoneName(cfg != null ? cfg.getName() : null)
                            .milestoneType(cfg != null ? cfg.getType().name() : null)
                            .status(lp.getStatus())
                            .sequenceOrder(cfg != null ? cfg.getSequenceOrder() : null)
                            .slaHours(cfg != null ? cfg.getSlaHours() : null)
                            .requiredProof(cfg != null ? cfg.getRequiredProof() : null)
                            .paymentRequired(cfg != null ? cfg.getPaymentRequired() : null)
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
