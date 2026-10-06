package com.example.ecommerce.controller;

import com.example.ecommerce.model.Cart;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartController(
            CartRepository cartRepository,
            ProductRepository productRepository) {

        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }


    // =========================
    // ADD TO CART
    // =========================

    @PostMapping
    public Cart addToCart(
            @RequestBody Cart cart) {

        Product product =
                productRepository
                        .findById(cart.getProductId())
                        .orElse(null);


        if (product == null) {

            throw new RuntimeException(
                    "Product not found"
            );
        }


        if (cart.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than 0"
            );
        }


        if (product.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Product is out of stock"
            );
        }


        Cart existingCart =
                cartRepository
                        .findByUserIdAndProductId(
                                cart.getUserId(),
                                cart.getProductId()
                        )
                        .orElse(null);


        // =========================
        // EXISTING CART ITEM
        // =========================

        if (existingCart != null) {

            int newQuantity =
                    existingCart.getQuantity()
                            + cart.getQuantity();


            if (newQuantity > product.getQuantity()) {

                throw new RuntimeException(
                        "Only " +
                                product.getQuantity() +
                                " items available in stock"
                );
            }


            existingCart.setQuantity(
                    newQuantity
            );


            return cartRepository.save(
                    existingCart
            );
        }


        // =========================
        // NEW CART ITEM
        // =========================

        if (cart.getQuantity() >
                product.getQuantity()) {

            throw new RuntimeException(
                    "Only " +
                            product.getQuantity() +
                            " items available in stock"
            );
        }


        return cartRepository.save(cart);
    }


    // =========================
    // GET USER CART
    // =========================

    @GetMapping("/{userId}")
    public List<Cart> getUserCart(
            @PathVariable Long userId) {

        List<Cart> items =
                cartRepository.findByUserId(
                        userId
                );


        for (Cart item : items) {

            Product product =
                    productRepository
                            .findById(
                                    item.getProductId()
                            )
                            .orElse(null);


            item.setProduct(product);
        }


        return items;
    }


    // =========================
    // REMOVE SINGLE ITEM
    // =========================

    @DeleteMapping("/{id}")
    public String removeFromCart(
            @PathVariable Long id) {

        if (!cartRepository.existsById(id)) {

            return "Cart item not found";
        }


        cartRepository.deleteById(id);


        return "Product removed from cart successfully";
    }


    // =========================
    // CLEAR CART
    // =========================

    @DeleteMapping("/user/{userId}")
    public String clearUserCart(
            @PathVariable Long userId) {

        List<Cart> cartItems =
                cartRepository.findByUserId(
                        userId
                );


        cartRepository.deleteAll(
                cartItems
        );


        return "Cart cleared successfully";
    }


    // =========================
    // UPDATE QUANTITY
    // =========================

    @PutMapping("/{id}")
    public Cart updateCartQuantity(
            @PathVariable Long id,
            @RequestBody Cart cart) {

        Cart existingCart =
                cartRepository
                        .findById(id)
                        .orElse(null);


        if (existingCart == null) {
            return null;
        }


        Product product =
                productRepository
                        .findById(
                                existingCart.getProductId()
                        )
                        .orElse(null);


        if (product == null) {

            throw new RuntimeException(
                    "Product not found"
            );
        }


        if (cart.getQuantity() <= 0) {

            throw new RuntimeException(
                    "Quantity must be greater than 0"
            );
        }


        if (cart.getQuantity() >
                product.getQuantity()) {

            throw new RuntimeException(
                    "Only " +
                            product.getQuantity() +
                            " items available in stock"
            );
        }


        existingCart.setQuantity(
                cart.getQuantity()
        );


        return cartRepository.save(
                existingCart
        );
    }
}