package com.backend.gapfinder.model.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.gapfinder.model.models.BuildingModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.repositories.StudyPeriodRepository;
import com.backend.gapfinder.model.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

// The friends that are in study mode do not show as nearby
class NearbyFriendsServiceStudyModeTest {

    private UserService userService;
    private FriendshipService friendshipService;
    private StudyPeriodRepository studyPeriods;
    private NearbyFriendsService service;
    private BuildingModel building;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        friendshipService = mock(FriendshipService.class);
        studyPeriods = mock(StudyPeriodRepository.class);
        service = new NearbyFriendsService(userService, mock(UserRepository.class), mock(BuildingService.class),
                mock(UserLocationLogService.class), friendshipService, studyPeriods);

        building = new BuildingModel();
        building.setId(5L);
        when(userService.getById(1L)).thenReturn(friend(1L));
    }

    private UserModel friend(Long id) {
        UserModel user = new UserModel();
        user.setId(id);
        user.setCurrentBuilding(building);
        user.setLocationUpdatedAt(LocalDateTime.now());
        return user;
    }

    @Test
    void aFriendInStudyModeIsNotNearby() {
        when(friendshipService.getFriendsByUser(1L)).thenReturn(List.of(friend(2L), friend(3L)));
        when(studyPeriods.findUserIdsStudyingAt(any(), any(LocalDateTime.class))).thenReturn(List.of(3L));

        List<UserModel> nearby = service.getNearbyFriends(1L);

        assertEquals(1, nearby.size());
        assertEquals(2L, nearby.get(0).getId());
    }

    @Test
    void withoutStudyPeriodsAllTheFriendsInTheBuildingAreNearby() {
        when(friendshipService.getFriendsByUser(1L)).thenReturn(List.of(friend(2L), friend(3L)));
        when(studyPeriods.findUserIdsStudyingAt(any(), any(LocalDateTime.class))).thenReturn(List.of());

        assertEquals(2, service.getNearbyFriends(1L).size());
    }

    @Test
    void withNoFriendsInTheBuildingNoStudyQueryIsMade() {
        when(friendshipService.getFriendsByUser(1L)).thenReturn(List.of());

        assertEquals(0, service.getNearbyFriends(1L).size());
        verify(studyPeriods, never()).findUserIdsStudyingAt(any(), any());
    }
}
