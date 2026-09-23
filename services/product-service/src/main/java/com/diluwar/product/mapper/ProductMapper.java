package com.diluwar.product.mapper;

import com.diluwar.product.dto.ProductRequest;
import com.diluwar.product.dto.ProductResponse;
import com.diluwar.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request) {

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());

        return product;
    }

    public ProductResponse toResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }

    public void updateEntity(Product product, ProductRequest request) {

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
    }
}