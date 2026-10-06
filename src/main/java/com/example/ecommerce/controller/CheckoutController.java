package com.example.ecommerce.controller;

import com.example.ecommerce.model.CheckoutRequest;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.service.CheckoutService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping
    public Order checkout(@RequestBody CheckoutRequest request) {
        return checkoutService.checkout(request);
    }
}