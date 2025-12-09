package com.example.tracking_progress.service;

import com.example.tracking_progress.dto.PaymentCallbackRequest;
import com.example.tracking_progress.entity.LeadProgress;
import com.example.tracking_progress.entity.Order;
import com.example.tracking_progress.enums.MilestoneStatus;
import com.example.tracking_progress.enums.PaymentStatus;
import com.example.tracking_progress.repository.LeadProgressRepository;
import com.example.tracking_progress.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepo;
    private final LeadProgressRepository progressRepo;

    public Order pay(String orderId) {
        return handlePayment(orderId, true);
    }

    public Order callback(PaymentCallbackRequest request) {
        boolean success = request != null && Boolean.TRUE.equals(request.getSuccess());
        return handlePayment(request.getOrderId(), success);
    }

    private Order handlePayment(String orderId, boolean success) {
        Order order = orderRepo.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        if (!success) return order;

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            orderRepo.save(order);
        }

        LeadProgress lp = progressRepo.findByLeadIdAndMilestoneCode(order.getLeadId(), "STEP_DKDN");
        if (lp != null && lp.getStatus() == MilestoneStatus.WAITING_PAYMENT) {
            lp.setStatus(MilestoneStatus.IN_PROGRESS);
            lp.setStartedAt(LocalDateTime.now());
            progressRepo.save(lp);
        }

        return order;
    }
}
