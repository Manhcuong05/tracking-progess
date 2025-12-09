package com.example.tracking_progress.dto;

import com.example.tracking_progress.enums.MilestoneStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LeadMilestoneDto {
    private String milestoneCode;
    private String milestoneName;
    private String milestoneType;
    private MilestoneStatus status;
    private Integer sequenceOrder;
    private Boolean requiredProof;
    private Boolean paymentRequired;
    private Integer slaHours;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String proofDocId;
    private String note;
}
