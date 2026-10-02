package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.UserBasicDTO;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.services.NearbyFriendsService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
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
            @RequestParam double longitude) {

        List<UserModel> nearby = nearbyFriendsService.updateLocationByCoordinates(userId, latitude, longitude);
        return modelMapper.map(nearby, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Update the user's current building directly and get friends nearby
    // PUT /nearby-friends/user/{userId}/building/{buildingId}
    @PutMapping("/user/{userId}/building/{buildingId}")
    public List<UserBasicDTO> updateLocationByBuilding(
            @PathVariable Long userId,
            @PathVariable Long buildingId) {

        List<UserModel> nearby = nearbyFriendsService.updateLocationAndGetNearbyFriends(userId, buildingId);
        return modelMapper.map(nearby, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Get the accepted friends currently in the user's current building
    // GET /nearby-friends/user/{userId}
    @GetMapping("/user/{userId}")
    public List<UserBasicDTO> getNearbyFriends(@PathVariable Long userId) {
        List<UserModel> nearby = nearbyFriendsService.getNearbyFriends(userId);
        return modelMapper.map(nearby, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

}