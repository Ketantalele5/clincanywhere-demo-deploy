#!/usr/bin/env bash
set -Eeuo pipefail

: "${AUDIT_ARCHIVE_MONTH:?Set the closed UTC month to archive, format YYYY-MM}"
: "${BACKUP_AGE_RECIPIENT:?Set BACKUP_AGE_RECIPIENT to the approved age public recipient}"
: "${BACKUP_AGE_RECIPIENT_ID:?Set BACKUP_AGE_RECIPIENT_ID to the key-manager recipient version}"
: "${BACKUP_S3_URI:?Set BACKUP_S3_URI to the locked archive bucket/prefix}"
: "${BACKUP_KMS_KEY_ID:?Set BACKUP_KMS_KEY_ID for S3 SSE-KMS}"
: "${AUDIT_ARCHIVE_RETAIN_UNTIL:?Set an approved ISO-8601 retention date}"
[[ "$AUDIT_ARCHIVE_MONTH" =~ ^[0-9]{4}-(0[1-9]|1[0-2])$ ]] || { echo "AUDIT_ARCHIVE_MONTH must be YYYY-MM" >&2; exit 2; }
command -v docker >/dev/null
command -v age >/dev/null
command -v aws >/dev/null

start="${AUDIT_ARCHIVE_MONTH}-01"
end="$(date -u -d "${start} +1 month" +%Y-%m-%d)"
name="clincanywhere-audit-${AUDIT_ARCHIVE_MONTH}.csv.age"
archive_dir="${AUDIT_ARCHIVE_DIR:-./audit-archives}"
mkdir -p "$archive_dir"
target="${archive_dir%/}/${name}"
[[ ! -e "$target" ]] || { echo "Refusing to overwrite existing archive: $target" >&2; exit 3; }
query="SELECT id,clinic_id,record_category,record_external_id,actor,action,correlation_id,created_at FROM record_audit_events WHERE created_at >= '${start}'::date AND created_at < '${end}'::date ORDER BY id"
docker compose exec -T postgres psql --csv -v ON_ERROR_STOP=1 -U "${POSTGRES_USER:-carepilot}" "${POSTGRES_DB:-carepilot}" -c "$query" \
  | age -r "$BACKUP_AGE_RECIPIENT" -o "$target"
sha256sum "$target" > "${target}.sha256"
checksum="$(cut -d ' ' -f 1 "${target}.sha256")"
printf '{"month":"%s","retentionUntil":"%s","ageRecipientId":"%s","sha256":"%s"}\n' \
  "$AUDIT_ARCHIVE_MONTH" "$AUDIT_ARCHIVE_RETAIN_UNTIL" "$BACKUP_AGE_RECIPIENT_ID" "$checksum" > "${target}.manifest.json"
aws s3 cp "$target" "$BACKUP_S3_URI/" --sse aws:kms --sse-kms-key-id "$BACKUP_KMS_KEY_ID" --object-lock-mode COMPLIANCE --object-lock-retain-until-date "${AUDIT_ARCHIVE_RETAIN_UNTIL:?Set an approved ISO-8601 retention date}"
aws s3 cp "${target}.sha256" "$BACKUP_S3_URI/" --sse aws:kms --sse-kms-key-id "$BACKUP_KMS_KEY_ID" --object-lock-mode COMPLIANCE --object-lock-retain-until-date "$AUDIT_ARCHIVE_RETAIN_UNTIL"
aws s3 cp "${target}.manifest.json" "$BACKUP_S3_URI/" --sse aws:kms --sse-kms-key-id "$BACKUP_KMS_KEY_ID" --object-lock-mode COMPLIANCE --object-lock-retain-until-date "$AUDIT_ARCHIVE_RETAIN_UNTIL"
printf 'Encrypted audit archive uploaded: %s (%s to %s UTC)\n' "$name" "$start" "$end"
