package io.github.rspereiratech.openapi.generator.core.fixtures.integration.controller;

import io.github.rspereiratech.openapi.generator.core.fixtures.integration.dto.SendNotificationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    @GetMapping
    public List<String> listNotifications() { return List.of(); }

    @PostMapping
    public String sendNotification(@RequestBody SendNotificationRequest request) { return ""; }
}
