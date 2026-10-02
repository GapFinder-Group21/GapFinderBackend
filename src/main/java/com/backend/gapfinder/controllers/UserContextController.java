package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.UserBasicDTO;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.services.NearbyFriendsService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserContextController {

    private final NearbyFriendsService nearbyFriendsService;
    private final ModelMapper modelMapper;

    public UserContextController(NearbyFriendsService nearbyFriendsService, ModelMapper modelMapper) {
        this.nearbyFriendsService = nearbyFriendsService;
        this.modelMapper = modelMapper;
    }

    // Update the user's location from GPS coordinates and get friends in the same building
    // PATCH /users/{userId}/location/gps?latitude=4.6015&longitude=-74.0660
    @PatchMapping("/{userId}/location/gps")
    public List<UserBasicDTO> updateLocationByGps(
            @PathVariable Long userId,
            @RequestParam double latitude,
            @RequestParam double longitude) {

        List<UserModel> friends = nearbyFriendsService.updateLocationByCoordinates(userId, latitude, longitude);
        return modelMapper.map(friends, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Update the user's current building and get friends already there (useful for testing without GPS)
    // PATCH /users/{userId}/location?buildingId=3
    @PatchMapping("/{userId}/location")
    public List<UserBasicDTO> updateLocation(
            @PathVariable Long userId,
            @RequestParam Long buildingId) {

        List<UserModel> friends = nearbyFriendsService.updateLocationAndGetNearbyFriends(userId, buildingId);
        return modelMapper.map(friends, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Get friends currently in the same building as the user
    // GET /users/{userId}/friends/nearby
    @GetMapping("/{userId}/friends/nearby")
    public List<UserBasicDTO> getNearbyFriends(@PathVariable Long userId) {
        List<UserModel> friends = nearbyFriendsService.getNearbyFriends(userId);
        return modelMapper.map(friends, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }
}