package com.clincanywhere.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
public record PatientNotificationRequest(@NotBlank @Pattern(regexp="WHATSAPP|SMS|EMAIL") String channel,@NotBlank @Size(max=200) String destination,@NotBlank @Size(max=2000) String message,boolean consentConfirmed){}
