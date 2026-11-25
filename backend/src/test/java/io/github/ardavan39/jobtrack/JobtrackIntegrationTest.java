package io.github.ardavan39.jobtrack;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The real application against a real MySQL (Testcontainers), called over HTTP the way the
 * frontend calls it. Skipped automatically on machines without Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers(disabledWithoutDocker = true)
class JobtrackIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    private static final ParameterizedTypeReference<Map<String, Object>> JSON = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private TestRestTemplate rest;

    @Test
    void registerLoginAndTrackAnApplicationThroughItsLifecycle() {
        String email = uniqueEmail();
        assertThat(register(email, "correct horse battery").getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // the same email again, in different case, is taken
        assertThat(register(email.toUpperCase(), "another password").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // wrong password and unknown email get the same answer
        assertThat(login(email, "wrong password").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(uniqueEmail(), "correct horse battery").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        String token = tokenOf(login(email, "correct horse battery"));
        assertThat(call(HttpMethod.GET, "/api/me", token, null).getBody()).containsEntry("email", email);

        // create
        ResponseEntity<Map<String, Object>> created = call(HttpMethod.POST, "/api/applications", token, Map.of(
                "company", "Acme", "roleTitle", "Backend Engineer", "location", "Amsterdam",
                "jobUrl", "https://acme.example/jobs/1", "status", "APPLIED"));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Number id = (Number) created.getBody().get("id");
        assertThat(created.getBody()).containsEntry("status", "APPLIED").containsEntry("version", 0);
        assertThat(created.getBody().get("appliedOn")).isEqualTo(LocalDate.now(ZoneOffset.UTC).toString());

        // edit with the version we loaded
        ResponseEntity<Map<String, Object>> updated = call(HttpMethod.PUT, "/api/applications/" + id, token, Map.of(
                "company", "Acme B.V.", "roleTitle", "Senior Backend Engineer", "version", 0));
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("company", "Acme B.V.").containsEntry("version", 1);

        // a second edit based on the old version is refused instead of overwriting
        ResponseEntity<Map<String, Object>> stale = call(HttpMethod.PUT, "/api/applications/" + id, token, Map.of(
                "company", "Old data", "roleTitle", "Backend Engineer", "version", 0));
        assertThat(stale.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // move it forward; each step is in the history
        call(HttpMethod.PATCH, "/api/applications/" + id + "/status", token, Map.of("status", "INTERVIEWING", "note", "Call with HR"));
        ResponseEntity<Map<String, Object>> offer = call(HttpMethod.PATCH, "/api/applications/" + id + "/status", token,
                Map.of("status", "OFFER"));
        assertThat(offer.getBody()).containsEntry("status", "OFFER");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> history = (List<Map<String, Object>>) offer.getBody().get("history");
        assertThat(history).extracting(change -> change.get("to")).containsExactly("APPLIED", "INTERVIEWING", "OFFER");
        assertThat(history.get(1)).containsEntry("note", "Call with HR");

        // the same status twice is a bad request
        assertThat(call(HttpMethod.PATCH, "/api/applications/" + id + "/status", token, Map.of("status", "OFFER"))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // delete
        assertThat(call(HttpMethod.DELETE, "/api/applications/" + id, token, null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(call(HttpMethod.GET, "/api/applications/" + id, token, null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchAndStatsOnlySeeTheUsersOwnApplications() {
        String ada = registerAndGetToken();
        String bob = registerAndGetToken();

        create(ada, "Acme", "Backend Engineer", "WISHLIST");
        create(ada, "Globex", "Java Developer", "APPLIED");
        create(ada, "Initech", "Full-stack Developer", "APPLIED");
        Number interviewing = create(ada, "Umbrella", "Platform Engineer", "APPLIED");
        call(HttpMethod.PATCH, "/api/applications/" + interviewing + "/status", ada, Map.of("status", "INTERVIEWING"));
        Number bobs = create(bob, "Acme", "Backend Engineer", "APPLIED");

        Map<String, Object> page = call(HttpMethod.GET, "/api/applications?q=acme", ada, null).getBody();
        assertThat(page).containsEntry("totalItems", 1);

        Map<String, Object> javaRoles = call(HttpMethod.GET, "/api/applications?q=JAVA", ada, null).getBody();
        assertThat(javaRoles).containsEntry("totalItems", 1);

        Map<String, Object> applied = call(HttpMethod.GET, "/api/applications?status=APPLIED", ada, null).getBody();
        assertThat(applied).containsEntry("totalItems", 2);

        // "%" is matched literally, not as "anything"
        Map<String, Object> percent = rest.exchange("/api/applications?q={q}", HttpMethod.GET,
                new HttpEntity<>(bearer(ada)), JSON, "%").getBody();
        assertThat(percent).containsEntry("totalItems", 0);

        Map<String, Object> stats = call(HttpMethod.GET, "/api/applications/stats", ada, null).getBody();
        assertThat(stats).containsEntry("total", 4).containsEntry("active", 3);
        // one of the three sent applications led to an interview
        assertThat(stats).containsEntry("interviewRate", 33);

        // Ada cannot see, change or delete Bob's application
        assertThat(call(HttpMethod.GET, "/api/applications/" + bobs, ada, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.PATCH, "/api/applications/" + bobs + "/status", ada, Map.of("status", "REJECTED"))
                .getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.DELETE, "/api/applications/" + bobs, ada, null).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(call(HttpMethod.GET, "/api/applications/" + bobs, bob, null).getBody()).containsEntry("status", "APPLIED");
    }

    @Test
    void statsForANewUserHaveNoRateYet() {
        Map<String, Object> stats = call(HttpMethod.GET, "/api/applications/stats", registerAndGetToken(), null).getBody();

        assertThat(stats).containsEntry("total", 0).containsEntry("interviewRate", null);
    }

    private Number create(String token, String company, String role, String status) {
        return (Number) call(HttpMethod.POST, "/api/applications", token,
                Map.of("company", company, "roleTitle", role, "status", status)).getBody().get("id");
    }

    private String registerAndGetToken() {
        return tokenOf(register(uniqueEmail(), "correct horse battery"));
    }

    private ResponseEntity<Map<String, Object>> register(String email, String password) {
        return call(HttpMethod.POST, "/api/auth/register", null,
                Map.of("email", email, "password", password, "displayName", "Test User"));
    }

    private ResponseEntity<Map<String, Object>> login(String email, String password) {
        return call(HttpMethod.POST, "/api/auth/login", null, Map.of("email", email, "password", password));
    }

    private static String tokenOf(ResponseEntity<Map<String, Object>> response) {
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        return (String) response.getBody().get("token");
    }

    private ResponseEntity<Map<String, Object>> call(HttpMethod method, String path, String token, Object body) {
        return rest.exchange(path, method, new HttpEntity<>(body, bearer(token)), JSON);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return headers;
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
