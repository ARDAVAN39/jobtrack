package io.github.ardavan39.jobtrack.applications;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class ApplicationDtos {

    private ApplicationDtos() {
    }

    public record CreateApplicationRequest(
            @NotBlank @Size(max = 150) String company,
            @NotBlank @Size(max = 150) String roleTitle,
            @Size(max = 150) String location,
            @Size(max = 500) @Pattern(regexp = "^(https?://.+)?$", message = "must start with http:// or https://") String jobUrl,
            @PastOrPresent LocalDate appliedOn,
            @Size(max = 5000) String notes,
            /* defaults to WISHLIST */
            ApplicationStatus status) {

        ApplicationDetails details() {
            return new ApplicationDetails(company, roleTitle, location, jobUrl, appliedOn, notes);
        }
    }

    /**
     * @param version the version the client loaded; the update is refused if the application has
     *                changed since
     */
    public record UpdateApplicationRequest(
            @NotBlank @Size(max = 150) String company,
            @NotBlank @Size(max = 150) String roleTitle,
            @Size(max = 150) String location,
            @Size(max = 500) @Pattern(regexp = "^(https?://.+)?$", message = "must start with http:// or https://") String jobUrl,
            @PastOrPresent LocalDate appliedOn,
            @Size(max = 5000) String notes,
            @NotNull Long version) {

        ApplicationDetails details() {
            return new ApplicationDetails(company, roleTitle, location, jobUrl, appliedOn, notes);
        }
    }

    public record StatusChangeRequest(@NotNull ApplicationStatus status, @Size(max = 500) String note) {
    }

    public record ApplicationSummary(
            Long id,
            String company,
            String roleTitle,
            String location,
            ApplicationStatus status,
            LocalDate appliedOn,
            Instant updatedAt) {

        static ApplicationSummary from(JobApplication a) {
            return new ApplicationSummary(a.getId(), a.getCompany(), a.getRoleTitle(), a.getLocation(), a.getStatus(),
                    a.getAppliedOn(), a.getUpdatedAt());
        }
    }

    public record ApplicationDetail(
            Long id,
            String company,
            String roleTitle,
            String location,
            String jobUrl,
            ApplicationStatus status,
            LocalDate appliedOn,
            String notes,
            long version,
            Instant createdAt,
            Instant updatedAt,
            List<StatusChangeResponse> history) {

        static ApplicationDetail from(JobApplication a) {
            return new ApplicationDetail(a.getId(), a.getCompany(), a.getRoleTitle(), a.getLocation(), a.getJobUrl(),
                    a.getStatus(), a.getAppliedOn(), a.getNotes(), a.getVersion(), a.getCreatedAt(), a.getUpdatedAt(),
                    a.getHistory().stream().map(StatusChangeResponse::from).toList());
        }
    }

    public record StatusChangeResponse(ApplicationStatus from, ApplicationStatus to, String note, Instant changedAt) {

        static StatusChangeResponse from(StatusChange c) {
            return new StatusChangeResponse(c.getFromStatus(), c.getToStatus(), c.getNote(), c.getChangedAt());
        }
    }

    /**
     * @param interviewRate share (0-100) of applications sent that led to an interview or offer;
     *                      null until at least one application has been sent
     */
    public record StatsResponse(long total, Map<ApplicationStatus, Long> byStatus, long active, Integer interviewRate) {
    }

    public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    }
}
