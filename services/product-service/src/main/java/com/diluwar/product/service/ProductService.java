package com.diluwar.product.service;

import com.diluwar.product.dto.ProductRequest;
import com.diluwar.product.dto.ProductResponse;
import com.diluwar.product.entity.Product;
import com.diluwar.product.exception.ProductNotFoundException;
import com.diluwar.product.mapper.ProductMapper;
import com.diluwar.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductService {

    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private ProductMapper productMapper;

    public ProductResponse createProduct(ProductRequest request) {
        Product product = request != null ? productMapper.toEntity(request) : new Product();
        Product savedProduct = productRepository.save(product);
        return productMapper.toResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
        .stream()
        .map(productMapper::toResponse)
        .collect(java.util.stream.Collectors.toList());
    }

    public ProductResponse getProductById(Long id) {
        return productMapper.toResponse(productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id)));
        }   

    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product existingProduct = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        productMapper.updateEntity(existingProduct, request);

        existingProduct.setStockQuantity(request.getStockQuantity());

        Product updatedProduct = productRepository.save(existingProduct);

        return productMapper.toResponse(updatedProduct);
    }

    public void deleteProduct(Long id) {

        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);       
    }

}