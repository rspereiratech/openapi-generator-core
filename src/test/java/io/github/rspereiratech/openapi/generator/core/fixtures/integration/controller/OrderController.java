package io.github.rspereiratech.openapi.generator.core.fixtures.integration.controller;

import io.github.rspereiratech.openapi.generator.core.fixtures.integration.dto.OrderDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    @GetMapping
    public List<String> listOrders() { return List.of(); }

    @PostMapping
    public OrderDto createOrder(@RequestBody OrderDto order) { return order; }
}
