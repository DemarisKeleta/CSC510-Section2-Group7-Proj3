package FoodSeer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import FoodSeer.config.SpringSecurityConfig;
import FoodSeer.dto.IncidentDto;
import FoodSeer.entity.Incident;
import FoodSeer.entity.Notification;
import FoodSeer.entity.User;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.repositories.UserRepository;
import FoodSeer.security.CustomUserDetailsService;
import FoodSeer.security.DriverStatsAccess;
import FoodSeer.security.JwtAuthenticationFilter;
import FoodSeer.security.JwtTokenProvider;
import FoodSeer.service.TraceResult;
import FoodSeer.service.TraceService;
import FoodSeer.service.impl.JwtAccessDeniedHandler;
import FoodSeer.service.impl.JwtAuthenticationEntryPoint;

/**
 * Real JWT login and method security for /api/incidents, with the trace
 * service mocked. No database or server needed.
 */
@WebMvcTest(value = IncidentController.class, properties = {
    "app.jwt-secret=Zm9vZHNlZXItdGVzdC1vbmx5LXNpZ25pbmcta2V5LTMyaXRlbXM=",
    "app.jwt-expiration-milliseconds=60000"
})
@Import({SpringSecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
    CustomUserDetailsService.class, DriverStatsAccess.class,
    JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class IncidentControllerTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 12, 12, 0);

    private static final String RESTAURANT_BODY = "{\"allergen\":\"peanut\",\"foodIds\":[10,11],"
            + "\"windowStart\":\"2026-10-12T11:00:00\",\"windowEnd\":\"2026-10-12T13:00:00\"}";

    private static final String REACTION_BODY = "{\"orderId\":5,\"allergen\":\"peanut\"}";

    @Autowired private MockMvc mvc;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtTokenProvider tokenProvider;
    @Autowired private PasswordEncoder passwordEncoder;
    @MockBean private UserRepository users;
    @MockBean private TraceService traceService;

    @BeforeEach
    void setUp() {
        final String password = passwordEncoder.encode("test-password");
        final String[][] accounts = {
            {"alice", "ROLE_CUSTOMER"}, {"staff", "ROLE_STAFF"}, {"admin", "ROLE_ADMIN"}, {"driver1", "ROLE_DRIVER"}
        };
        for (final String[] account : accounts) {
            final User user = User.builder().username(account[0]).email(account[0] + "@example.test")
                    .password(password).role(account[1]).build();
            when(users.findByUsernameOrEmail(account[0], account[0])).thenReturn(Optional.of(user));
        }
    }

    private String bearer(final String login) {
        final var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(login, "test-password"));
        return "Bearer " + tokenProvider.generateToken(authentication);
    }

    private TraceResult resultWithCounts(final long incidentId, final int exposed, final int notified) {
        final Incident incident = new Incident();
        incident.setId(incidentId);
        final List<Long> exposedIds = new java.util.ArrayList<>();
        for (long i = 1; i <= exposed; i++) {
            exposedIds.add(i);
        }
        final List<Notification> notifications = new java.util.ArrayList<>();
        for (int i = 0; i < notified; i++) {
            notifications.add(new Notification());
        }
        return new TraceResult(incident, exposedIds, notifications);
    }

    private IncidentDto summary(final long id, final String remedy) {
        return new IncidentDto(id, "RESTAURANT", "PEANUT", null, List.of(10L), List.of("COOKIES"),
                NOON.minusHours(1), NOON.plusHours(1), NOON, 3, 1, null, null, remedy);
    }

    // POST /api/incidents (restaurant report)
    @Test
    void anonymousCannotReport() throws Exception {
        mvc.perform(post("/api/incidents").contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(traceService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "admin"})
    void staffAndAdminCanReportAndSeeCounts(final String login) throws Exception {
        when(traceService.reportRestaurantIncident(eq(login), eq("peanut"), eq(List.of(10L, 11L)),
                eq(NOON.minusHours(1)), eq(NOON.plusHours(1)))).thenReturn(resultWithCounts(42L, 3, 2));

        mvc.perform(post("/api/incidents").header("Authorization", bearer(login))
                        .contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(42))
                .andExpect(jsonPath("$.exposedOrderCount").value(3))
                .andExpect(jsonPath("$.notifiedCount").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "driver1"})
    void customersAndDriversCannotFileRestaurantReports(final String login) throws Exception {
        mvc.perform(post("/api/incidents").header("Authorization", bearer(login))
                        .contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(traceService);
    }

    @Test
    void invalidRestaurantReportIsBadRequestWithMessage() throws Exception {
        when(traceService.reportRestaurantIncident(anyString(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("The window start must not be after the window end."));

        mvc.perform(post("/api/incidents").header("Authorization", bearer("staff"))
                        .contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The window start must not be after the window end."));
    }

    @Test
    void unknownFoodIsNotFound() throws Exception {
        when(traceService.reportRestaurantIncident(anyString(), any(), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Food does not exist with id 99"));

        mvc.perform(post("/api/incidents").header("Authorization", bearer("staff"))
                        .contentType(MediaType.APPLICATION_JSON).content(RESTAURANT_BODY))
                .andExpect(status().isNotFound());
    }

    // POST /api/incidents/reaction (customer report)
    @Test
    void customerCanReportAReactionAndSeesNoCounts() throws Exception {
        when(traceService.reportCustomerReaction("alice", 5L, "peanut")).thenReturn(resultWithCounts(7L, 4, 3));

        mvc.perform(post("/api/incidents/reaction").header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(REACTION_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.incidentId").value(7))
                .andExpect(jsonPath("$.exposedOrderCount").doesNotExist())
                .andExpect(jsonPath("$.notifiedCount").doesNotExist());
        verify(traceService).reportCustomerReaction("alice", 5L, "peanut");
    }

    @ParameterizedTest
    @ValueSource(strings = {"staff", "driver1"})
    void onlyCustomersCanReportReactions(final String login) throws Exception {
        mvc.perform(post("/api/incidents/reaction").header("Authorization", bearer(login))
                        .contentType(MediaType.APPLICATION_JSON).content(REACTION_BODY))
                .andExpect(status().isForbidden());
        verifyNoInteractions(traceService);
    }

    @Test
    void reactionOnSomeoneElsesOrderIsForbidden() throws Exception {
        when(traceService.reportCustomerReaction("alice", 5L, "peanut"))
                .thenThrow(new AccessDeniedException("You can only report a reaction on your own order."));

        mvc.perform(post("/api/incidents/reaction").header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(REACTION_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactionOnUntraceableOrderIsBadRequest() throws Exception {
        when(traceService.reportCustomerReaction("alice", 5L, "peanut"))
                .thenThrow(new IllegalArgumentException("This order has no recorded time, so it cannot be traced."));

        mvc.perform(post("/api/incidents/reaction").header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content(REACTION_BODY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This order has no recorded time, so it cannot be traced."));
    }

    // GET /api/incidents
    @Test
    void staffCanListIncidents() throws Exception {
        when(traceService.listIncidents()).thenReturn(List.of(summary(1L, null)));

        mvc.perform(get("/api/incidents").header("Authorization", bearer("staff")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].allergen").value("PEANUT"))
                .andExpect(jsonPath("$[0].suspectFoodNames[0]").value("COOKIES"))
                .andExpect(jsonPath("$[0].notifiedCount").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"alice", "driver1"})
    void nonStaffCannotListIncidents(final String login) throws Exception {
        mvc.perform(get("/api/incidents").header("Authorization", bearer(login)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(traceService);
    }

    // POST /api/incidents/{id}/acknowledge
    @Test
    void staffCanAcknowledgeWithARemedy() throws Exception {
        when(traceService.acknowledgeIncident(1L, "staff", "Full refund or a free remake"))
                .thenReturn(summary(1L, "Full refund or a free remake"));

        mvc.perform(post("/api/incidents/1/acknowledge").header("Authorization", bearer("staff"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remedy\":\"Full refund or a free remake\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remedy").value("Full refund or a free remake"));
        verify(traceService).acknowledgeIncident(1L, "staff", "Full refund or a free remake");
    }

    @Test
    void customerCannotAcknowledge() throws Exception {
        mvc.perform(post("/api/incidents/1/acknowledge").header("Authorization", bearer("alice"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remedy\":\"x\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(traceService);
    }

    @Test
    void blankRemedyIsBadRequest() throws Exception {
        when(traceService.acknowledgeIncident(1L, "staff", " "))
                .thenThrow(new IllegalArgumentException("Please describe what the restaurant offers affected customers."));

        mvc.perform(post("/api/incidents/1/acknowledge").header("Authorization", bearer("staff"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remedy\":\" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acknowledgingUnknownIncidentIsNotFound() throws Exception {
        when(traceService.acknowledgeIncident(99L, "staff", "Refund"))
                .thenThrow(new ResourceNotFoundException("Incident does not exist with id 99"));

        mvc.perform(post("/api/incidents/99/acknowledge").header("Authorization", bearer("staff"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"remedy\":\"Refund\"}"))
                .andExpect(status().isNotFound());
    }
}
