package com.gamehub.backend.business.impl;

import org.junit.jupiter.api.Test;
import com.gamehub.backend.domain.User;
import com.gamehub.backend.domain.Event;
import com.gamehub.backend.persistence.UserRepository;
import com.gamehub.backend.persistence.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private EventServiceImpl eventService;

    private Event event;
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("TestUser");

        event = new Event();
        event.setId(1L);
        event.setName("Test Event");
        event.setDescription("Test Description");
        event.setStartDate(new Date());
        event.setEndDate(new Date());
        event.setParticipants(new HashSet<>(Collections.singletonList(user)));
    }

    @Test
    void createEvent() {
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        Event createdEvent = eventService.createEvent(event);

        assertNotNull(createdEvent);
        assertEquals(event.getId(), createdEvent.getId());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void getEventById() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        Optional<Event> foundEvent = eventService.getEventById(1L);

        assertTrue(foundEvent.isPresent());
        assertEquals(event.getId(), foundEvent.get().getId());
        verify(eventRepository).findById(1L);
    }

    @Test
    void getEventByIdWithNonexistentId() {
        Long nonexistentEventId = 999L;
        when(eventRepository.findById(nonexistentEventId)).thenReturn(Optional.empty());

        Optional<Event> result = eventService.getEventById(nonexistentEventId);

        assertFalse(result.isPresent());
        verify(eventRepository).findById(nonexistentEventId);
    }

    @Test
    void getAllEvents() {
        when(eventRepository.findAll()).thenReturn(Arrays.asList(event));
        List<Event> events = eventService.getAllEvents();

        assertFalse(events.isEmpty());
        assertEquals(1, events.size());
        verify(eventRepository).findAll();
    }

    @Test
    void getAllEventsWhenNoneExist() {
        when(eventRepository.findAll()).thenReturn(Collections.emptyList());
        List<Event> events = eventService.getAllEvents();

        assertTrue(events.isEmpty());
        verify(eventRepository).findAll();
    }

    @Test
    void updateEvent() {
        Event updatedEventInfo = new Event();
        updatedEventInfo.setName("Updated Event Name");
        updatedEventInfo.setDescription("Updated description");
        updatedEventInfo.setStartDate(new Date());
        updatedEventInfo.setEndDate(new Date());

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event updatedEvent = eventService.updateEvent(1L, updatedEventInfo);

        assertNotNull(updatedEvent);
        assertEquals("Updated Event Name", updatedEvent.getName());
        assertEquals("Updated description", updatedEvent.getDescription());
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    void updateNonexistentEvent() {
        Event updatedEventInfo = new Event();
        updatedEventInfo.setStartDate(new Date());
        updatedEventInfo.setEndDate(new Date());

        lenient().when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            eventService.updateEvent(999L, updatedEventInfo);
        });
        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void deleteEvent() {
        doNothing().when(eventRepository).deleteById(1L);

        eventService.deleteEvent(1L);

        verify(eventRepository).deleteById(1L);
    }

    @Test
    void deleteNonexistentEvent() {
        doThrow(new RuntimeException("Event not found.")).when(eventRepository).deleteById(999L);

        Exception exception = assertThrows(RuntimeException.class, () -> {
            eventService.deleteEvent(999L);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository).deleteById(999L);
    }

    @Test
    void addParticipant() {
        User participant = new User();
        participant.setId(2L);
        Set<User> initialParticipants = new HashSet<>();

        event.setParticipants(initialParticipants);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(eventRepository.save(event)).thenAnswer(invocation -> invocation.getArgument(0));

        Event updatedEvent = eventService.addParticipant(1L, 2L);

        assertNotNull(updatedEvent);
        assertTrue(updatedEvent.getParticipants().contains(participant));
        assertEquals(1, updatedEvent.getParticipants().size());
        verify(eventRepository).save(event);
    }

    @Test
    void addParticipantToNonexistentEvent() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            eventService.addParticipant(999L, 2L);
        });
    }

    @Test
    void removeParticipant() {
        User participant = new User();
        participant.setId(2L);
        Set<User> participants = new HashSet<>();
        participants.add(participant);

        event.setParticipants(participants);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(userRepository.findById(2L)).thenReturn(Optional.of(participant));
        when(eventRepository.save(event)).thenAnswer(invocation -> invocation.getArgument(0));

        Event updatedEvent = eventService.removeParticipant(1L, 2L);

        assertNotNull(updatedEvent);
        assertFalse(updatedEvent.getParticipants().contains(participant));
        assertEquals(0, updatedEvent.getParticipants().size());
        verify(eventRepository).save(event);
    }

    @Test
    void removeParticipantFromNonexistentEvent() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            eventService.removeParticipant(999L, 2L);
        });
    }

    @Test
    void getParticipants() {
        User participant = new User();
        participant.setId(2L);
        Set<User> participants = new HashSet<>();
        participants.add(participant);

        event.setParticipants(participants);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Set<User> fetchedParticipants = eventService.getParticipants(1L);

        assertNotNull(fetchedParticipants);
        assertFalse(fetchedParticipants.isEmpty());
        assertEquals(1, fetchedParticipants.size());
        assertTrue(fetchedParticipants.contains(participant));
        verify(eventRepository).findById(1L);
    }

    @Test
    void getParticipantsForNonexistentEvent() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        Exception exception = assertThrows(RuntimeException.class, () -> {
            eventService.getParticipants(999L);
        });

        assertTrue(exception.getMessage().contains("Event not found"));
        verify(eventRepository).findById(999L);
    }

    @Test
    void createEventWithNullStartDate() {
        event.setStartDate(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            eventService.createEvent(event);
        });

        assertEquals("Event dates cannot be null", exception.getMessage());
    }

    @Test
    void createEventWithNullEndDate() {
        event.setEndDate(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            eventService.createEvent(event);
        });

        assertEquals("Event dates cannot be null", exception.getMessage());
    }

    @Test
    void createEventWithStartDateAfterEndDate() {
        Date now = new Date();
        Date later = new Date(now.getTime() + 10000);
        event.setStartDate(later);
        event.setEndDate(now);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            eventService.createEvent(event);
        });

        assertEquals("Start date cannot be after end date", exception.getMessage());
    }
}
