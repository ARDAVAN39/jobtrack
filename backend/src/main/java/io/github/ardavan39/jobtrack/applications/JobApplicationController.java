package io.github.ardavan39.jobtrack.applications;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.github.ardavan39.jobtrack.applications.ApplicationDtos.ApplicationDetail;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.ApplicationSummary;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.CreateApplicationRequest;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.PageResponse;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.StatsResponse;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.StatusChangeRequest;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.UpdateApplicationRequest;
import io.github.ardavan39.jobtrack.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/applications")
@Tag(name = "Applications")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @Operation(summary = "List your applications, most recently updated first")
    @GetMapping
    public PageResponse<ApplicationSummary> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.search(CurrentUser.id(jwt), status, q, page, size);
    }

    @Operation(summary = "Add an application")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApplicationDetail create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateApplicationRequest request) {
        return service.create(CurrentUser.id(jwt), request);
    }

    @Operation(summary = "Get an application with its status history")
    @GetMapping("/{id}")
    public ApplicationDetail get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @Operation(summary = "Edit an application. Send the version you loaded; a stale version gets 409.")
    @PutMapping("/{id}")
    public ApplicationDetail update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody UpdateApplicationRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @Operation(summary = "Move an application to another status; the change is added to its history")
    @PatchMapping("/{id}/status")
    public ApplicationDetail changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
            @Valid @RequestBody StatusChangeRequest request) {
        return service.changeStatus(CurrentUser.id(jwt), id, request);
    }

    @Operation(summary = "Delete an application")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }

    @Operation(summary = "Counts per status and interview rate")
    @GetMapping("/stats")
    public StatsResponse stats(@AuthenticationPrincipal Jwt jwt) {
        return service.stats(CurrentUser.id(jwt));
    }
}
