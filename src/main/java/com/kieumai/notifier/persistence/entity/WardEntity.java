package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "wards")
@Getter
@Setter
@NoArgsConstructor
public class WardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false)
    private String name;
    @Column(name = "province_name", nullable = false)
    private String provinceName;

    public WardEntity(String code, String name, String provinceName) {
        this.code = code;
        this.name = name;
        this.provinceName = provinceName;
    }
}
