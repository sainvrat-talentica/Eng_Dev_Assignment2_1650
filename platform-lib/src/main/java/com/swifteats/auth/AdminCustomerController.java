package com.swifteats.auth;

import com.swifteats.auth.dto.AdminCustomerDetailResponse;
import com.swifteats.auth.dto.AdminCustomerSummaryResponse;
import com.swifteats.auth.dto.UpdateCustomerStatusRequest;
import com.swifteats.common.config.OpenApiConfig;
import com.swifteats.common.runtime.ServiceName;
import com.swifteats.common.runtime.ServiceScope;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/customers")
@SecurityRequirement(name = OpenApiConfig.ADMIN_API_KEY)
@ServiceScope(ServiceName.BACKEND)
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;

    public AdminCustomerController(AdminCustomerService adminCustomerService) {
        this.adminCustomerService = adminCustomerService;
    }

    @GetMapping
    public List<AdminCustomerSummaryResponse> listCustomers(
            @RequestParam(required = false) String status) {
        return adminCustomerService.listForAdmin(status);
    }

    @GetMapping("/{id}")
    public AdminCustomerDetailResponse getCustomer(@PathVariable UUID id) {
        return adminCustomerService.getForAdmin(id);
    }

    @PatchMapping("/{id}/status")
    public AdminCustomerDetailResponse updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateCustomerStatusRequest request) {
        return adminCustomerService.updateStatus(id, request.status());
    }
}
