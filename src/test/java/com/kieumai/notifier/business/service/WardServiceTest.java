package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.Ward;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WardServiceTest {

    private FakeWardCatalog wardCatalog;
    private WardService service;

    @BeforeEach
    void setUp() {
        wardCatalog = new FakeWardCatalog();
        service = new WardService(wardCatalog);
    }

    @Test
    void getWards_isEmptyWhenNoWardExists() {
        assertTrue(service.getWards().isEmpty());
    }

    @Test
    void getWards_sortsByProvinceThenName() {
        wardCatalog.add(1L, "Yên Hòa", "Hà Nội");
        wardCatalog.add(2L, "Cầu Giấy", "Hà Nội");
        wardCatalog.add(3L, "Bến Nghé", "Đà Nẵng");

        List<Ward> wards = service.getWards();

        assertEquals(List.of(3L, 2L, 1L), wards.stream().map(Ward::id).toList());
    }
}
