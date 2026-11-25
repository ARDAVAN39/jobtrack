package io.github.ardavan39.jobtrack.applications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import io.github.ardavan39.jobtrack.web.ApiExceptions.BadRequestException;

class JobApplicationTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-20T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);

    @Test
    void aNewApplicationStartsItsHistoryWithItsStatus() {
        JobApplication application = JobApplication.create(1L, details(null), ApplicationStatus.WISHLIST, CLOCK);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.WISHLIST);
        assertThat(application.getHistory()).singleElement().satisfies(change -> {
            assertThat(change.getFromStatus()).isNull();
            assertThat(change.getToStatus()).isEqualTo(ApplicationStatus.WISHLIST);
            assertThat(change.getChangedAt()).isEqualTo(CLOCK.instant());
        });
        assertThat(application.getAppliedOn()).isNull();
    }

    @Test
    void creatingItAsAppliedWithoutADateMeansAppliedToday() {
        JobApplication application = JobApplication.create(1L, details(null), ApplicationStatus.APPLIED, CLOCK);

        assertThat(application.getAppliedOn()).isEqualTo(TODAY);
    }

    @Test
    void aDateGivenByTheUserIsKept() {
        LocalDate lastWeek = TODAY.minusDays(7);

        JobApplication application = JobApplication.create(1L, details(lastWeek), ApplicationStatus.APPLIED, CLOCK);

        assertThat(application.getAppliedOn()).isEqualTo(lastWeek);
    }

    @Test
    void changingStatusAddsToTheHistory() {
        JobApplication application = JobApplication.create(1L, details(null), ApplicationStatus.WISHLIST, CLOCK);

        application.changeStatus(ApplicationStatus.APPLIED, null, CLOCK);
        application.changeStatus(ApplicationStatus.INTERVIEWING, "  First call with HR  ", CLOCK);

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.INTERVIEWING);
        assertThat(application.getAppliedOn()).isEqualTo(TODAY);
        assertThat(application.getHistory()).hasSize(3);
        StatusChange last = application.getHistory().get(2);
        assertThat(last.getFromStatus()).isEqualTo(ApplicationStatus.APPLIED);
        assertThat(last.getToStatus()).isEqualTo(ApplicationStatus.INTERVIEWING);
        assertThat(last.getNote()).isEqualTo("First call with HR");
    }

    @Test
    void changingToTheSameStatusIsRefused() {
        JobApplication application = JobApplication.create(1L, details(null), ApplicationStatus.APPLIED, CLOCK);

        assertThatThrownBy(() -> application.changeStatus(ApplicationStatus.APPLIED, null, CLOCK))
                .isInstanceOf(BadRequestException.class);
        assertThat(application.getHistory()).hasSize(1);
    }

    @Test
    void blankOptionalFieldsAreStoredAsNullAndTextIsTrimmed() {
        ApplicationDetails details = new ApplicationDetails("  Acme  ", " Backend Engineer ", "   ", "", null, " ");

        JobApplication application = JobApplication.create(1L, details, ApplicationStatus.WISHLIST, CLOCK);

        assertThat(application.getCompany()).isEqualTo("Acme");
        assertThat(application.getRoleTitle()).isEqualTo("Backend Engineer");
        assertThat(application.getLocation()).isNull();
        assertThat(application.getJobUrl()).isNull();
        assertThat(application.getNotes()).isNull();
    }

    @Test
    void searchTextIsEscapedSoWildcardsMatchLiterally() {
        assertThat(JobApplicationService.toLikePattern("  Acme ")).isEqualTo("%acme%");
        assertThat(JobApplicationService.toLikePattern("100%_remote!")).isEqualTo("%100!%!_remote!!%");
        assertThat(JobApplicationService.toLikePattern("   ")).isNull();
        assertThat(JobApplicationService.toLikePattern(null)).isNull();
    }

    private static ApplicationDetails details(LocalDate appliedOn) {
        return new ApplicationDetails("Acme", "Backend Engineer", "Amsterdam", "https://acme.example/jobs/1", appliedOn, null);
    }
}
