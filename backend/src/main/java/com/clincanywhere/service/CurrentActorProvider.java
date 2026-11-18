package com.clincanywhere.service;

/** Supplies the identity associated with the current application operation. */
public interface CurrentActorProvider {
    String currentActor();
}
