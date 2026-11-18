package com.clincanywhere.mapper;
import java.util.Map;
import com.clincanywhere.entity.CareRecord;
import com.clincanywhere.dto.RecordResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;

@Component
public class CareRecordMapper {
 private final ObjectMapper mapper;
 public CareRecordMapper(ObjectMapper mapper){this.mapper=mapper;}
 public CareRecord toEntity(String clinic, String category, String id, Map<String, Object> data) {
  return new CareRecord(clinic, category, id, serialize(data));
 }
 public String serialize(Map<String, Object> data) {
  if (data == null) throw new IllegalArgumentException("Record data is required");
  try { return mapper.writeValueAsString(data); }
  catch (JsonProcessingException e) { throw new IllegalArgumentException("Record could not be serialized", e); }
 }
 public RecordResponse toResponse(CareRecord entity) {
  return toResponse(entity, entity.getPayloadJson());
 }
 public RecordResponse toResponse(CareRecord entity, String payload) {
  if (entity == null || payload == null || payload.isBlank()) throw new IllegalStateException("Stored record payload is missing");
  try {
   Map<String, Object> fields = mapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
   if (fields == null) throw new IllegalStateException("Stored record payload must be a JSON object");
   return new RecordResponse(fields);
  }
  catch (JsonProcessingException e) { throw new IllegalStateException("Stored record is not valid JSON", e); }
 }
}
