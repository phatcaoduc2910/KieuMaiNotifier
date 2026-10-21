package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

}
