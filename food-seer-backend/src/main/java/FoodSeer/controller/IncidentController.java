package FoodSeer.controller;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import FoodSeer.dto.AcknowledgeIncidentDto;
import FoodSeer.dto.CustomerReactionRequestDto;
import FoodSeer.dto.IncidentDto;
import FoodSeer.dto.IncidentReportResultDto;
import FoodSeer.dto.RestaurantIncidentRequestDto;
import FoodSeer.exception.ErrorDetails;
import FoodSeer.service.TraceResult;
import FoodSeer.service.TraceService;

/**
 * Allergy incident reports and the restaurant's view of them.
 *
 * <ul>
 * <li>POST /api/incidents: staff report a contaminated dish.</li>
 * <li>POST /api/incidents/reaction: a customer reports a reaction on their own order.</li>
 * <li>GET /api/incidents: staff list every incident, with counts only.</li>
 * <li>POST /api/incidents/{id}/acknowledge: staff acknowledge and attach a remedy.</li>
 * </ul>
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final TraceService traceService;

    /**
     * Creates the controller.
     *
     * @param traceService
     *            the trace-back service
     */
    public IncidentController(final TraceService traceService) {
        this.traceService = traceService;
    }

    /**
     * Restaurant staff report that dishes may carry an allergen during a window.
     *
     * @param request
     *            allergen, dish ids and window
     * @param authentication
     *            the signed-in staff member
     * @return 201 with the incident id and how many orders and customers were affected
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping
    public ResponseEntity<IncidentReportResultDto> reportRestaurantIncident(
            @RequestBody final RestaurantIncidentRequestDto request, final Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("A report body is required.");
        }
        final TraceResult result = traceService.reportRestaurantIncident(authentication.getName(),
                request.allergen(), request.foodIds(), request.windowStart(), request.windowEnd());
        final IncidentReportResultDto body = new IncidentReportResultDto(result.getIncident().getId(),
                result.getExposedOrderIds().size(), result.getNotifications().size());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * A customer reports a reaction after one of their own orders. The reply
     * holds only the incident id, never how many others were affected.
     *
     * @param request
     *            order id and allergen
     * @param authentication
     *            the signed-in customer
     * @return 201 with the incident id
     */
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/reaction")
    public ResponseEntity<IncidentReportResultDto> reportCustomerReaction(
            @RequestBody final CustomerReactionRequestDto request, final Authentication authentication) {
        if (request == null) {
            throw new IllegalArgumentException("A report body is required.");
        }
        final TraceResult result = traceService.reportCustomerReaction(authentication.getName(), request.orderId(),
                request.allergen());
        final IncidentReportResultDto body = new IncidentReportResultDto(result.getIncident().getId(), null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    /**
     * Lists every incident for staff, newest first.
     *
     * @return incident summaries
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @GetMapping
    public List<IncidentDto> listIncidents() {
        return traceService.listIncidents();
    }

    /**
     * Staff acknowledge an incident and say what they offer affected customers.
     *
     * @param id
     *            the incident
     * @param request
     *            the remedy text
     * @param authentication
     *            the signed-in staff member
     * @return the updated incident summary
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @PostMapping("/{id}/acknowledge")
    public IncidentDto acknowledgeIncident(@PathVariable("id") final Long id,
            @RequestBody final AcknowledgeIncidentDto request, final Authentication authentication) {
        final String remedy;
        if (request == null) {
            remedy = null;
        } else {
            remedy = request.remedy();
        }
        return traceService.acknowledgeIncident(id, authentication.getName(), remedy);
    }

    /**
     * Turns bad input from the service into 400 Bad Request with its message.
     *
     * @param ex
     *            the validation error
     * @return 400 with the message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDetails> handleBadInput(final IllegalArgumentException ex) {
        final ErrorDetails details = new ErrorDetails(new Date(), ex.getMessage(), "Invalid incident request");
        return new ResponseEntity<>(details, HttpStatus.BAD_REQUEST);
    }
}
