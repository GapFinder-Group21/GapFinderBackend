package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.enums.OpenTableStatusEnum;
import com.backend.gapfinder.model.exceptions.AlreadyDoneException;
import com.backend.gapfinder.model.models.OpenTableModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.OpenTableParticipantRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.List;

class OpenTableParticipantServiceStatusTest {

    private OpenTableParticipantRepository participants;
    private OpenTableService openTableService;
    private UserService userService;
    private OpenTableParticipantService service;

    private OpenTableModel table;

    @BeforeEach
    void setUp() {
        participants = mock(OpenTableParticipantRepository.class);
        openTableService = mock(OpenTableService.class);
        userService = mock(UserService.class);
        service = new OpenTableParticipantService(participants, userService, openTableService,
                mock(ApplicationEventPublisher.class));

        table = new OpenTableModel();
        table.setId(10L);
        table.setCreator(user(1L));
        table.setStatus(OpenTableStatusEnum.OPEN);
        table.setMaxParticipants(2);
        table.setEndTime(LocalDateTime.now().plusHours(1));

        when(openTableService.getByIdForUpdate(10L)).thenReturn(table);
        when(userService.getById(2L)).thenReturn(user(2L));
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    @Test
    void joiningATableYouAlreadyJoinedIsAlreadyDone() {
        when(participants.existsByOpenTableIdAndUserId(10L, 2L)).thenReturn(true);

        assertThrows(AlreadyDoneException.class, () -> service.join(10L, 2L));
    }

    @Test
    void aFullTableIsARefusalNotAlreadyDone() {
        when(participants.countByOpenTableId(10L)).thenReturn(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.join(10L, 2L));

        // A refusal is a plain IllegalStateException (answers 422); AlreadyDoneException is a different type (409)
        assertEquals(IllegalStateException.class, ex.getClass());
        assertEquals("La open table está llena", ex.getMessage());
    }

    @Test
    void aFinishedTableIsARefusalNotAlreadyDone() {
        table.setEndTime(LocalDateTime.now().minusMinutes(5));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.join(10L, 2L));

        assertEquals(IllegalStateException.class, ex.getClass());
    }

    @Test
    void joinedTablesComeFromOneQuery() {
        OpenTableModel other = new OpenTableModel();
        other.setId(11L);
        when(participants.findJoinedOpenTables(2L)).thenReturn(List.of(table, other));

        List<OpenTableModel> joined = service.getJoinedOpenTables(2L);

        assertEquals(2, joined.size());
        verify(participants).findJoinedOpenTables(2L);
    }
}
