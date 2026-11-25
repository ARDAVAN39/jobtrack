package io.github.ardavan39.jobtrack.applications;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ardavan39.jobtrack.applications.ApplicationDtos.ApplicationSummary;
import io.github.ardavan39.jobtrack.applications.ApplicationDtos.PageResponse;
import io.github.ardavan39.jobtrack.config.SecurityConfig;
import io.github.ardavan39.jobtrack.web.ApiExceptions.NotFoundException;

/** The HTTP layer with the real security configuration and a mocked service. */
@WebMvcTest(JobApplicationController.class)
@Import(SecurityConfig.class)
class JobApplicationControllerTest {

    private static final String APPLICATIONS = "/api/applications";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobApplicationService service;

    @Test
    void withoutATokenTheRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get(APPLICATIONS))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void anInvalidTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get(APPLICATIONS).header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void theUserIdComesFromTheToken() throws Exception {
        when(service.search(eq(7L), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(new PageResponse<>(List.<ApplicationSummary>of(), 0, 20, 0, 0));

        mockMvc.perform(get(APPLICATIONS).with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());

        verify(service).search(7L, null, null, 0, 20);
    }

    @Test
    void filtersArePassedThrough() throws Exception {
        when(service.search(eq(7L), eq(ApplicationStatus.APPLIED), eq("acme"), eq(1), eq(10)))
                .thenReturn(new PageResponse<>(List.of(), 1, 10, 0, 0));

        mockMvc.perform(get(APPLICATIONS).param("status", "APPLIED").param("q", "acme").param("page", "1").param("size", "10")
                .with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isOk());
    }

    @Test
    void anOversizedPageIsABadRequest() throws Exception {
        mockMvc.perform(get(APPLICATIONS).param("size", "500").with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void validationErrorsAreReportedPerField() throws Exception {
        mockMvc.perform(post(APPLICATIONS)
                .with(jwt().jwt(token -> token.subject("7")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"company": "", "roleTitle": "Backend Engineer", "jobUrl": "ftp://acme.example"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.company").exists())
                .andExpect(jsonPath("$.errors.jobUrl").value("must start with http:// or https://"));
        verifyNoInteractions(service);
    }

    @Test
    void someoneElsesApplicationIsNotFound() throws Exception {
        when(service.get(7L, 99L)).thenThrow(new NotFoundException("Application 99 does not exist"));

        mockMvc.perform(get(APPLICATIONS + "/99").with(jwt().jwt(token -> token.subject("7"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Application 99 does not exist"));
    }

    @Test
    void aStatusChangeNeedsAKnownStatus() throws Exception {
        mockMvc.perform(patch(APPLICATIONS + "/1/status")
                .with(jwt().jwt(token -> token.subject("7")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"HIRED\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).changeStatus(any(), any(), any());
    }
}
