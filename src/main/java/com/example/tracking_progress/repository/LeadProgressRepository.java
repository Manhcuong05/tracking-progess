package com.example.tracking_progress.repository;

import com.example.tracking_progress.entity.LeadProgress;
import com.example.tracking_progress.enums.MilestoneStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeadProgressRepository extends JpaRepository<LeadProgress, String> {

    LeadProgress findByLeadIdAndMilestoneCode(String leadId, String milestoneCode);

    boolean existsByLeadIdAndMilestoneCode(String leadId, String milestoneCode);

    List<LeadProgress> findByLeadIdOrderByCreatedAtAsc(String leadId);
    List<LeadProgress> findByStatus(MilestoneStatus status);

}
