package com.mapr.rendezvous.commons.kafka;

import com.mapr.streams.Admin;
import com.mapr.streams.StreamDescriptor;
import com.mapr.streams.Streams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class AdminService {

    private Admin admin;

    @SneakyThrows
    public AdminService() {
        admin = Streams.newAdmin(new Configuration());
    }

    @SneakyThrows
    public boolean streamExists(final String stream) {
        return admin.streamExists(stream);
    }

    @SneakyThrows
    public Set<String> getTopicNames(final String stream) {
        return new HashSet<>(admin.listTopics(stream));
    }

    @SneakyThrows
    public int countTopics(final String stream) {
        return admin.countTopics(stream);
    }

    @SneakyThrows
    public int getTopicPartitions(final String stream, final String topic) {
        return admin.getTopicDescriptor(stream, topic).getPartitions();
    }

    @SneakyThrows
    public void createStream(final String stream) {
        log.info("Creating stream {}", stream);
        admin.createStream(stream, Streams.newStreamDescriptor());
    }

    public Mono<Void> createStreamAsync(String stream) {
        return Mono.create(voidMonoSink -> {
            try {
                admin.createStream(stream, Streams.newStreamDescriptor());
                voidMonoSink.success();
            } catch (IOException e) {
                voidMonoSink.error(e);
            }
        });
    }

    @SneakyThrows
    public void createTopic(final String stream, final String topic) {
        log.info("Creating topic {}:{}", stream, topic);
        admin.createTopic(stream, topic);
    }

    public Mono<Void> createTopicAsync(String stream, String topic) {
        return Mono.create(voidMonoSink -> {
            try {
                admin.createTopic(stream, topic);
                voidMonoSink.success();
            } catch (IOException e) {
                voidMonoSink.error(e);
            }
        });
    }

    @SneakyThrows
    public void removeStream(final String stream) {
        log.info("Removing stream {}", stream);
        admin.deleteStream(stream);
    }

    @SneakyThrows
    public void removeTopic(final String stream, final String topic) {
        log.info("Removing topic {}:{}", stream, topic);
        admin.deleteTopic(stream, topic);
    }

    @SneakyThrows
    public boolean topicExists(String stream, String topic) {
        return admin.listTopics(stream).contains(topic);
    }

    @SneakyThrows
    public StreamDescriptor getStreamDescriptor(String stream) {
        return admin.getStreamDescriptor(stream);
    }

    public boolean createStreamAndTopicIfNotExists(String topic) {
        String[] nameParts = topic.split(":");

        if (nameParts.length != 2)
            throw new RuntimeException("Should have stream separated from topic by ':' " + topic);

        createStreamIfNotExists(nameParts[0]);
        return createTopicIfNotExists(nameParts[0], nameParts[1]);
    }

    public boolean createStreamIfNotExists(String stream) {
        if (!streamExists(stream)) {
            createStream(stream);
            return true;
        } else {
            return false;
        }
    }

    public boolean createTopicIfNotExists(String stream, String topic) {
        if (!topicExists(stream, topic)) {
            createTopic(stream, topic);
            return true;
        } else {
            return false;
        }
    }

}
