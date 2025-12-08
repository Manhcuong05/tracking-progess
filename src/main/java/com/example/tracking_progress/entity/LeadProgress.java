package com.example.tracking_progress.entity;

import com.example.tracking_progress.enums.MilestoneStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lead_progress",
       uniqueConstraints = @UniqueConstraint(columnNames = {"lead_id", "milestone_code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeadProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "lead_id", nullable = false)
    private String leadId;

    @Column(name = "milestone_code", nullable = false)
    private String milestoneCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MilestoneStatus status;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private String proofDocId;

    @Column(columnDefinition = "TEXT")
    private String note;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
