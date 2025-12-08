package com.example.tracking_progress.repository;

import com.example.tracking_progress.entity.LeadProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LeadProgressRepository extends JpaRepository<LeadProgress, String> {

    List<LeadProgress> findByLeadId(String leadId);

    LeadProgress findByLeadIdAndMilestoneCode(String leadId, String milestoneCode);
}
