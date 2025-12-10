package com.example.tracking_progress.config;

import com.example.tracking_progress.entity.MilestoneConfig;
import com.example.tracking_progress.enums.MilestoneType;
import com.example.tracking_progress.repository.MilestoneConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MilestoneConfigSeeder implements CommandLineRunner {

    private final MilestoneConfigRepository repo;

    @Override
    public void run(String... args) {

        if (repo.count() > 0) return;

        repo.save(MilestoneConfig.builder()
                .code("STEP_CONSULT")
                .name("Tư vấn & Chốt gói")
                .type(MilestoneType.CORE)
                .sequenceOrder(1)
                .paymentRequired(false)
                .requiredProof(false)
                .slaHours(24)
                .minPackageLevel(1)
                .build());

        repo.save(MilestoneConfig.builder()
                .code("STEP_DKDN")
                .name("Đăng ký kinh doanh")
                .type(MilestoneType.CORE)
                .sequenceOrder(2)
                .paymentRequired(true)
                .requiredProof(true)
                .slaHours(48)
                .minPackageLevel(1)
                .build());

        repo.save(MilestoneConfig.builder()
                .code("STEP_MST")
                .name("Mã số thuế")
                .type(MilestoneType.CORE)
                .sequenceOrder(3)
                .paymentRequired(false)
                .requiredProof(true)
                .slaHours(48)
                .minPackageLevel(1)
                .build());

        repo.save(MilestoneConfig.builder()
                .code("STEP_HDDT")
                .name("Hóa đơn điện tử")
                .type(MilestoneType.CORE)
                .sequenceOrder(4)
                .paymentRequired(false)
                .requiredProof(true)
                .slaHours(48)
                .minPackageLevel(1)
                .build());
        
                // chỉ áp dụng cho Gói 2
        repo.save(MilestoneConfig.builder()
                .code("STEP_TAX_SVC")
                .name("Dịch vụ thuế")
                .type(MilestoneType.CORE)
                .sequenceOrder(5)
                .paymentRequired(false)
                .requiredProof(false)
                .slaHours(72)
                .minPackageLevel(2) 
                .build());

        repo.save(MilestoneConfig.builder()
                .code("ADDON_ZALO")
                .name("Zalo OA Business")
                .type(MilestoneType.ADDON)
                .sequenceOrder(null)
                .paymentRequired(false)
                .requiredProof(false)
                .slaHours(72)
                .minPackageLevel(1)
                .build());

        repo.save(MilestoneConfig.builder()
                .code("ADDON_WEB")
                .name("Khởi tạo Website")
                .type(MilestoneType.ADDON)
                .sequenceOrder(null)
                .paymentRequired(false)
                .requiredProof(false)
                .slaHours(120)
                .minPackageLevel(1)
                .build());
    }
}
