package com.kieumai.notifier.persistence.jpa;

import com.kieumai.notifier.persistence.entity.WardEntity;
import com.kieumai.notifier.persistence.repository.WardRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inserts a small set of Hà Nội wards when the table is empty, so the API is usable right after startup.
 */
@Component
public class WardSeeder implements ApplicationRunner {

    private static final String PROVINCE = "Hà Nội";

    private final WardRepository wardRepository;

    public WardSeeder(WardRepository wardRepository) {
        this.wardRepository = wardRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (wardRepository.count() > 0) {
            return;
        }
        wardRepository.saveAll(List.of(
                new WardEntity("HN-CAUGIAY", "Cầu Giấy", PROVINCE),
                new WardEntity("HN-NGHIADO", "Nghĩa Đô", PROVINCE),
                new WardEntity("HN-YENHOA", "Yên Hòa", PROVINCE),
                new WardEntity("HN-LANG", "Láng", PROVINCE),
                new WardEntity("HN-HOANKIEM", "Hoàn Kiếm", PROVINCE)));
    }
}
