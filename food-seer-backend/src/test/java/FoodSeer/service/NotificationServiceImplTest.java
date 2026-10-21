package FoodSeer.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import FoodSeer.dto.NotificationDto;
import FoodSeer.entity.Incident;
import FoodSeer.entity.Notification;
import FoodSeer.exception.ResourceNotFoundException;
import FoodSeer.repositories.IncidentRepository;
import FoodSeer.repositories.NotificationRepository;
import FoodSeer.service.impl.NotificationServiceImpl;

/**
 * Unit tests for {@link NotificationServiceImpl} with mocked repositories.
 */
public class NotificationServiceImplTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 10, 12, 12, 0);

    private NotificationRepository notificationRepository;
    private IncidentRepository incidentRepository;
    private NotificationServiceImpl service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        incidentRepository = mock(IncidentRepository.class);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new NotificationServiceImpl(notificationRepository, incidentRepository);
    }

    private Notification notice(final long id, final String recipient, final long incidentId) {
        final Notification notification = new Notification();
        notification.setId(id);
        notification.setIncidentId(incidentId);
        notification.setOrderId(8L);
        notification.setRecipientUsername(recipient);
        notification.setMessage("Food safety notice: your order #8 (COOKIES) may have been exposed to PEANUT");
        notification.setCreatedAt(NOON);
        notification.setAcknowledged(false);
        return notification;
    }

    private Incident incidentWithRemedy(final long id, final String remedy) {
        final Incident incident = new Incident();
        incident.setId(id);
        incident.setRemedy(remedy);
        return incident;
    }

    // getNotifications
    @Test
    public void testListIncludesAllFieldsAndRemedy() {
        when(notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(Arrays.asList(notice(1L, "alice", 3L)));
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incidentWithRemedy(3L, "Full refund")));

        final List<NotificationDto> list = service.getNotifications("alice");

        assertEquals(1, list.size());
        final NotificationDto dto = list.get(0);
        assertEquals(1L, dto.id());
        assertEquals(3L, dto.incidentId());
        assertEquals(8L, dto.orderId());
        assertTrue(dto.message().contains("PEANUT"));
        assertEquals(NOON, dto.createdAt());
        assertFalse(dto.acknowledged());
        assertNull(dto.acknowledgedAt());
        assertEquals("Full refund", dto.remedy());
    }

    @Test
    public void testRemedyIsNullBeforeTheRestaurantResponds() {
        when(notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(Arrays.asList(notice(1L, "alice", 3L)));
        when(incidentRepository.findById(3L)).thenReturn(Optional.of(incidentWithRemedy(3L, null)));

        assertNull(service.getNotifications("alice").get(0).remedy());
    }

    @Test
    public void testRemedyIsNullWhenIncidentWasRemoved() {
        when(notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(Arrays.asList(notice(1L, "alice", 3L)));
        when(incidentRepository.findById(3L)).thenReturn(Optional.empty());

        assertNull(service.getNotifications("alice").get(0).remedy());
    }

    @Test
    public void testListKeepsNewestFirstOrderFromRepository() {
        when(notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(Arrays.asList(notice(2L, "alice", 3L), notice(1L, "alice", 3L)));

        final List<NotificationDto> list = service.getNotifications("alice");

        assertEquals(2L, list.get(0).id());
        assertEquals(1L, list.get(1).id());
    }

    @Test
    public void testEmptyInbox() {
        when(notificationRepository.findByRecipientUsernameOrderByCreatedAtDesc("alice"))
                .thenReturn(new ArrayList<Notification>());

        assertTrue(service.getNotifications("alice").isEmpty());
    }

    @Test
    public void testListRejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> service.getNotifications(null));
        assertThrows(IllegalArgumentException.class, () -> service.getNotifications("  "));
    }

    // countUnacknowledged
    @Test
    public void testCountComesFromRepository() {
        when(notificationRepository.countByRecipientUsernameAndAcknowledgedFalse("alice")).thenReturn(3L);

        assertEquals(3L, service.countUnacknowledged("alice"));
    }

    @Test
    public void testCountRejectsBlankUsername() {
        assertThrows(IllegalArgumentException.class, () -> service.countUnacknowledged(null));
        assertThrows(IllegalArgumentException.class, () -> service.countUnacknowledged(""));
    }

    // acknowledge
    @Test
    public void testAcknowledgeOwnNotice() {
        final Notification notification = notice(1L, "alice", 3L);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        final NotificationDto dto = service.acknowledge(1L, "alice");

        assertTrue(dto.acknowledged());
        assertNotNull(dto.acknowledgedAt());
        verify(notificationRepository).save(notification);
    }

    @Test
    public void testAcknowledgeTwiceKeepsFirstTime() {
        final Notification notification = notice(1L, "alice", 3L);
        final LocalDateTime firstTime = NOON.plusMinutes(3);
        notification.setAcknowledged(true);
        notification.setAcknowledgedAt(firstTime);
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notification));

        final NotificationDto dto = service.acknowledge(1L, "alice");

        assertTrue(dto.acknowledged());
        assertEquals(firstTime, dto.acknowledgedAt());
    }

    @Test
    public void testCannotAcknowledgeSomeoneElsesNotice() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(notice(1L, "bob", 3L)));

        assertThrows(AccessDeniedException.class, () -> service.acknowledge(1L, "alice"));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    public void testAcknowledgeUnknownNotice() {
        when(notificationRepository.findById(99L)).thenReturn(Optional.empty());

        final ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.acknowledge(99L, "alice"));
        assertEquals("Notification does not exist with id 99", ex.getMessage());
    }

    @Test
    public void testAcknowledgeRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> service.acknowledge(null, "alice"));
        assertThrows(IllegalArgumentException.class, () -> service.acknowledge(1L, null));
        assertThrows(IllegalArgumentException.class, () -> service.acknowledge(1L, " "));
        verify(notificationRepository, never()).save(any());
    }
}
