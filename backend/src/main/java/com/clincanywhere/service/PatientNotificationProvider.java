package com.clincanywhere.service;
public interface PatientNotificationProvider { boolean supports(String channel); String send(String channel,String destination,String message); }
