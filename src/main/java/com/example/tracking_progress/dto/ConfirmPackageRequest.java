package com.example.tracking_progress.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConfirmPackageRequest {
    private String packageCode;
    private List<String> addons;
    private Boolean isPaid;
}
