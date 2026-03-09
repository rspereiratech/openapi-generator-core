package io.github.rspereiratech.openapi.generator.core.fixtures.integration.controller;

import io.github.rspereiratech.openapi.generator.core.fixtures.integration.dto.CreateUserRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping
    public List<String> listUsers() { return List.of(); }

    @PostMapping
    public String createUser(@RequestBody CreateUserRequest request) { return ""; }
}
