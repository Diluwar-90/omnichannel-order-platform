package com.diluwar.product.controller;

import com.diluwar.product.dto.ProductRequest;
import com.diluwar.product.dto.ProductResponse;
import com.diluwar.product.service.ProductService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/products")    
public class ProductController {

    @Autowired
    private ProductService productService;

    @PostMapping
    @Transactional 
    public ProductResponse createProduct(@Valid  @RequestBody ProductRequest request) {
        return productService.createProduct(request);
    }

    @GetMapping
    public List<ProductResponse> getAllProducts() {
        return productService.getAllProducts();
}

    @GetMapping("/{id}")
    public ProductResponse getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    @Transactional
    public ProductResponse updateProduct(
        @PathVariable Long id,
        @Valid @RequestBody ProductRequest request) {

        return productService.updateProduct(id, request);
    }

    @Transactional     
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
    }
}