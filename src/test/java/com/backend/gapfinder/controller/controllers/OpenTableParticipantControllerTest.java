package com.backend.gapfinder.controller.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.OpenTableModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.OpenTableParticipantService;
import com.backend.gapfinder.view.dto.OpenTableListDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.List;

class OpenTableParticipantControllerTest {

    private OpenTableParticipantService service;
    private ModelMapper modelMapper;
    private OpenTableParticipantController controller;
    private UserModel me;
    private UserModel stranger;

    @BeforeEach
    void setUp() {
        service = mock(OpenTableParticipantService.class);
        modelMapper = mock(ModelMapper.class);
        controller = new OpenTableParticipantController(service, modelMapper);
        me = user(2L);
        stranger = user(3L);
    }

    private static UserModel user(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        return user;
    }

    @Test
    void aUserSeesTheTablesTheyJoinedWithOneCall() {
        OpenTableModel table = new OpenTableModel();
        table.setId(10L);
        when(service.getJoinedOpenTables(2L)).thenReturn(List.of(table));
        when(modelMapper.map(any(), any(java.lang.reflect.Type.class))).thenReturn(List.of(new OpenTableListDTO()));

        List<OpenTableListDTO> result = controller.getJoinedOpenTables(2L, me);

        assertEquals(1, result.size());
        verify(service).getJoinedOpenTables(2L);
    }

    @Test
    void aUserCannotSeeTheTablesAnotherUserJoined() {
        assertThrows(ForbiddenException.class, () -> controller.getJoinedOpenTables(2L, stranger));

        verify(service, never()).getJoinedOpenTables(any());
    }
}
