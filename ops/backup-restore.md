# Production backup and restore runbook

This runbook is a deployment requirement; Docker named volumes are not backups. The repository includes [`ops/backup.sh`](backup.sh), which streams a custom-format `pg_dump` through `age` encryption without writing a plaintext dump to disk. Schedule it on a secured Linux host with Docker Compose, `age`, and (for off-site upload) AWS CLI installed. Use managed PostgreSQL point-in-time recovery as an additional control where available.

## Backup

1. Provide `BACKUP_AGE_RECIPIENT` (public recipient), `BACKUP_S3_URI`, `BACKUP_KMS_KEY_ID`, `APP_DATA_ENCRYPTION_KEY_ID`, and stable `APP_PHONE_FINGERPRINT_KEY_ID` from deployment configuration. The age identity and AWS upload credentials stay in the secret manager/host identity and are never placed in the command line or committed.
2. Run `BACKUP_AGE_RECIPIENT=age1... BACKUP_S3_URI=s3://<locked-bucket>/<prefix> BACKUP_KMS_KEY_ID=<key-id> APP_DATA_ENCRYPTION_KEY_ID=<secret-version> APP_PHONE_FINGERPRINT_KEY_ID=<stable-secret-version> ./ops/backup.sh` from the repository root. The script writes an encrypted dump, checksum, and manifest containing schema/key identifiers, then uploads them with SSE-KMS; configure the target bucket in a separate account/region with object lock and lifecycle retention.
3. Include the application encryption key version/identifier and database migration version in the backup manifest. Do not store the key beside the backup.
4. Alert on a missed backup, failed upload, checksum mismatch, or backup age beyond the documented RPO.

## Restore drill

1. Restore the selected backup into an isolated, access-controlled PostgreSQL instance. Do not restore over production.
2. Decrypt the backup using KMS, run `pg_restore --exit-on-error`, and start the matching application release with the matching encryption key.
3. Verify Liquibase/Hibernate startup, record reads and writes, authentication, clinic isolation, and audit rows using approved synthetic data. Compare table counts/checksums against the backup manifest.
4. Record start/end time, achieved RPO/RTO, release and schema versions, evidence location, operator, and any corrective actions. A restore drill is complete only after an application-level read succeeds.
5. Run quarterly and after material database, encryption-key, or backup-policy changes. Retain signed drill evidence per the organization's retention schedule.

Example secured host cron schedule (daily at 02:15 UTC; route output to monitored logs): `15 2 * * * cd /srv/clincanywhere && ./ops/backup.sh`.

## Audit archive

The append-only application audit metadata can be exported by closed UTC month using [`ops/archive-audit.sh`](archive-audit.sh). Configure the approved retention date from the records schedule and run after month close, for example `AUDIT_ARCHIVE_MONTH=2026-09 AUDIT_ARCHIVE_RETAIN_UNTIL=2033-10-01T00:00:00Z ./ops/archive-audit.sh`. The script encrypts CSV before disk write, refuses to overwrite an existing local month archive, uploads the archive/checksum/key-reference manifest with S3 Object Lock COMPLIANCE retention, and does not delete database rows. Configure `BACKUP_AGE_RECIPIENT_ID` and confirm bucket Object Lock is enabled at creation; test authorized recovery before scheduling. The retention date must come from Legal/Privacy; do not choose one from this example.

## Encryption key rotation

Back up the current keys in the secret manager's recovery mechanism before rotation. The application uses one AES-256-GCM data key and `enc:v1` ciphertext; startup migration encrypts legacy plaintext but does not re-key existing ciphertext. It does not yet support dual-key online rotation. For data-key rotation, schedule a maintenance window, stop writes, run an approved re-encryption migration that decrypts with the old key and writes with the new key, verify record/message/outbox reads, then retire the old key only after backup and restore evidence exists. Keep `APP_PHONE_FINGERPRINT_KEY` stable: changing it would make existing consent and opt-out fingerprints unmatchable because phone numbers are intentionally not stored in that table.

Do not enable real patient data until automated backup scheduling, off-site retention, alerting, and a successful restore drill are configured for the target environment.
