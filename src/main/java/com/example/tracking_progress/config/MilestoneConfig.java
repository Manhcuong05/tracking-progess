package com.example.tracking_progress.config;

import com.example.tracking_progress.enums.MilestoneType;
import jakarta.persistence.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "milestone_configs")
public class MilestoneConfig {

    @Id
    private String code;

    private String name;

    private boolean paymentRequired;
    private boolean requiredProof;

    private Integer sequenceOrder;
    private Integer slaHours;

    @Enumerated(EnumType.STRING)
    private MilestoneType type;

    private Integer minPackageLevel;
}
