package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.controller.controllers.UserController;
import com.backend.gapfinder.model.exceptions.ForbiddenException;
import com.backend.gapfinder.model.models.GapModel;
import com.backend.gapfinder.model.models.InterestModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.UserRepository;
import com.backend.gapfinder.model.strategies.SharedInterestStrategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class UserServiceRemoveInterestTest {

    private UserRepository userRepository;
    private UserService service;
    private UserModel user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new UserService(userRepository, mock(InterestService.class), mock(BuildingService.class));

        user = user(1L, interest(1L, "Music"), interest(2L, "Sports"));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserModel.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static InterestModel interest(Long id, String name) {
        InterestModel interest = new InterestModel();
        interest.setId(id);
        interest.setName(name);
        return interest;
    }

    private static UserModel user(Long id, InterestModel... interests) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setInterests(new ArrayList<>(List.of(interests)));
        return user;
    }

    private static GapModel gap(UserModel owner) {
        GapModel gap = new GapModel();
        gap.setUser(owner);
        return gap;
    }

    @Test
    void removeInterestDeletesOnlyThatInterest() {
        service.removeInterest(1L, 1L);

        assertEquals(1, user.getInterests().size());
        assertEquals(2L, user.getInterests().get(0).getId());
        verify(userRepository).save(user);
    }

    @Test
    void removeInterestTheUserDoesNotHaveChangesNothing() {
        service.removeInterest(1L, 99L);

        assertEquals(2, user.getInterests().size());
        verify(userRepository, never()).save(any(UserModel.class));
    }

    @Test
    void removeInterestWhenUserHasNoInterestListDoesNotFail() {
        user.setInterests(null);

        service.removeInterest(1L, 1L);

        verify(userRepository, never()).save(any(UserModel.class));
    }

    @Test
    void matchingNoLongerCountsTheRemovedInterest() {
        UserModel other = user(2L, interest(1L, "Music"), interest(2L, "Sports"));
        SharedInterestStrategy strategy = new SharedInterestStrategy();

        assertEquals(20.0, strategy.calculateScore(gap(user), gap(other)));

        service.removeInterest(1L, 1L);

        assertEquals(10.0, strategy.calculateScore(gap(user), gap(other)));
    }

    @Test
    void controllerRejectsAnotherUserChangingMyInterests() {
        UserService userService = mock(UserService.class);
        UserController controller = new UserController(userService, mock(ModelMapper.class));
        UserModel stranger = user(3L);

        assertThrows(ForbiddenException.class, () -> controller.removeInterest(1L, 1L, stranger));
        verify(userService, never()).removeInterest(any(), any());
    }

    @Test
    void controllerRemovesMyOwnInterest() {
        UserService userService = mock(UserService.class);
        UserController controller = new UserController(userService, mock(ModelMapper.class));

        controller.removeInterest(1L, 1L, user);

        verify(userService).removeInterest(1L, 1L);
    }
}
