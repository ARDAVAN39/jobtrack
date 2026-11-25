package io.github.ardavan39.jobtrack.applications;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import io.github.ardavan39.jobtrack.web.ApiExceptions.BadRequestException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * A job the user applied to (or wants to). The status can only change through
 * {@link #changeStatus}, which also writes the timeline entry, so the two never disagree.
 */
@Entity
@Table(name = "job_applications")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kept as a plain id: the application never needs the user's data, only to know whose it is. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false, length = 150)
    private String company;

    @Column(name = "role_title", nullable = false, length = 150)
    private String roleTitle;

    @Column(length = 150)
    private String location;

    @Column(name = "job_url", length = 500)
    private String jobUrl;

    // VARCHAR rather than Hibernate's default native ENUM type on MySQL, to match the migration
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ApplicationStatus status;

    @Column(name = "applied_on")
    private LocalDate appliedOn;

    @Column(columnDefinition = "text")
    private String notes;

    /** Optimistic locking: a save based on stale data fails instead of overwriting newer changes. */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<StatusChange> history = new ArrayList<>();

    protected JobApplication() {
        // for JPA
    }

    public static JobApplication create(Long userId, ApplicationDetails details, ApplicationStatus status, Clock clock) {
        JobApplication application = new JobApplication();
        Instant now = clock.instant();
        application.userId = userId;
        application.createdAt = now;
        application.apply(details.normalised(), now);
        application.status = status;
        application.history.add(new StatusChange(application, null, status, null, now));
        application.fillAppliedOnIfApplying(clock);
        return application;
    }

    public void updateDetails(ApplicationDetails details, Clock clock) {
        apply(details.normalised(), clock.instant());
    }

    public void changeStatus(ApplicationStatus next, String note, Clock clock) {
        if (next == status) {
            throw new BadRequestException("The application is already " + status);
        }
        Instant now = clock.instant();
        history.add(new StatusChange(this, status, next, note == null || note.isBlank() ? null : note.strip(), now));
        status = next;
        updatedAt = now;
        fillAppliedOnIfApplying(clock);
    }

    /** Moving to APPLIED without a date means "I applied today". */
    private void fillAppliedOnIfApplying(Clock clock) {
        if (status == ApplicationStatus.APPLIED && appliedOn == null) {
            appliedOn = LocalDate.now(clock);
        }
    }

    private void apply(ApplicationDetails details, Instant now) {
        company = details.company();
        roleTitle = details.roleTitle();
        location = details.location();
        jobUrl = details.jobUrl();
        appliedOn = details.appliedOn();
        notes = details.notes();
        updatedAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getCompany() {
        return company;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public String getLocation() {
        return location;
    }

    public String getJobUrl() {
        return jobUrl;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public LocalDate getAppliedOn() {
        return appliedOn;
    }

    public String getNotes() {
        return notes;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<StatusChange> getHistory() {
        return Collections.unmodifiableList(history);
    }
}
