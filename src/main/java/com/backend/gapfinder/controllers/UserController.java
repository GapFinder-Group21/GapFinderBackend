package com.backend.gapfinder.controllers;

import com.backend.gapfinder.dto.UserBasicDTO;
import com.backend.gapfinder.models.UserModel;
import com.backend.gapfinder.services.UserService;

import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final ModelMapper modelMapper;

    public UserController(UserService userService, ModelMapper modelMapper) {
        this.userService = userService;
        this.modelMapper = modelMapper;
    }

    // Get a user by its id
    // GET /users/{id}
    @GetMapping("/{id}")
    public UserBasicDTO getUser(@PathVariable Long id) {
        UserModel user = userService.getById(id);
        return modelMapper.map(user, UserBasicDTO.class);
    }

    // Get all users
    // GET /users
    @GetMapping
    public List<UserBasicDTO> getAll() {
        List<UserModel> users = userService.getAll();
        return modelMapper.map(users, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

    // Create a new user
    // POST /users
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserBasicDTO createUser(@RequestBody UserBasicDTO dto) {
        UserModel userModel = modelMapper.map(dto, UserModel.class);
        UserModel created = userService.create(userModel);
        return modelMapper.map(created, UserBasicDTO.class);
    }

    // Update an existing user
    // PUT /users/{id}
    @PutMapping("/{id}")
    public UserBasicDTO updateUser(@PathVariable Long id, @RequestBody UserBasicDTO dto) {
        UserModel userModel = modelMapper.map(dto, UserModel.class);
        UserModel updated = userService.update(id, userModel);
        return modelMapper.map(updated, UserBasicDTO.class);
    }

    // Delete a user
    // DELETE /users/{id}
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) {
        userService.delete(id);
    }

    // Add an interest to a user
    // POST /users/{userId}/interests/{interestId}
    @PostMapping("/{userId}/interests/{interestId}")
    public UserBasicDTO addInterest(@PathVariable Long userId, @PathVariable Long interestId) {
        UserModel updated = userService.addInterest(userId, interestId);
        return modelMapper.map(updated, UserBasicDTO.class);
    }

    // Search users by name
    // GET /users/search?name={name}
    @GetMapping("/search")
    public List<UserBasicDTO> searchByName(@RequestParam String name) {
        List<UserModel> users = userService.searchByName(name);
        return modelMapper.map(users, new TypeToken<List<UserBasicDTO>>() {}.getType());
    }

}