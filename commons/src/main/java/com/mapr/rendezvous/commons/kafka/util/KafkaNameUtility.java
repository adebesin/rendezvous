package com.mapr.rendezvous.commons.kafka.util;

import lombok.experimental.UtilityClass;

import static java.lang.String.format;

@UtilityClass
public class KafkaNameUtility {

    public String convertToKafkaFormat(String folder, String name) {
        return format(folder.endsWith("/") ? "%s%s" : "%s/%s", folder, name);
    }

    public String convertToKafkaTopic(String stream, String topic) {
        return format("%s:%s", stream, topic);
    }
}
