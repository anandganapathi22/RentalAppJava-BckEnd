CREATE TABLE IF NOT EXISTS "rentalapps-customer-data" (
    "id" VARCHAR(255) PRIMARY KEY,
    "customerName" VARCHAR(255),
    "locationCode" VARCHAR(64),
    "stall" VARCHAR(64),
    "oneClub" VARCHAR(128),
    "ra" VARCHAR(128),
    "sourceSystem" VARCHAR(64),
    "arrivalDate" VARCHAR(32),
    "arrivalTime" VARCHAR(32),
    "createdDatetime" VARCHAR(64),
    "updatedDatetime" VARCHAR(64)
);

CREATE INDEX IF NOT EXISTS "idx_rentalapps_customer_locationCode"
    ON "rentalapps-customer-data" ("locationCode");

CREATE TABLE IF NOT EXISTS "rentalapps-audit-data" (
    "id" VARCHAR(255) NOT NULL,
    "operationTime" VARCHAR(128) NOT NULL,
    "operationDate" VARCHAR(64),
    "customerName" VARCHAR(255),
    "locationCode" VARCHAR(64),
    "stall" VARCHAR(64),
    "oneClub" VARCHAR(128),
    "ra" VARCHAR(128),
    "sourceSystem" VARCHAR(64),
    "arrivalDate" VARCHAR(32),
    "arrivalTime" VARCHAR(32),
    "updatedDatetime" VARCHAR(64),
    "OPERATION" VARCHAR(32),
    "expiresAt" BIGINT,
    PRIMARY KEY ("id", "operationTime")
);

CREATE INDEX IF NOT EXISTS "idx_rentalapps_audit_locationCode"
    ON "rentalapps-audit-data" ("locationCode");

CREATE TABLE IF NOT EXISTS "rentalapps-locations-data" (
    "hertzLocationCode" VARCHAR(64) PRIMARY KEY,
    "displayName" VARCHAR(255),
    "timeZone" VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS "rentalapps-shedlock-data" (
    "name" VARCHAR(64) PRIMARY KEY,
    "lock_until" TIMESTAMP,
    "locked_at" TIMESTAMP,
    "locked_by" VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS "rentalapps-users-data" (
    "username" VARCHAR(128) PRIMARY KEY,
    "passwordHash" VARCHAR(255) NOT NULL,
    "role" VARCHAR(64) NOT NULL,
    "enabled" BOOLEAN NOT NULL DEFAULT TRUE,
    "createdDatetime" VARCHAR(64),
    "updatedDatetime" VARCHAR(64)
);
