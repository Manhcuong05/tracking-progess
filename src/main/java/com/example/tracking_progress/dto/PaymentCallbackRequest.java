package com.example.tracking_progress.dto;

import lombok.Data;

@Data
public class PaymentCallbackRequest {
    private String orderId;
    private Boolean success;
}
