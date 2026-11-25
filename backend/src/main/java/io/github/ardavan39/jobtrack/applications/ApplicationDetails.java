package io.github.ardavan39.jobtrack.applications;

import java.time.LocalDate;

/** The fields of an application that the user edits freely. Status is changed separately. */
public record ApplicationDetails(
        String company,
        String roleTitle,
        String location,
        String jobUrl,
        LocalDate appliedOn,
        String notes) {

    /** Trims text and turns blank optional fields into null, so "" and null mean the same thing. */
    public ApplicationDetails normalised() {
        return new ApplicationDetails(
                company.strip(),
                roleTitle.strip(),
                blankToNull(location),
                blankToNull(jobUrl),
                appliedOn,
                blankToNull(notes));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
