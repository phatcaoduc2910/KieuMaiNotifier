package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.IncidentConfirmationEntity;
import com.kieumai.notifier.persistence.entity.IncidentConfirmationId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentConfirmationRepository
        extends JpaRepository<IncidentConfirmationEntity, IncidentConfirmationId> {
    // Đếm tổng số lượt confirm của 1 sự cố (Để check xem đã đủ >= 3 lượt chưa)
    long countByIdIncidentId(Long incidentId);

    // Xóa lượt confirm của 1 User trên 1 sự cố (Phục vụ API Hủy confirm)
    void deleteByIdIncidentIdAndUserId(Long incidentId, Long userId);

    // Kiểm tra xem User đã confirm sự cố này chưa (Dùng cho cả API Confirm và isConfirmedByMe)
    boolean existsByIdIncidentIdAndIdUserId(Long incidentId, Long userId);
}
