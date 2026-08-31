package com.swifteats.analytics.service;

import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;

import com.swifteats.common.exception.ImportInProgressException;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Component
@ServiceScope(ServiceName.ANALYTICS)
public class ImportLock {

    private final AtomicBoolean inProgress = new AtomicBoolean(false);

    public <T> T execute(Supplier<T> action) {
        if (!inProgress.compareAndSet(false, true)) {
            throw new ImportInProgressException();
        }
        try {
            return action.get();
        } finally {
            inProgress.set(false);
        }
    }
}
