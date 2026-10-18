package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.UserBasicDTO;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.NearbyFriendsService;
import com.backend.gapfinder.security.AccessGuard;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/nearby-friends")
public class NearbyFriendsController {

    private final NearbyFriendsService nearbyFriendsService;
    private final ModelMapper modelMapper;

    public NearbyFriendsController(NearbyFriendsService nearbyFriendsService, ModelMapper modelMapper) {
        this.nearbyFriendsService = nearbyFriendsService;
        this.modelMapper = modelMapper;
    }

    // Update the user's location from GPS coordinates and get friends nearby
    // PUT /nearby-friends/user/{userId}/location?latitude=4.6015&longitude=-74.0661
    @PutMapping("/user/{userId}/location")
    public List<UserBasicDTO> updateLocationByCoordinates(
            @PathVariable Long userId,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @AuthenticationPrincipal UserModel me) {

        // A user can only update their own location
        AccessGuard.requireSameUser(me, userId);
        List<UserModel> nearby = nearbyFriendsService.updateLocationByCoordinates(userId, latitude, longitude);
        return modelMapper.map(nearby, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Update the user's current building directly and get friends nearby
    // PUT /nearby-friends/user/{userId}/building/{buildingId}
    @PutMapping("/user/{userId}/building/{buildingId}")
    public List<UserBasicDTO> updateLocationByBuilding(
            @PathVariable Long userId,
            @PathVariable Long buildingId,
            @AuthenticationPrincipal UserModel me) {

        // A user can only update their own location
        AccessGuard.requireSameUser(me, userId);
        List<UserModel> nearby = nearbyFriendsService.updateLocationAndGetNearbyFriends(userId, buildingId);
        return modelMapper.map(nearby, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Get the accepted friends currently in the user's current building
    // GET /nearby-friends/user/{userId}
    @GetMapping("/user/{userId}")
    public List<UserBasicDTO> getNearbyFriends(@PathVariable Long userId, @AuthenticationPrincipal UserModel me) {
        // Only the user can see which of their friends are nearby
        AccessGuard.requireSameUser(me, userId);
        List<UserModel> nearby = nearbyFriendsService.getNearbyFriends(userId);
        return modelMapper.map(nearby, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

}