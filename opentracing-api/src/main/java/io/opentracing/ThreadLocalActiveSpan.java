package io.opentracing;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

 /**
 * {@link ThreadLocalActiveSpan} is a trivial {@link ActiveSpan} implementation that relies on Java's thread-local
 * storage primitive.
 *
 * @see ActiveSpanSource
 * @see Tracer#spanSource()
 */
public class ThreadLocalActiveSpan implements ActiveSpan {
    private ThreadLocalActiveSpanSource source;
    private final Span wrapped;
    private ThreadLocalActiveSpan toRestore = null;
    private final AtomicInteger refCount;

    ThreadLocalActiveSpan(ThreadLocalActiveSpanSource source, Span wrapped, AtomicInteger refCount) {
        this.source = source;
        this.refCount = refCount;
        this.wrapped = wrapped;
        this.toRestore = source.tlsSnapshot.get();
        source.tlsSnapshot.set(this);
    }

    @Override
    public void deactivate() {
        if (source.tlsSnapshot.get() != this) {
            // This shouldn't happen if users call methods in the expected order. Bail out.
            return;
        }
        source.tlsSnapshot.set(toRestore);

        if (0 == refCount.decrementAndGet()) {
            wrapped.finish();
        }
    }

    @Override
    public Continuation defer() {
        refCount.incrementAndGet();
        return source.makeContinuation(wrapped, refCount);
    }

    @Override
    public SpanContext context() {
        return wrapped.context();
    }

    @Override
    public void finish() {
        wrapped.finish();
    }

    @Override
    public void finish(long finishMicros) {
        wrapped.finish(finishMicros);
    }

    @Override
    public Span setTag(String key, String value) {
        return wrapped.setTag(key, value);
    }

    @Override
    public Span setTag(String key, boolean value) {
        return wrapped.setTag(key, value);
    }

    @Override
    public Span setTag(String key, Number value) {
        return wrapped.setTag(key, value);
    }

    @Override
    public Span log(Map<String, ?> fields) {
        return wrapped.log(fields);
    }

    @Override
    public Span log(long timestampMicroseconds, Map<String, ?> fields) {
        return wrapped.log(timestampMicroseconds, fields);
    }

    @Override
    public Span log(String event) {
        return wrapped.log(event);
    }

    @Override
    public Span log(long timestampMicroseconds, String event) {
        return wrapped.log(timestampMicroseconds, event);
    }

    @Override
    public Span setBaggageItem(String key, String value) {
        return wrapped.setBaggageItem(key, value);
    }

    @Override
    public String getBaggageItem(String key) {
        return wrapped.getBaggageItem(key);
    }

    @Override
    public Span setOperationName(String operationName) {
        return wrapped.setOperationName(operationName);
    }

    @Override
    public Span log(String eventName, Object payload) {
        return wrapped.log(eventName, payload);
    }

    @Override
    public Span log(long timestampMicroseconds, String eventName, Object payload) {
        return wrapped.log(timestampMicroseconds, eventName, payload);
    }

    @Override
    public void close() {

    }

    static class Continuation implements ActiveSpan.Continuation {
        private ThreadLocalActiveSpanSource source;
        private final Span span;
        private final AtomicInteger refCount;

        Continuation(ThreadLocalActiveSpanSource source, Span span, AtomicInteger refCount) {
            this.source = source;
            this.refCount = refCount;
            this.span = span;
        }

        @Override
        public ThreadLocalActiveSpan activate() {
            return new ThreadLocalActiveSpan(source, span, refCount);
        }
    }

 }
