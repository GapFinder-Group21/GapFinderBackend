package com.backend.gapfinder.controller.controllers;

import com.backend.gapfinder.view.dto.FriendshipBasicDTO;
import com.backend.gapfinder.view.dto.UserBasicDTO;
import com.backend.gapfinder.model.models.FriendshipModel;
import com.backend.gapfinder.model.models.UserModel;
import com.backend.gapfinder.model.services.FriendshipService;
import com.backend.gapfinder.security.AccessGuard;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friendships")
public class FriendshipController {

    private final FriendshipService friendshipService;
    private final ModelMapper modelMapper;

    public FriendshipController(FriendshipService friendshipService, ModelMapper modelMapper) {
        this.friendshipService = friendshipService;
        this.modelMapper = modelMapper;
    }

    // Get a friendship by its id
    // GET /friendships/{id}
    @GetMapping("/{id}")
    public FriendshipBasicDTO getFriendship(@PathVariable Long id) {
        FriendshipModel friendship = friendshipService.getById(id);
        return modelMapper.map(friendship, FriendshipBasicDTO.class);
    }

    // Get all friendships
    // GET /friendships
    @GetMapping
    public List<FriendshipBasicDTO> getAll() {
        List<FriendshipModel> friendships = friendshipService.getAll();
        return modelMapper.map(friendships, new TypeToken<List<FriendshipBasicDTO>>() {}.getType());
    }

    // Get all accepted friends from a user
    // GET /friendships/user/{userId}/friends
    @GetMapping("/user/{userId}/friends")
    public List<UserBasicDTO> getFriendsByUser(@PathVariable Long userId) {
        List<UserModel> friends = friendshipService.getFriendsByUser(userId);
        return modelMapper.map(friends, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Create a new friendship
    // POST /friendships
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FriendshipBasicDTO createFriendship(@RequestBody FriendshipBasicDTO dto,
                                               @AuthenticationPrincipal UserModel me) {
        // A user can only send friend requests in their own name
        if (dto.getRequesterId() != null) {
            AccessGuard.requireSameUser(me, dto.getRequesterId());
        }
        FriendshipModel friendshipModel = modelMapper.map(dto, FriendshipModel.class);
        FriendshipModel created = friendshipService.create(friendshipModel);
        return modelMapper.map(created, FriendshipBasicDTO.class);
    }

    // Update an existing friendship
    // PUT /friendships/{id}
    @PutMapping("/{id}")
    public FriendshipBasicDTO updateFriendship(@PathVariable Long id, @RequestBody FriendshipBasicDTO dto,
                                               @AuthenticationPrincipal UserModel me) {
        requireMember(friendshipService.getById(id), me);
        FriendshipModel friendshipModel = modelMapper.map(dto, FriendshipModel.class);
        FriendshipModel updated = friendshipService.update(id, friendshipModel);
        return modelMapper.map(updated, FriendshipBasicDTO.class);
    }

    // Delete a friendship
    // DELETE /friendships/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFriendship(@PathVariable Long id, @AuthenticationPrincipal UserModel me) {
        requireMember(friendshipService.getById(id), me);
        friendshipService.delete(id);
    }

        // Accept a pending friend request
    // PATCH /friendships/57/accept?userId=400
    @PatchMapping("/{id}/accept")
    public FriendshipBasicDTO acceptFriendship(
            @PathVariable Long id,
            @RequestParam Long userId,
            @AuthenticationPrincipal UserModel me) {

        // A user can only answer requests in their own name
        AccessGuard.requireSameUser(me, userId);
        FriendshipModel friendship = friendshipService.acceptFriendship(id, userId);
        return modelMapper.map(friendship, FriendshipBasicDTO.class);
    }

    // Reject a pending friend request
    // PATCH /friendships/57/reject?userId=400
    @PatchMapping("/{id}/reject")
    public FriendshipBasicDTO rejectFriendship(
            @PathVariable Long id,
            @RequestParam Long userId,
            @AuthenticationPrincipal UserModel me) {

        // A user can only answer requests in their own name
        AccessGuard.requireSameUser(me, userId);
        FriendshipModel friendship = friendshipService.rejectFriendship(id, userId);
        return modelMapper.map(friendship, FriendshipBasicDTO.class);
    }

    // Only the two users of the friendship (requester or receiver) can change it
    private void requireMember(FriendshipModel friendship, UserModel me) {
        if (me == null || !me.getId().equals(friendship.getRequester().getId())) {
            AccessGuard.requireSameUser(me, friendship.getReceiver().getId());
        }
    }
}
