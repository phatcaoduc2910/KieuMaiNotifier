-- SQL Server: run against the existing application database before starting the new version.
-- Fresh databases are created by Hibernate and do not need this migration.
SET XACT_ABORT ON;
BEGIN TRANSACTION;

IF OBJECT_ID(N'dbo.reports', N'U') IS NOT NULL
BEGIN
    ALTER TABLE dbo.reports ALTER COLUMN incident_id BIGINT NULL;

    IF COL_LENGTH(N'dbo.reports', N'ward_id') IS NULL
        EXEC(N'ALTER TABLE dbo.reports ADD ward_id BIGINT NULL');

    IF COL_LENGTH(N'dbo.reports', N'type') IS NULL
        EXEC(N'ALTER TABLE dbo.reports ADD [type] VARCHAR(255) NULL');

    EXEC(N'
        UPDATE r
        SET ward_id = COALESCE(r.ward_id, i.ward_id),
            [type] = COALESCE(r.[type], i.[type])
        FROM dbo.reports r
        INNER JOIN dbo.incidents i ON i.id = r.incident_id
        WHERE r.ward_id IS NULL OR r.[type] IS NULL
    ');
END;

COMMIT TRANSACTION;
