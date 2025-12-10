package com.example.tracking_progress.dto;

import lombok.Data;

@Data
public class UpdateProgressRequest {
    private String action;       // START | COMPLETE | FAIL
    private String proofDocId;   // nếu step required_proof = true
    private String note;         // ghi chú 
}
