#!/usr/bin/env bash
set -Eeuo pipefail

: "${BACKUP_AGE_RECIPIENT:?Set BACKUP_AGE_RECIPIENT to a KMS-managed age recipient public key}"
: "${APP_DATA_ENCRYPTION_KEY_ID:?Set APP_DATA_ENCRYPTION_KEY_ID to the secret-manager version identifier}"
: "${APP_PHONE_FINGERPRINT_KEY_ID:?Set APP_PHONE_FINGERPRINT_KEY_ID to the stable secret-manager version identifier}"
backup_dir="${BACKUP_DIR:-./backups}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
name="clincanywhere-${timestamp}.dump.age"
mkdir -p "$backup_dir"
target="${backup_dir%/}/${name}"

command -v docker >/dev/null
command -v age >/dev/null
docker compose exec -T postgres pg_dump --format=custom --no-owner --no-acl -U "${POSTGRES_USER:-carepilot}" "${POSTGRES_DB:-carepilot}" \
  | age -r "$BACKUP_AGE_RECIPIENT" -o "$target"
sha256sum "$target" > "${target}.sha256"
schema_version="$(docker compose exec -T postgres psql -At -U "${POSTGRES_USER:-carepilot}" "${POSTGRES_DB:-carepilot}" -c "SELECT id FROM databasechangelog WHERE exectype='EXECUTED' ORDER BY orderexecuted DESC LIMIT 1" | tr -d '\r')"
checksum="$(cut -d ' ' -f 1 "${target}.sha256")"
printf '{"createdAt":"%s","database":"%s","schemaVersion":"%s","encryptionKeyId":"%s","phoneFingerprintKeyId":"%s","sha256":"%s"}\n' \
  "$timestamp" "${POSTGRES_DB:-carepilot}" "$schema_version" "$APP_DATA_ENCRYPTION_KEY_ID" "$APP_PHONE_FINGERPRINT_KEY_ID" "$checksum" > "${target}.manifest.json"

if [[ -n "${BACKUP_S3_URI:-}" ]]; then
  command -v aws >/dev/null
  aws s3 cp "$target" "$BACKUP_S3_URI/" --sse aws:kms --sse-kms-key-id "${BACKUP_KMS_KEY_ID:?Set BACKUP_KMS_KEY_ID for off-site backup upload}"
  aws s3 cp "${target}.sha256" "$BACKUP_S3_URI/" --sse aws:kms --sse-kms-key-id "$BACKUP_KMS_KEY_ID"
  aws s3 cp "${target}.manifest.json" "$BACKUP_S3_URI/" --sse aws:kms --sse-kms-key-id "$BACKUP_KMS_KEY_ID"
fi
printf 'Encrypted backup written: %s\n' "$target"
