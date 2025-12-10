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

    @Id
    @Column(length = 50)
    private String code; 

    private String name;

    @Enumerated(EnumType.STRING)
    private MilestoneType type;

    @Column(name = "min_package_level")
    private Integer minPackageLevel;

    @Column(name = "sequence_order")
    private Integer sequenceOrder; 

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Column(name = "required_proof")
    private Boolean requiredProof;

    @Column(name = "payment_required")
    private Boolean paymentRequired;

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
