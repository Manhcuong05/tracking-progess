package com.example.tracking_progress.repository;

import com.example.tracking_progress.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
    Order findByLeadId(String leadId);
}
