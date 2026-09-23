package com.diluwar.product.controller;

import com.diluwar.product.dto.ProductRequest;
import com.diluwar.product.dto.ProductResponse;
import com.diluwar.product.exception.ProductNotFoundException;
import com.diluwar.product.service.ProductService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;


    // =========================================================
    // GET /api/products/{id}
    // =========================================================

    @Test
    void getProduct_shouldReturn200() throws Exception {

        Long productId = 1L;

        ProductResponse response = new ProductResponse(
                productId,
                "iPhone 17 Pro",
                "Apple premium smartphone",
                99999.0,
                15
        );

        when(productService.getProductById(productId))
                .thenReturn(response);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("iPhone 17 Pro"))
                .andExpect(jsonPath("$.description")
                        .value("Apple premium smartphone"))
                .andExpect(jsonPath("$.price").value(99999.0))
                .andExpect(jsonPath("$.stockQuantity").value(15));
    }


    @Test
    void getProduct_whenProductDoesNotExist_shouldReturn404()
            throws Exception {

        Long productId = 999L;

        when(productService.getProductById(productId))
                .thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Product not found: " + productId));
    }


    // =========================================================
    // POST /api/products
    // =========================================================

    @Test
    void createProduct_shouldReturn201() throws Exception {

        ProductResponse response = new ProductResponse(
                2L,
                "MacBook Pro",
                "Apple laptop",
                199999.0,
                10
        );

        when(productService.createProduct(any(ProductRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "MacBook Pro",
                                          "description": "Apple laptop",
                                          "price": 199999.0,
                                          "stockQuantity": 10
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("MacBook Pro"))
                .andExpect(jsonPath("$.description")
                        .value("Apple laptop"))
                .andExpect(jsonPath("$.price").value(199999.0))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }


    @Test
    void createProduct_whenRequestIsInvalid_shouldReturn400()
            throws Exception {

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "",
                                          "description": "Invalid product",
                                          "price": -100,
                                          "stockQuantity": -5
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fields.name")
                        .value("Product name is required"))
                .andExpect(jsonPath("$.fields.price")
                        .value("Product price must be greater than 0"))
                .andExpect(jsonPath("$.fields.stockQuantity")
                        .value("Stock quantity cannot be negative"));
    }


    // =========================================================
    // PUT /api/products/{id}
    // =========================================================

    @Test
    void updateProduct_shouldReturn200() throws Exception {

        Long productId = 1L;

        ProductResponse response = new ProductResponse(
                productId,
                "iPhone 17 Pro Max",
                "Apple premium smartphone",
                109999.0,
                20
        );

        when(productService.updateProduct(
                any(Long.class),
                any(ProductRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/products/{id}", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "iPhone 17 Pro Max",
                                          "description": "Apple premium smartphone",
                                          "price": 109999.0,
                                          "stockQuantity": 20
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("iPhone 17 Pro Max"))
                .andExpect(jsonPath("$.description")
                        .value("Apple premium smartphone"))
                .andExpect(jsonPath("$.price").value(109999.0))
                .andExpect(jsonPath("$.stockQuantity").value(20));
    }


    @Test
    void updateProduct_whenProductDoesNotExist_shouldReturn404()
            throws Exception {

        Long productId = 999L;

        when(productService.updateProduct(
                any(Long.class),
                any(ProductRequest.class)
        )).thenThrow(new ProductNotFoundException(productId));

        mockMvc.perform(
                        put("/api/products/{id}", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "iPhone 17 Pro Max",
                                          "description": "Apple premium smartphone",
                                          "price": 109999.0,
                                          "stockQuantity": 20
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Product not found: " + productId));
    }


    @Test
    void updateProduct_whenRequestIsInvalid_shouldReturn400()
            throws Exception {

        Long productId = 1L;

        mockMvc.perform(
                        put("/api/products/{id}", productId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "name": "",
                                          "description": "Invalid product",
                                          "price": -100,
                                          "stockQuantity": -5
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message")
                        .value("Request validation failed"))
                .andExpect(jsonPath("$.fields.name")
                        .value("Product name is required"))
                .andExpect(jsonPath("$.fields.price")
                        .value("Product price must be greater than 0"))
                .andExpect(jsonPath("$.fields.stockQuantity")
                        .value("Stock quantity cannot be negative"));
    }


    // =========================================================
    // DELETE /api/products/{id}
    // =========================================================

    @Test
    void deleteProduct_shouldReturn204() throws Exception {

        Long productId = 1L;

        doNothing()
                .when(productService)
                .deleteProduct(productId);

        mockMvc.perform(
                        delete("/api/products/{id}", productId)
                )
                .andExpect(status().isNoContent());
    }


    @Test
    void deleteProduct_whenProductDoesNotExist_shouldReturn404()
            throws Exception {

        Long productId = 999L;

        doThrow(new ProductNotFoundException(productId))
                .when(productService)
                .deleteProduct(productId);

        mockMvc.perform(
                        delete("/api/products/{id}", productId)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value("Product not found: " + productId));
    }
}