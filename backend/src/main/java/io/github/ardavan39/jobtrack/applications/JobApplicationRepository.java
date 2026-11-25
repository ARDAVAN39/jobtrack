package io.github.ardavan39.jobtrack.applications;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every query takes the user id. There is no method that loads an application by id alone, so
 * code in this project cannot accidentally hand one user's data to another.
 */
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    Optional<JobApplication> findByIdAndUserId(Long id, Long userId);

    /** {@code pattern} is a lower-case LIKE pattern that uses {@code !} as escape character, or null. */
    @Query("""
            select a from JobApplication a
            where a.userId = :userId
              and (:status is null or a.status = :status)
              and (:pattern is null
                   or lower(a.company) like :pattern escape '!'
                   or lower(a.roleTitle) like :pattern escape '!')
            """)
    Page<JobApplication> search(@Param("userId") Long userId, @Param("status") ApplicationStatus status,
            @Param("pattern") String pattern, Pageable pageable);

    @Query("select a.status as status, count(a) as count from JobApplication a where a.userId = :userId group by a.status")
    List<StatusCount> countByStatus(@Param("userId") Long userId);

    /** How many of the user's applications have been in one of the statuses at some point. */
    @Query("""
            select count(distinct c.application.id) from StatusChange c
            where c.application.userId = :userId and c.toStatus in :statuses
            """)
    long countEverReached(@Param("userId") Long userId, @Param("statuses") Collection<ApplicationStatus> statuses);

    interface StatusCount {
        ApplicationStatus getStatus();

        long getCount();
    }
}
