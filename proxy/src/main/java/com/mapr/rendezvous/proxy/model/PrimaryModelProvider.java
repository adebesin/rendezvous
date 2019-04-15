package com.mapr.rendezvous.proxy.model;

import java.util.Optional;

public interface PrimaryModelProvider {

    Optional<String> getPrimaryModel();

}
