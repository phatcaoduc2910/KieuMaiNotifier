package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "wards")
public class WardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false)
    private String name;

    @Column(name = "province_name", nullable = false)
    private String provinceName;

    protected WardEntity() {
    }

    public WardEntity(String code, String name, String provinceName) {
        this.code = code;
        this.name = name;
        this.provinceName = provinceName;
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getProvinceName() {
        return provinceName;
    }
}
