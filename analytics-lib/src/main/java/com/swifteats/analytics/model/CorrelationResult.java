package com.swifteats.analytics.model;

import java.util.List;

public record CorrelationResult(List<CorrelationRuleMatch> matches) {
}
