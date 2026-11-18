package com.clincanywhere.controller;

import java.util.List;

import com.clincanywhere.config.DemoData;
import com.clincanywhere.service.CareRecordService;
import com.clincanywhere.security.RecordScope;
import com.clincanywhere.dto.RecordRequest;
import com.clincanywhere.dto.RecordResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
@RestController
@RequestMapping("/api")
public class CareRecordController {
    private final CareRecordService service;
    private final DemoData demoData;
    private final RecordScope scope;

    public CareRecordController(CareRecordService service, DemoData demoData, RecordScope scope) {
        this.service = service;
        this.demoData = demoData;
        this.scope = scope;
    }

    @GetMapping("/{clinicId}/{category}")
    public List<RecordResponse> list(@PathVariable @NotBlank String clinicId,
                                     @PathVariable @NotBlank String category, Authentication authentication) {
        return service.list(clinicId, category, authentication);
    }

    @GetMapping(value = "/{clinicId}/{category}", params = {"page", "size"})
    public List<RecordResponse> listPage(@PathVariable @NotBlank String clinicId,
                                         @PathVariable @NotBlank String category,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "50") int size,
                                         Authentication authentication) {
        if (page < 0 || size < 1 || size > 200) throw new com.clincanywhere.common.exception.BadRequestException("Invalid page or size (maximum size is 200)");
        return service.list(clinicId, category, authentication, page, size);
    }

    @GetMapping(value = "/{clinicId}/{category}", params = {"q", "size"})
    public List<RecordResponse> search(@PathVariable @NotBlank String clinicId,
                                       @PathVariable @NotBlank String category,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "50") int size,
                                       Authentication authentication) {
        if (size < 1 || size > 200 || q != null && q.length() > 160) throw new com.clincanywhere.common.exception.BadRequestException("Invalid search query or size (maximum size is 200)");
        return service.search(clinicId, category, authentication, q, size);
    }

    @GetMapping("/{clinicId}/{category}/{externalId}")
    public RecordResponse get(@PathVariable @NotBlank String clinicId,
                              @PathVariable @NotBlank String category,
                              @PathVariable @NotBlank String externalId, Authentication authentication) {
        return service.get(clinicId, category, externalId, authentication);
    }

    @PostMapping("/{clinicId}/{category}")
    @PreAuthorize("@recordAuthorization.canWrite(#category, authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    public RecordResponse create(@PathVariable @NotBlank String clinicId,
                                 @PathVariable @NotBlank String category,
                                 @Valid @RequestBody RecordRequest payload, Authentication authentication) {
        validateScope(clinicId, category, authentication);
        return service.create(clinicId, category, payload, authentication.getName(), scope.isDoctor(authentication.getName()));
    }

    @PutMapping("/{clinicId}/{category}/{externalId}")
    @PreAuthorize("@recordAuthorization.canWrite(#category, authentication)")
    public RecordResponse update(@PathVariable @NotBlank String clinicId,
                                 @PathVariable @NotBlank String category,
                                 @PathVariable @NotBlank String externalId,
                                 @Valid @RequestBody RecordRequest payload, Authentication authentication) {
        validateScope(clinicId, category, authentication);
        return service.update(clinicId, category, externalId, payload, authentication.getName(), scope.isDoctor(authentication.getName()));
    }

    @DeleteMapping("/{clinicId}/{category}/{externalId}")
    @PreAuthorize("@recordAuthorization.canWrite(#category, authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @NotBlank String clinicId,
                       @PathVariable @NotBlank String category,
                       @PathVariable @NotBlank String externalId, Authentication authentication) {
        validateScope(clinicId, category, authentication);
        service.delete(clinicId, category, externalId, authentication.getName(), scope.isDoctor(authentication.getName()));
    }

    @DeleteMapping("/{clinicId}/demo-data")
    @PreAuthorize("@recordAuthorization.isAdmin(authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetDemoData(@PathVariable @NotBlank String clinicId, Authentication authentication) {
        if (!scope.isAdmin(authentication)) throw new org.springframework.security.access.AccessDeniedException("Administrator role required");
        scope.requireMember(clinicId, authentication);
        demoData.resetClinic(clinicId);
    }

    private void validateScope(String clinicId, String category, Authentication authentication) {
        scope.requireScope(clinicId, category);
        scope.requireMember(clinicId, authentication);
    }

}
