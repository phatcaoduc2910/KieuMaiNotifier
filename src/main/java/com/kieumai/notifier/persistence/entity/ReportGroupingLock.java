package com.kieumai.notifier.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Database mutex shared by report writers for the same ward and incident type. */
@Entity
@Table(name = "report_grouping_locks")
public class ReportGroupingLock {
    @Id
    @Column(name = "lock_key", length = 64)
    private String key;

    protected ReportGroupingLock() {}

    public ReportGroupingLock(String key) { this.key = key; }
}
