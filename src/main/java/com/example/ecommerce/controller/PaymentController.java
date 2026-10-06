package com.example.ecommerce.controller;

import com.example.ecommerce.model.Payment;
import com.example.ecommerce.repository.PaymentRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentRepository paymentRepository;

    public PaymentController(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    // Create COD Payment
    @PostMapping
    public Payment createPayment(@RequestBody Payment payment) {

        payment.setPaymentMethod("COD");
        payment.setPaymentStatus("PENDING");

        return paymentRepository.save(payment);
    }

    // Get payment by Order ID
    @GetMapping("/order/{orderId}")
    public Payment getPaymentByOrderId(@PathVariable Long orderId) {

        return paymentRepository.findByOrderId(orderId).orElse(null);
    }
}