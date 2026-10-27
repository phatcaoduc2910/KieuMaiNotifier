package com.kieumai.notifier.business.service;

import com.kieumai.notifier.business.model.Ward;
import com.kieumai.notifier.persistence.repository.WardCatalog;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class WardService {

    private static final Collator VIETNAMESE = Collator.getInstance(Locale.forLanguageTag("vi"));

    private static final Comparator<Ward> BY_PROVINCE_THEN_NAME = Comparator
            .comparing(Ward::provinceName, VIETNAMESE)
            .thenComparing(Ward::name, VIETNAMESE)
            .thenComparingLong(Ward::id);

    private final WardCatalog wardCatalog;

    public WardService(WardCatalog wardCatalog) {
        this.wardCatalog = wardCatalog;
    }

    public List<Ward> getWards() {
        return wardCatalog.findAll().stream()
                .sorted(BY_PROVINCE_THEN_NAME)
                .toList();
    }
}
