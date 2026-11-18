package com.clincanywhere.controller;

import com.clincanywhere.common.exception.BadRequestException;
import com.clincanywhere.security.RecordScope;
import com.clincanywhere.service.CareRecordService;
import com.clincanywhere.dto.RecordRequest;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.clincanywhere.dto.RecordResponse;

@RestController
@Validated
@RequestMapping("/api/{clinicId}/pharmacy-workflow")
public class PharmacyDispenseController {
    private final RecordScope scope; private final CareRecordService records;
    public PharmacyDispenseController(RecordScope scope,CareRecordService records){this.scope=scope;this.records=records;}
    @PostMapping("/{prescriptionId}/dispense")
    @Transactional
    public Map<String,Object> dispense(@PathVariable String clinicId,@PathVariable String prescriptionId,@Valid @RequestBody DispenseRequest request,Authentication auth){
        scope.requireMember(clinicId,auth);
        if(!auth.getAuthorities().stream().anyMatch(a->a.getAuthority().equals("ROLE_PLATFORM_OWNER")||a.getAuthority().equals("ROLE_PHARMACIST")||a.getAuthority().equals("ROLE_ADMIN")||a.getAuthority().equals("ROLE_INVENTORY"))) throw new org.springframework.security.access.AccessDeniedException("Pharmacy or inventory role required");
        RecordResponse prescription;
        try {
            prescription = records.lockForUpdate(clinicId, "prescriptions", prescriptionId);
        } catch (com.clincanywhere.common.exception.NotFoundException missing) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found");
        }
        var fields=new LinkedHashMap<>(prescription.fields());
        if(!"Finalized".equalsIgnoreCase(String.valueOf(fields.get("clinicalStatus")))) throw new BadRequestException("Only a doctor-finalized prescription can be dispensed");
        String name=request.medicineName().trim(); int quantity=request.quantity();
        var prescribedMedicines = java.util.Arrays.stream(String.valueOf(fields.getOrDefault("medicines", "")).split("[,;\n]"))
                .map(part -> part.trim()).filter(part -> !part.isBlank()).toList();
        String normalizedName = name.toLowerCase(java.util.Locale.ROOT);
        String alreadyDispensed = String.valueOf(fields.getOrDefault("dispensedItems", ""));
        var dispensedLines = java.util.Arrays.stream(alreadyDispensed.split("\\n"))
                .map(line -> line.split("\\|", 2)).filter(parts -> parts.length == 2)
                .map(parts -> parts[0].toLowerCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        boolean prescribed = prescribedMedicines.stream()
                .filter(part -> part.toLowerCase(java.util.Locale.ROOT).contains(normalizedName))
                .findAny().isPresent();
        if (!prescribed) throw new BadRequestException("Medicine is not listed on this prescription");
        if (dispensedLines.contains(normalizedName)) throw new BadRequestException("This medicine was already dispensed for this prescription");
        RecordResponse stock;
        try {
            stock = records.lockByPayloadName(clinicId, "pharmacy", name);
        } catch (com.clincanywhere.common.exception.NotFoundException missing) {
            throw new BadRequestException("Medicine not found in clinic stock");
        }
        var stockFields = new LinkedHashMap<>(stock.fields());
        int available;
        try {
            available = Integer.parseInt(String.valueOf(stockFields.getOrDefault("stock", 0)));
        } catch (NumberFormatException invalidStock) {
            throw new BadRequestException("Stock quantity is invalid for this medicine");
        }
        if(available<quantity) throw new BadRequestException("Insufficient stock: available quantity is "+available);
        String existing=alreadyDispensed;
        stockFields.put("stock",available-quantity); records.updateForDispense(clinicId,"pharmacy",String.valueOf(stock.fields().get("id")),requestFor(stockFields),auth);
        String updatedItems=existing.isBlank()?name+"|"+quantity:existing+"\n"+name+"|"+quantity;
        var updatedNames = java.util.Arrays.stream(updatedItems.split("\\n"))
                .map(line -> line.split("\\|", 2)).filter(parts -> parts.length == 2)
                .map(parts -> parts[0].toLowerCase(java.util.Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
        boolean fullyDispensed=prescribedMedicines.stream().allMatch(item -> updatedNames.stream()
                .anyMatch(dispensed -> item.toLowerCase(java.util.Locale.ROOT).contains(dispensed)));
        fields.put("dispensedItems",updatedItems); fields.put("pharmacyStatus",fullyDispensed?"Dispensed":"Processing"); fields.put("dispensedMedicine",name); fields.put("dispensedQuantity",quantity); fields.put("dispensedBy",auth.getName()); fields.put("dispensedAt",java.time.Instant.now().toString());
        records.updateForDispense(clinicId,"prescriptions",prescriptionId,requestFor(fields),auth);
        return Map.of("prescriptionId",prescriptionId,"medicine",name,"dispensed",quantity,"remainingStock",available-quantity,"status",fullyDispensed?"Dispensed":"Processing");
    }
    private RecordRequest requestFor(Map<String,Object> fields){ var request=new RecordRequest(); fields.forEach(request::addField); return request; }
    public record DispenseRequest(@jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=120) String medicineName,@jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(9999) int quantity){}
}
