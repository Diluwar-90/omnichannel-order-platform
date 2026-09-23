package com.diluwar.product.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor 
public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stockQuantity;
}
