package io.github.ardavan39.jobtrack.applications;

import java.util.EnumSet;
import java.util.Set;

public enum ApplicationStatus {
    /** Saved to apply to later. */
    WISHLIST,
    APPLIED,
    INTERVIEWING,
    OFFER,
    REJECTED,
    /** Withdrawn by the candidate. */
    WITHDRAWN;

    /** Statuses that mean the employer answered with interest. */
    public static final Set<ApplicationStatus> POSITIVE_RESPONSE = EnumSet.of(INTERVIEWING, OFFER);

    /** Still waiting on the employer or in their process. */
    public static final Set<ApplicationStatus> ACTIVE = EnumSet.of(APPLIED, INTERVIEWING);
}
