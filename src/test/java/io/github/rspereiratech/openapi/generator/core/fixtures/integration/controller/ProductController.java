package io.github.rspereiratech.openapi.generator.core.fixtures.integration.controller;

import io.github.rspereiratech.openapi.generator.core.fixtures.integration.dto.ProductDto;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    @GetMapping
    public List<String> listProducts() { return List.of(); }

    @PostMapping
    public ProductDto createProduct(@RequestBody ProductDto product) { return product; }
}
