package com.example.chitchat.controllers;

import com.example.chitchat.dto.CreateUserRequest;
import com.example.chitchat.entity.UserEntity;
import com.example.chitchat.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CreateNewUserController {

    private final UserService userService;

    public CreateNewUserController( UserService userService ){
        this.userService = userService;
    }

    @PostMapping("/user/create")
    public UserEntity createUser(@RequestBody CreateUserRequest req){
        return userService.createUser(req);
    }

}
