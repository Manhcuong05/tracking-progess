package com.example.tracking_progress.controller;

import com.example.tracking_progress.dto.PaymentCallbackRequest;
import com.example.tracking_progress.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/pay/{orderId}")
    public ResponseEntity<?> pay(@PathVariable String orderId) {
        return ResponseEntity.ok(paymentService.pay(orderId));
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody PaymentCallbackRequest request) {
        return ResponseEntity.ok(paymentService.callback(request));
    }
}
