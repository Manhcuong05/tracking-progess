package com.example.tracking_progress.repository;

import com.example.tracking_progress.entity.MilestoneConfig;
import com.example.tracking_progress.enums.MilestoneType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MilestoneConfigRepository extends JpaRepository<MilestoneConfig, String> {

    MilestoneConfig findByCode(String code);

    @Query("SELECT m FROM MilestoneConfig m WHERE m.type = 'CORE' AND m.sequenceOrder = :seq")
    MilestoneConfig findCoreBySequence(int seq);

    List<MilestoneConfig> findByTypeOrderBySequenceOrderAsc(MilestoneType type);
    
}
