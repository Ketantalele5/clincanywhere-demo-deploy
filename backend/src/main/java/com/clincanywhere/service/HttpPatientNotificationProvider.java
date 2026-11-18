package com.clincanywhere.service;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;

@Component @ConditionalOnProperty(name="app.notifications.provider-enabled",havingValue="true")
public class HttpPatientNotificationProvider implements PatientNotificationProvider{
 private final RestClient client;private final String token,smsSender,emailSender;
 public HttpPatientNotificationProvider(@Value("${app.notifications.endpoint:}")String endpoint,@Value("${app.notifications.api-token:}")String token,@Value("${app.notifications.sms-sender:}")String smsSender,@Value("${app.notifications.email-sender:}")String emailSender){String configuredEndpoint=Objects.requireNonNull(endpoint,"Notification endpoint");this.token=Objects.requireNonNull(token,"Notification API token");this.smsSender=Objects.requireNonNull(smsSender,"SMS sender");this.emailSender=Objects.requireNonNull(emailSender,"Email sender");if(configuredEndpoint.isBlank()||this.token.isBlank())throw new IllegalStateException("Notification endpoint and token required when enabled");var factory=new JdkClientHttpRequestFactory(java.util.Objects.requireNonNull(java.util.Objects.requireNonNull(HttpClient.newBuilder()).connectTimeout(java.util.Objects.requireNonNull(Duration.ofSeconds(3))).build()));factory.setReadTimeout(java.util.Objects.requireNonNull(Duration.ofSeconds(5)));client=RestClient.builder().baseUrl(configuredEndpoint).requestFactory(factory).build();}
 public boolean supports(String channel){return "SMS".equals(channel)||"EMAIL".equals(channel);}
 public String send(String channel,String destination,String message){Objects.requireNonNull(channel,"Notification channel");Objects.requireNonNull(destination,"Notification destination");Objects.requireNonNull(message,"Notification message");ProviderResponse response=client.post().uri("/messages").contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_JSON)).header("Authorization","Bearer "+token).body(java.util.Objects.requireNonNull(Map.of("channel",channel,"to",destination,"message",message,"from",channel.equals("SMS")?smsSender:emailSender))).retrieve().body(ProviderResponse.class);if(response==null||response.id()==null||response.id().isBlank())throw new IllegalStateException("Notification provider did not return message reference");return response.id();}

 private record ProviderResponse(String id) {}
}
