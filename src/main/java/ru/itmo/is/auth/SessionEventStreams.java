package ru.itmo.is.auth;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.ws.rs.sse.SseEventSink;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@SessionScoped
public class SessionEventStreams implements Serializable {

    private transient List<SseEventSink> streams;

    public synchronized void add(SseEventSink sink) {
        if (streams == null) {
            streams = new ArrayList<>();
        }
        streams.removeIf(SseEventSink::isClosed);
        streams.add(sink);
    }

    @PreDestroy
    private synchronized void close() {
        if (streams == null) {
            return;
        }
        for (SseEventSink stream : streams) {
            try {
                stream.close();
            } catch (IOException ignored) {
                // The connection may already be gone.
            }
        }
        streams.clear();
    }
}
