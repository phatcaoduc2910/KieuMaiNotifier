package com.kieumai.notifier;

import com.kieumai.notifier.business.model.Subscription;
import com.kieumai.notifier.persistence.entity.*;
import com.kieumai.notifier.persistence.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DatabaseIntegrationTests {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired UserRepository users;
    @Autowired WardRepository wards;
    @Autowired IncidentRepository incidents;
    @Autowired ReportRepository reports;
    @Autowired JpaSubscriptionRepository subscriptions;
    @Autowired SubscriptionRepository subscriptionPort;
    @Autowired ReportUpdateRepository updates;
    User user;
    WardEntity ward;
    IncidentEntity incident;
    ReportEntity report;
    SubscriptionEntity subscription;
    ReportUpdateEntity update;

    @BeforeEach
    void createDatabaseFixture() {
        updates.deleteAll();
        subscriptions.deleteAll();
        reports.deleteAll();
        incidents.deleteAll();
        users.deleteAll();
        wards.deleteAll();

        user = new User();
        user.setUsername("database-user");
        user.setPasswordHash("test-only-password-hash");
        user.setDisplayName("Người dùng thử nghiệm");
        user = users.saveAndFlush(user);

        ward = new WardEntity();
        ward.setCode("DB-TEST");
        ward.setName("Phường thử nghiệm");
        ward.setProvinceName("Tỉnh thử nghiệm");
        ward = wards.saveAndFlush(ward);

        incident = new IncidentEntity();
        incident.setWardId(ward.getId());
        incident.setWard(ward);
        incident.setType("FLOOD");
        incident.setTitle("Sự cố thử nghiệm");
        incident.setLatitude(new BigDecimal("10.7769000"));
        incident.setLongitude(new BigDecimal("106.7009000"));
        incident.setStatus("PENDING");
        incident.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusHours(4));
        incident = incidents.saveAndFlush(incident);

        report = new ReportEntity();
        report.setUser(user);
        report.setIncident(incident);
        report.setDescription("Phản ánh thử nghiệm");
        report.setLatitude(incident.getLatitude());
        report.setLongitude(incident.getLongitude());
        report.setObservedAt(OffsetDateTime.parse("2026-10-27T10:00:00.123456+07:00"));
        report = reports.saveAndFlush(report);

        subscription = new SubscriptionEntity();
        subscription.setUser(user);
        subscription.setWard(ward);
        subscription = subscriptions.saveAndFlush(subscription);

        update = new ReportUpdateEntity();
        update.setUser(user);
        update.setReport(report);
        update.setContent("Diễn biến thử nghiệm");
        update = updates.saveAndFlush(update);
    }

    @Test
    void flywayCreatesExactlySixBusinessTablesAndHibernateMapsSixEntities() {
        var tables = jdbc.queryForList("""
                select table_name from information_schema.tables
                where lower(table_schema) = 'public' and table_type = 'BASE TABLE'
                """, String.class).stream().map(String::toLowerCase)
                .filter(name -> !name.equals("flyway_schema_history")).collect(Collectors.toSet());
        assertEquals(Set.of("users", "wards", "incidents", "reports", "subscriptions", "report_updates"), tables);
        assertEquals(6, entityManager.getMetamodel().getEntities().size());
        assertEquals(1, jdbc.queryForObject("""
                select count(*) from "flyway_schema_history" where "version" = '1' and "success" = true
                """, Integer.class));
    }

    @Test
    void everyTableHasExactlyTheColumnsInTheDiagram() {
        var expected = Map.of(
                "users", Set.of("id", "username", "password_hash", "display_name", "latitude", "longitude", "created_at"),
                "wards", Set.of("id", "code", "name", "province_name"),
                "incidents", Set.of("id", "ward_id", "type", "title", "latitude", "longitude", "status",
                        "started_at", "expires_at", "alerted_at", "created_at"),
                "reports", Set.of("id", "user_id", "incident_id", "description", "latitude", "longitude",
                        "observed_at", "created_at", "deleted_at"),
                "subscriptions", Set.of("id", "user_id", "ward_id", "created_at"),
                "report_updates", Set.of("id", "report_id", "user_id", "content", "created_at"));
        expected.forEach((table, columns) -> {
            var actual = jdbc.queryForList("""
                    select column_name from information_schema.columns
                    where lower(table_schema) = 'public' and lower(table_name) = ?
                    """, String.class, table).stream().map(String::toLowerCase).collect(Collectors.toSet());
            assertEquals(columns, actual, table);
        });
    }

    @Test
    void identitiesAndCreationTimestampsAreGeneratedForAllApplicableTables() {
        assertTrue(user.getId() > 0);
        assertTrue(ward.getId() > 0);
        assertTrue(incident.getId() > 0);
        assertTrue(report.getId() > 0);
        assertTrue(subscription.getId() > 0);
        assertTrue(update.getId() > 0);
        assertNotNull(users.findById(user.getId()).orElseThrow().getCreatedAt());
        assertNotNull(incidents.findById(incident.getId()).orElseThrow().getCreatedAt());
        assertNotNull(reports.findById(report.getId()).orElseThrow().getCreatedAt());
        assertNotNull(subscriptions.findById(subscription.getId()).orElseThrow().getCreatedAt());
        assertNotNull(updates.findById(update.getId()).orElseThrow().getCreatedAt());
    }

    @Test
    void associationsLoadCorrectlyAcrossAllSevenForeignKeys() {
        assertEquals(7, jdbc.queryForObject("""
                select count(*) from information_schema.table_constraints
                where lower(table_schema) = 'public' and constraint_type = 'FOREIGN KEY'
                """, Integer.class));
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var savedReport = reports.findById(report.getId()).orElseThrow();
            assertEquals(user.getUsername(), savedReport.getUser().getUsername());
            assertEquals(ward.getCode(), savedReport.getIncident().getWard().getCode());
            var savedSubscription = subscriptions.findById(subscription.getId()).orElseThrow();
            assertEquals(user.getUsername(), savedSubscription.getUser().getUsername());
            assertEquals(ward.getCode(), savedSubscription.getWard().getCode());
            var savedUpdate = updates.findById(update.getId()).orElseThrow();
            assertEquals(report.getDescription(), savedUpdate.getReport().getDescription());
            assertEquals(user.getUsername(), savedUpdate.getUser().getUsername());
        });
    }

    @Test
    void allForeignKeysRejectMissingParents() {
        for (String target : List.of("incidents:ward_id", "reports:user_id", "reports:incident_id",
                "subscriptions:user_id", "subscriptions:ward_id", "report_updates:report_id", "report_updates:user_id")) {
            String[] parts = target.split(":");
            assertThrows(DataIntegrityViolationException.class,
                    () -> jdbc.update("update " + parts[0] + " set " + parts[1] + " = 9223372036854775806"), target);
        }
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("delete from wards where id = ?", ward.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("delete from users where id = ?", user.getId()));
    }

    @Test
    void usernameWardCodeAndSubscriptionPairAreUnique() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into users(username, password_hash, display_name) values (?, 'hash', 'name')", user.getUsername()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into wards(code, name, province_name) values (?, 'ward', 'province')", ward.getCode()));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "insert into subscriptions(user_id, ward_id) values (?, ?)", user.getId(), ward.getId()));
    }

    @Test
    void nullableFieldsAndMandatoryIncidentReferenceMatchTheDiagram() {
        assertNull(users.findById(user.getId()).orElseThrow().getLatitude());
        assertNull(users.findById(user.getId()).orElseThrow().getLongitude());
        assertNull(incidents.findById(incident.getId()).orElseThrow().getAlertedAt());
        assertNull(reports.findById(report.getId()).orElseThrow().getDeletedAt());
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update reports set incident_id = null where id = ?", report.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update report_updates set content = null where id = ?", update.getId()));
    }

    @Test
    void userCoordinatesRoundTripWithSevenDecimalPlacesAndValidBoundaryValues() {
        user.setLatitude(new BigDecimal("10.1234567"));
        user.setLongitude(new BigDecimal("106.7654321"));
        users.saveAndFlush(user);
        var saved = users.findById(user.getId()).orElseThrow();
        assertEquals(0, saved.getLatitude().compareTo(new BigDecimal("10.1234567")));
        assertEquals(0, saved.getLongitude().compareTo(new BigDecimal("106.7654321")));
        jdbc.update("update users set latitude = 90, longitude = 180 where id = ?", user.getId());
        jdbc.update("update users set latitude = -90, longitude = -180 where id = ?", user.getId());
        jdbc.update("update users set latitude = null, longitude = null where id = ?", user.getId());
        assertNull(users.findById(user.getId()).orElseThrow().getLatitude());
    }

    @Test
    void databaseRejectsInvalidCoordinatesAndIncompleteUserLocation() {
        for (String values : List.of("91, 0", "-91, 0", "0, 181", "0, -181", "0, null", "null, 0")) {
            String[] pair = values.split(", ");
            assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                    "update users set latitude = " + pair[0] + ", longitude = " + pair[1] + " where id = ?", user.getId()));
        }
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update incidents set latitude = 91 where id = ?", incident.getId()));
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("update reports set longitude = 181 where id = ?", report.getId()));
    }

    @Test
    void timestampsPreserveTheInstantAndTextStoresUnicodeBeyondVarcharLimits() {
        var saved = reports.findById(report.getId()).orElseThrow();
        assertEquals(Instant.parse("2026-10-27T03:00:00.123456Z"), saved.getObservedAt().toInstant());
        String description = "Nước ngập đường, cần hỗ trợ. ".repeat(200);
        report.setDescription(description);
        reports.saveAndFlush(report);
        update.setContent(description);
        updates.saveAndFlush(update);
        assertEquals(description, reports.findById(report.getId()).orElseThrow().getDescription());
        assertEquals(description, updates.findById(update.getId()).orElseThrow().getContent());
    }

    @Test
    void softDeletionIsPersistedAndExcludedFromReportCounts() {
        assertEquals(1, reports.countByIncidentIdAndDeletedAtIsNull(incident.getId()));
        report.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        reports.saveAndFlush(report);
        assertEquals(0, reports.countByIncidentIdAndDeletedAtIsNull(incident.getId()));
        assertEquals(1, reports.count());
        assertTrue(reports.findByIdAndDeletedAtIsNull(report.getId()).isEmpty());
    }

    @Test
    void subscriptionAdapterUsesTheRealJpaTables() {
        var otherWard = new WardEntity();
        otherWard.setCode("DB-TEST-OTHER");
        otherWard.setName("Phường khác");
        otherWard.setProvinceName("Tỉnh thử nghiệm");
        otherWard = wards.saveAndFlush(otherWard);
        var created = subscriptionPort.save(new Subscription(null, user.getId(), otherWard.getId(),
                Instant.parse("2026-10-27T03:00:00.123456Z")));
        assertNotNull(created.id());
        assertTrue(subscriptionPort.existsByUserIdAndWardId(user.getId(), otherWard.getId()));
        assertEquals(2, subscriptionPort.findByUserId(user.getId()).size());
        assertEquals(java.util.stream.Stream.of(ward.getId(), otherWard.getId()).sorted().toList(),
                subscriptionPort.findWardIdsByUserId(user.getId()));
        assertEquals(List.of(user.getId()), subscriptionPort.findUserIdsByWardId(otherWard.getId()));
        assertTrue(subscriptionPort.deleteByUserIdAndWardId(user.getId(), otherWard.getId()));
        assertFalse(subscriptionPort.existsByUserIdAndWardId(user.getId(), otherWard.getId()));
        assertEquals(List.of(ward.getId()), subscriptionPort.findWardIdsByUserId(user.getId()));
    }

    @Test
    void wardIdsAreScopedToTheRequestedUser() {
        var otherUser = new User();
        otherUser.setUsername("another-database-user");
        otherUser.setPasswordHash("test-only-password-hash");
        otherUser.setDisplayName("Another user");
        otherUser = users.saveAndFlush(otherUser);

        var otherWard = new WardEntity();
        otherWard.setCode("DB-OTHER-USER");
        otherWard.setName("Other ward");
        otherWard.setProvinceName("Test province");
        otherWard = wards.saveAndFlush(otherWard);

        subscriptionPort.save(new Subscription(null, otherUser.getId(), otherWard.getId(), Instant.now()));
        assertEquals(List.of(ward.getId()), subscriptionPort.findWardIdsByUserId(user.getId()));
        assertEquals(List.of(otherWard.getId()), subscriptionPort.findWardIdsByUserId(otherUser.getId()));
        assertTrue(subscriptionPort.findWardIdsByUserId(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void aForeignKeyFailureRollsBackOtherChangesInTheSameTransaction() {
        assertThrows(DataIntegrityViolationException.class, () ->
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    jdbc.update("update users set display_name = 'Uncommitted' where id = ?", user.getId());
                    jdbc.update("update reports set incident_id = 9223372036854775806 where id = ?", report.getId());
                }));
        assertEquals("Người dùng thử nghiệm", users.findById(user.getId()).orElseThrow().getDisplayName());
        assertEquals(incident.getId(), reports.findById(report.getId()).orElseThrow().getIncident().getId());
    }
}
