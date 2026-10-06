package com.example.ecommerce.controller;

import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository productRepository;

    // Product images yahan save hongi
    private final Path uploadDir =
            Paths.get("uploads", "products").toAbsolutePath().normalize();

    public ProductController(ProductRepository productRepository) {

        this.productRepository = productRepository;

        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException(
                    "Could not create upload directory: "
                            + e.getMessage(),
                    e
            );
        }
    }


    // ============================================================
    // ADD PRODUCT WITH IMAGE
    // ============================================================

    @PostMapping("/admin")
    public ResponseEntity<?> createProduct(

            @RequestParam("name")
            String name,

            @RequestParam("description")
            String description,

            @RequestParam("price")
            double price,

            @RequestParam("quantity")
            int quantity,

            @RequestParam("category")
            String category,

            @RequestParam(value = "image", required = false)
            MultipartFile image

    ) {

        try {

            // Basic validation
            if (name == null || name.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Product name is required."));
            }

            if (price < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Price cannot be negative."));
            }

            if (quantity < 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Quantity cannot be negative."));
            }


            // ====================================================
            // CREATE PRODUCT
            // ====================================================

            Product product = new Product();

            product.setName(name.trim());

            product.setDescription(
                    description == null
                            ? ""
                            : description.trim()
            );

            product.setPrice(price);

            product.setQuantity(quantity);

            product.setCategory(
                    category == null
                            ? ""
                            : category.trim()
            );


            // ====================================================
            // SAVE IMAGE
            // ====================================================

            if (image != null && !image.isEmpty()) {

                String originalName =
                        image.getOriginalFilename();

                if (originalName == null ||
                        originalName.trim().isEmpty()) {

                    originalName = "product-image";
                }


                // Sirf extension nikalna
                String extension = "";

                int dotIndex =
                        originalName.lastIndexOf(".");

                if (dotIndex >= 0) {
                    extension =
                            originalName.substring(dotIndex)
                                    .toLowerCase();
                }


                // Safe unique filename
                String fileName =
                        UUID.randomUUID()
                                + extension;


                Path filePath =
                        uploadDir.resolve(fileName)
                                .normalize();


                // Security check
                if (!filePath.startsWith(uploadDir)) {

                    return ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body(Map.of(
                                    "error",
                                    "Invalid image file."
                            ));
                }


                // Save image
                Files.copy(
                        image.getInputStream(),
                        filePath,
                        StandardCopyOption.REPLACE_EXISTING
                );


                // DB mein image URL
                product.setImageUrl(
                        "/uploads/products/" + fileName
                );
            }


            // ====================================================
            // SAVE PRODUCT IN MYSQL
            // ====================================================

            Product savedProduct =
                    productRepository.save(product);


            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(savedProduct);


        } catch (Exception e) {

            // IMPORTANT:
            // Ab generic 500 ke bajay actual error frontend ko milega.

            e.printStackTrace();

            Map<String, Object> error =
                    new HashMap<>();

            error.put(
                    "error",
                    "Product could not be saved."
            );

            error.put(
                    "message",
                    e.getMessage()
            );

            error.put(
                    "exception",
                    e.getClass().getSimpleName()
            );


            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(error);
        }
    }


    // ============================================================
    // GET ALL PRODUCTS
    // ============================================================

    @GetMapping
    public List<Product> getAllProducts() {

        return productRepository.findAll();
    }


    // ============================================================
    // GET PRODUCT BY ID
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(
            @PathVariable Long id) {

        return productRepository
                .findById(id)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(null)
                );
    }


    // ============================================================
    // DELETE PRODUCT
    // ============================================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Long id) {

        if (!productRepository.existsById(id)) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error",
                            "Product not found."
                    ));
        }

        productRepository.deleteById(id);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Product deleted successfully"
                )
        );
    }


    // ============================================================
    // UPDATE PRODUCT
    // ============================================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable Long id,
            @RequestBody Product product) {

        Product existingProduct =
                productRepository
                        .findById(id)
                        .orElse(null);


        if (existingProduct == null) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "error",
                            "Product not found."
                    ));
        }


        existingProduct.setName(
                product.getName()
        );

        existingProduct.setDescription(
                product.getDescription()
        );

        existingProduct.setPrice(
                product.getPrice()
        );

        existingProduct.setQuantity(
                product.getQuantity()
        );

        existingProduct.setCategory(
                product.getCategory()
        );


        // Old image preserve hogi
        if (product.getImageUrl() != null
                && !product.getImageUrl().isBlank()) {

            existingProduct.setImageUrl(
                    product.getImageUrl()
            );
        }


        return ResponseEntity.ok(
                productRepository.save(
                        existingProduct
                )
        );
    }

    // ============================================================
    // UPDATE PRODUCT IMAGE
    // ============================================================

    @PutMapping("/{id}/image")
    public ResponseEntity<?> updateProductImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image) {

        try {

            Product product =
                    productRepository.findById(id).orElse(null);

            if (product == null) {

                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "error",
                                "Product not found."
                        ));
            }

            if (image == null || image.isEmpty()) {

                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error",
                                "Please select an image."
                        ));
            }


            // Original filename
            String originalName =
                    image.getOriginalFilename();

            String extension = "";

            if (originalName != null) {

                int dotIndex =
                        originalName.lastIndexOf(".");

                if (dotIndex >= 0) {

                    extension =
                            originalName
                                    .substring(dotIndex)
                                    .toLowerCase();
                }
            }


            // Unique filename
            String fileName =
                    UUID.randomUUID() + extension;


            Path filePath =
                    uploadDir
                            .resolve(fileName)
                            .normalize();


            // Save new image
            Files.copy(
                    image.getInputStream(),
                    filePath,
                    StandardCopyOption.REPLACE_EXISTING
            );


            // Update DB
            product.setImageUrl(
                    "/uploads/products/" + fileName
            );


            Product savedProduct =
                    productRepository.save(product);


            return ResponseEntity.ok(savedProduct);


        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error",
                            "Unable to update product image.",
                            "message",
                            e.getMessage()
                    ));
        }
    }
}