package com.clincanywhere.controller;

import com.clincanywhere.dto.MessageRequest;
import com.clincanywhere.dto.MessageResponse;
import com.clincanywhere.service.MessagingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/{clinicId}/messages")
public class MessagingController {
    private final MessagingService service;
    public MessagingController(MessagingService service) { this.service = service; }

    @GetMapping
    public List<MessageResponse> conversation(@PathVariable @NotBlank String clinicId,
                                               @RequestParam @NotBlank String with,
                                               @RequestParam(defaultValue = "100") int limit,
                                               Principal principal) {
        return service.conversation(clinicId, principal.getName(), with, Math.max(1, Math.min(limit, 200)));
    }

    @GetMapping("/contacts")
    public List<com.clincanywhere.dto.MessageContact> contacts(@PathVariable @NotBlank String clinicId, Principal principal) {
        return service.contacts(clinicId, principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse send(@PathVariable @NotBlank String clinicId, @Valid @RequestBody MessageRequest request,
                                Principal principal) {
        return service.send(clinicId, principal.getName(), request);
    }
}
