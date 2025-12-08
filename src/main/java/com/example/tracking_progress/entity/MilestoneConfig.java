package com.example.tracking_progress.entity;

import com.example.tracking_progress.enums.MilestoneType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "milestone_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MilestoneConfig {

    public static final MilestoneType CORE = null;

    public static final MilestoneType ADDON = null;

    @Id
    @Column(length = 50)
    private String code;    // STEP_CONSULT, STEP_DKDN, ...

    private String name;

    @Enumerated(EnumType.STRING)
    private MilestoneType type;  // CORE / ADDON

    @Column(name = "min_package_level")
    private Integer minPackageLevel; // 1 or 2

    @Column(name = "sequence_order")
    private Integer sequenceOrder;   // null nếu ADDON

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(name = "required_proof")
    private Boolean requiredProof;

    @Column(name = "payment_required")
    private Boolean paymentRequired;

    // Audit
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
