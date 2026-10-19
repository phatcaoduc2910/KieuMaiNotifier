package com.kieumai.notifier.persistence.repository;

import com.kieumai.notifier.persistence.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User,Long> {
    User findByUserName(String userName);
}
