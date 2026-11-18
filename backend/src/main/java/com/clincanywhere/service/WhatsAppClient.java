package com.clincanywhere.service;

public interface WhatsAppClient {
    /** Returns Meta's message identifier, used to reconcile asynchronous webhook receipts. */
    String sendText(String phoneNumber, String message);
}
