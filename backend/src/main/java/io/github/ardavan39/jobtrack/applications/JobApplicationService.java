package io.github.ardavan39.jobtrack.applications;

import java.time.Clock;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ardavan39.jobtrack.applications.ApplicationDtos.ApplicationDetail;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.ApplicationSummary;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.CreateApplicationRequest;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.PageResponse;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.StatsResponse;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.StatusChangeRequest;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.UpdateApplicationRequest;
import io.github.ardavan39.jobtrack.web.ApiExceptions.ConflictException;
import io.github.ardavan39.jobtrack.web.ApiExceptions.NotFoundException;

/**
 * All methods take the id of the signed-in user and only ever touch that user's applications.
 * Asking for someone else's application gives the same 404 as asking for one that does not exist,
 * so ids cannot be probed.
 */
@Service
public class JobApplicationService {

    private static final Sort RECENTLY_UPDATED_FIRST = Sort.by(Sort.Direction.DESC, "updatedAt").and(Sort.by("id"));

    private final JobApplicationRepository repository;
    private final Clock clock;

    public JobApplicationService(JobApplicationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public ApplicationDetail create(Long userId, CreateApplicationRequest request) {
        ApplicationStatus status = request.status() == null ? ApplicationStatus.WISHLIST : request.status();
        JobApplication application = JobApplication.create(userId, request.details(), status, clock);
        return ApplicationDetail.from(repository.save(application));
    }

    @Transactional(readOnly = true)
    public ApplicationDetail get(Long userId, Long id) {
        return ApplicationDetail.from(load(userId, id));
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationSummary> search(Long userId, ApplicationStatus status, String query, int page, int size) {
        Page<JobApplication> result = repository.search(userId, status, toLikePattern(query),
                PageRequest.of(page, size, RECENTLY_UPDATED_FIRST));
        return new PageResponse<>(result.getContent().stream().map(ApplicationSummary::from).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional
    public ApplicationDetail update(Long userId, Long id, UpdateApplicationRequest request) {
        JobApplication application = load(userId, id);
        if (application.getVersion() != request.version()) {
            throw new ConflictException("The application was changed since you loaded it. Reload it and try again.");
        }
        application.updateDetails(request.details(), clock);
        // flush now so the response carries the new version number
        return ApplicationDetail.from(repository.saveAndFlush(application));
    }

    @Transactional
    public ApplicationDetail changeStatus(Long userId, Long id, StatusChangeRequest request) {
        JobApplication application = load(userId, id);
        application.changeStatus(request.status(), request.note(), clock);
        return ApplicationDetail.from(repository.saveAndFlush(application));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(load(userId, id));
    }

    @Transactional(readOnly = true)
    public StatsResponse stats(Long userId) {
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus status : ApplicationStatus.values()) {
            byStatus.put(status, 0L);
        }
        repository.countByStatus(userId).forEach(count -> byStatus.put(count.getStatus(), count.getCount()));

        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long active = ApplicationStatus.ACTIVE.stream().mapToLong(byStatus::get).sum();
        long sent = total - byStatus.get(ApplicationStatus.WISHLIST);
        Integer interviewRate = null;
        if (sent > 0) {
            long positive = repository.countEverReached(userId, ApplicationStatus.POSITIVE_RESPONSE);
            interviewRate = (int) Math.round(100.0 * positive / sent);
        }
        return new StatsResponse(total, byStatus, active, interviewRate);
    }

    private JobApplication load(Long userId, Long id) {
        return repository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Application " + id + " does not exist"));
    }

    /**
     * Turns free text into a case-insensitive "contains" pattern. {@code %} and {@code _} typed by
     * the user are escaped so they match literally instead of acting as wildcards.
     */
    static String toLikePattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String escaped = query.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escaped + "%";
    }
}
