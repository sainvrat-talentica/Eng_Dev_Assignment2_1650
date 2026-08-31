package com.swifteats.common.runtime;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.util.Arrays;
import java.util.List;

/**
 * Excludes beans annotated with {@link ServiceScope} when the active service is not listed.
 * Active service is read from {@code swifteats.service.name} (set in each boot {@code main}).
 */
public class ServiceScopeExcludeFilter implements TypeFilter {

    @Override
    public boolean match(MetadataReader reader, MetadataReaderFactory metadataReaderFactory) {
        var annotation = reader.getAnnotationMetadata().getAnnotationAttributes(ServiceScope.class.getName());
        if (annotation == null) {
            return false;
        }
        ServiceName active = resolveActiveService();
        ServiceName[] scopes = parseScopes(annotation.get("value"));
        return Arrays.stream(scopes).noneMatch(scope -> scope == active);
    }

    static ServiceName[] parseScopes(Object raw) {
        if (raw == null) {
            return new ServiceName[0];
        }
        if (raw instanceof ServiceName[] names) {
            return names;
        }
        if (raw instanceof ServiceName name) {
            return new ServiceName[] {name};
        }
        if (raw instanceof String[] strings) {
            return Arrays.stream(strings).map(ServiceName::valueOf).toArray(ServiceName[]::new);
        }
        if (raw instanceof String string) {
            return new ServiceName[] {ServiceName.valueOf(string)};
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                    .map(item -> item instanceof ServiceName n ? n : ServiceName.valueOf(item.toString()))
                    .toArray(ServiceName[]::new);
        }
        throw new IllegalStateException("Unsupported ServiceScope value type: " + raw.getClass().getName());
    }

    static ServiceName resolveActiveService() {
        String raw = System.getProperty("swifteats.service.name");
        if (raw == null || raw.isBlank()) {
            raw = System.getenv("SWIFTEATS_SERVICE_NAME");
        }
        if (raw == null || raw.isBlank()) {
            return ServiceName.BACKEND;
        }
        return ServiceName.valueOf(raw.trim().toUpperCase());
    }
}
