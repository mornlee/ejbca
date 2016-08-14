-- These columns are added by the JPA provider if there are sufficient privileges
-- ALTER TABLE CertificateData ADD notBefore INT8;
-- ALTER TABLE CertificateData ADD endEntityProfileId INT4;
-- ALTER TABLE CertificateData ADD subjectAltName TEXT;
--
-- Table ProfileData is new and is added by the JPA provider if there are sufficient privileges. 
-- See create-tables-database.sql
--
-- subjectDN and subjectAltName columns in UserData has been extended to accommodate longer names for most databases
-- In PostgreSQL the column was already TEXT so no change is needed