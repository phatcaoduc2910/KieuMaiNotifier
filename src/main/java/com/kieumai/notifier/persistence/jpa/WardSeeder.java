package com.kieumai.notifier.persistence.jpa;

import com.kieumai.notifier.persistence.entity.WardEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WardSeeder implements ApplicationRunner {

    private static final String PROVINCE = "Hà Nội";

    private final WardJpaRepository wardJpaRepository;

    public WardSeeder(WardJpaRepository wardJpaRepository) {
        this.wardJpaRepository = wardJpaRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (wardJpaRepository.count() > 0) {
            return;
        }
        wardJpaRepository.saveAll(List.of(
                new WardEntity("HN-CAUGIAY", "Cầu Giấy", PROVINCE),
                new WardEntity("HN-NGHIADO", "Nghĩa Đô", PROVINCE),
                new WardEntity("HN-YENHOA", "Yên Hòa", PROVINCE),
                new WardEntity("HN-LANG", "Láng", PROVINCE),
                new WardEntity("HN-HOANKIEM", "Hoàn Kiếm", PROVINCE)));
    }
}
