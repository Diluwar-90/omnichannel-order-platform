package com.diluwar.product.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.diluwar.product.dto.ProductRequest;
import com.diluwar.product.dto.ProductResponse;
import com.diluwar.product.entity.Product;
import com.diluwar.product.exception.ProductNotFoundException;
import com.diluwar.product.mapper.ProductMapper;
import com.diluwar.product.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock 
    private ProductRepository productRepository;

    @Mock 
    private ProductMapper productMapper;

    @InjectMocks 
    private ProductService productService;

    @Test
    void getProductById_shouldReturnProduct_whenProductExists() {
    // Arrange
    Long productId = 1L;

    Product product = new Product();
    product.setId(productId);
    product.setName("iPhone 17 Pro");
    product.setDescription("Apple premium smartphone");
    product.setPrice(99999.0);
    product.setStockQuantity(15);

    ProductResponse response = new ProductResponse(
            productId,
            "iPhone 17 Pro",
            "Apple premium smartphone",
            99999.0,
            15
    );

    when(productRepository.findById(productId))
            .thenReturn(Optional.of(product));

    when(productMapper.toResponse(product))
            .thenReturn(response);

    // Act
    ProductResponse result = productService.getProductById(productId);

    // Assert
    assertEquals(productId, result.getId());
    assertEquals("iPhone 17 Pro", result.getName());
    assertEquals(99999.0, result.getPrice());

    verify(productRepository).findById(productId);
    verify(productMapper).toResponse(product);
    }

    @Test
void getProductById_shouldThrowException_whenProductDoesNotExist() {
    // Arrange
    Long productId = 999L;

    when(productRepository.findById(productId))
            .thenReturn(Optional.empty());

    // Act & Assert
    ProductNotFoundException exception = assertThrows(
            ProductNotFoundException.class,
            () -> productService.getProductById(productId)
    );

    assertEquals("Product not found: 999", exception.getMessage());

    verify(productRepository).findById(productId);
    verifyNoInteractions(productMapper);
}

@Test
void createProduct_shouldSaveAndReturnProduct() {
    // Arrange
   
    ProductRequest request = new ProductRequest(
            "MacBook Pro",
            "Apple laptop",
            199999.0,
            10
    );

    Product product = new Product();
    product.setName("MacBook Pro");
    product.setDescription("Apple laptop");
    product.setPrice(199999.0);
    product.setStockQuantity(10);

    Product savedProduct = new Product();
    savedProduct.setId(2L);
    savedProduct.setName("MacBook Pro");
    savedProduct.setDescription("Apple laptop");
    savedProduct.setPrice(199999.0);
    savedProduct.setStockQuantity(10);

    ProductResponse response = new ProductResponse(
            2L,
            "MacBook Pro",
            "Apple laptop",
            199999.0,
            10
    );

    when(productMapper.toEntity(request))
            .thenReturn(product);

    when(productRepository.save(product))
            .thenReturn(savedProduct);

    when(productMapper.toResponse(savedProduct))
            .thenReturn(response);

    // Act
    ProductResponse result = productService.createProduct(request);

    // Assert
    assertEquals(2L, result.getId());
    assertEquals("MacBook Pro", result.getName());
    assertEquals(199999.0, result.getPrice());

    verify(productMapper).toEntity(request);
    verify(productRepository).save(product);
    verify(productMapper).toResponse(savedProduct);
}

@Test
void getAllProducts_shouldReturnAllProducts() {
    // Arrange
    Product product1 = new Product();
    product1.setId(1L);
    product1.setName("iPhone 17 Pro");
    product1.setPrice(99999.0);
    product1.setStockQuantity(15);

    Product product2 = new Product();
    product2.setId(2L);
    product2.setName("MacBook Pro");
    product2.setPrice(199999.0);
    product2.setStockQuantity(10);

    ProductResponse response1 = new ProductResponse(
            1L, "iPhone 17 Pro", "Apple premium smartphone", 99999.0, 15
    );

    ProductResponse response2 = new ProductResponse(
            2L, "MacBook Pro", "Apple laptop", 199999.0, 10
    );

    when(productRepository.findAll())
            .thenReturn(List.of(product1, product2));

    when(productMapper.toResponse(product1))
            .thenReturn(response1);

    when(productMapper.toResponse(product2))
            .thenReturn(response2);

    // Act
    List<ProductResponse> result = productService.getAllProducts();

    // Assert
    assertEquals(2, result.size());
    assertEquals("iPhone 17 Pro", result.get(0).getName());
    assertEquals("MacBook Pro", result.get(1).getName());

    verify(productRepository).findAll();
    verify(productMapper).toResponse(product1);
    verify(productMapper).toResponse(product2);
}

@Test
void updateProduct_shouldUpdateAndReturnProduct() {
    // Arrange
    Long productId = 1L;

    ProductRequest request = new ProductRequest(
            "iPhone 17 Pro Max",
            "Apple flagship smartphone",
            119999.0,
            20
    );

    Product existingProduct = new Product();
    existingProduct.setId(productId);
    existingProduct.setName("iPhone 17 Pro");
    existingProduct.setDescription("Apple premium smartphone");
    existingProduct.setPrice(99999.0);
    existingProduct.setStockQuantity(15);

    ProductResponse response = new ProductResponse(
            productId,
            "iPhone 17 Pro Max",
            "Apple flagship smartphone",
            119999.0,
            20
    );

    when(productRepository.findById(productId))
            .thenReturn(Optional.of(existingProduct));

    when(productRepository.save(existingProduct))
            .thenReturn(existingProduct);

    when(productMapper.toResponse(existingProduct))
            .thenReturn(response);

    // Act
    ProductResponse result =
            productService.updateProduct(productId, request);

    // Assert
    assertEquals(productId, result.getId());
    assertEquals("iPhone 17 Pro Max", result.getName());
    assertEquals(119999.0, result.getPrice());
    assertEquals(20, result.getStockQuantity());

    verify(productRepository).findById(productId);
    verify(productRepository).save(existingProduct);
    verify(productMapper).toResponse(existingProduct);
}

@Test
void updateProduct_shouldThrowException_whenProductDoesNotExist() {
    // Arrange
    Long productId = 999L;

    ProductRequest request = new ProductRequest(
            "MacBook Pro",
            "Apple laptop",
            199999.0,
            10
    );

    when(productRepository.findById(productId))
            .thenReturn(Optional.empty());

    // Act & Assert
    ProductNotFoundException exception = assertThrows(
            ProductNotFoundException.class,
            () -> productService.updateProduct(productId, request)
    );

    assertEquals("Product not found: 999", exception.getMessage());

    verify(productRepository).findById(productId);
    verify(productRepository, never()).save(any());
    verifyNoInteractions(productMapper);
}

@Test
void deleteProduct_shouldDeleteProduct_whenProductExists() {
    // Arrange
    Long productId = 1L;

    when(productRepository.existsById(productId))
            .thenReturn(true);

    // Act
    productService.deleteProduct(productId);

    // Assert
    verify(productRepository).existsById(productId);
    verify(productRepository).deleteById(productId);
}

@Test
void deleteProduct_shouldThrowException_whenProductDoesNotExist() {
    // Arrange
    Long productId = 999L;

    when(productRepository.existsById(productId))
            .thenReturn(false);

    // Act & Assert
    ProductNotFoundException exception = assertThrows(
            ProductNotFoundException.class,
            () -> productService.deleteProduct(productId)
    );

    assertEquals("Product not found: 999", exception.getMessage());

    verify(productRepository).existsById(productId);
    verify(productRepository, never()).deleteById(productId);
}

}
