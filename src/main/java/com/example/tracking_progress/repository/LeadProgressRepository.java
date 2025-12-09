package com.example.tracking_progress.repository;

import com.example.tracking_progress.entity.LeadProgress;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadProgressRepository extends JpaRepository<LeadProgress, Long> {

    LeadProgress findByLeadIdAndMilestoneCode(String leadId, String milestoneCode);
}
