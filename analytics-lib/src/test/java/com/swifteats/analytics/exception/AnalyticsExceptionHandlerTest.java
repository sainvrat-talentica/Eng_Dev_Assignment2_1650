package com.swifteats.analytics.exception;

import com.swifteats.common.exception.ImportInProgressException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsExceptionHandlerTest {

    private final AnalyticsExceptionHandler handler = new AnalyticsExceptionHandler();

    @Test
    void handleImportInProgress_returns409() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/admin/analytics/import");

        var response = handler.handleImportInProgress(new ImportInProgressException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("IMPORT_IN_PROGRESS");
    }

    @Test
    void handleAnalyticsDataNotLoaded_returns412() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/analytics/delays");

        var response = handler.handleAnalyticsDataNotLoaded(new AnalyticsDataNotLoadedException(), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PRECONDITION_FAILED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ANALYTICS_DATA_NOT_LOADED");
    }
}
