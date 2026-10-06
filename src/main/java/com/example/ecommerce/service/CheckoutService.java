package com.example.ecommerce.service;

import com.example.ecommerce.model.*;
import com.example.ecommerce.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CheckoutService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final NotificationRepository notificationRepository;

    public CheckoutService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository,
            NotificationRepository notificationRepository) {

        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public Order checkout(CheckoutRequest request) {

        Long userId = request.getUserId();

        List<Cart> cartItems = cartRepository.findByUserId(userId);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double totalAmount = 0;

        // Check stock and calculate total
        for (Cart cart : cartItems) {

            Product product = productRepository
                    .findById(cart.getProductId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found"));

            if (product.getQuantity() < cart.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for " + product.getName());
            }

            totalAmount += product.getPrice() * cart.getQuantity();
        }

        // Create order
        Order order = new Order();

        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus("PLACED");
        order.setPaymentMethod("COD");
        order.setAddress(request.getAddress());

        Order savedOrder = orderRepository.save(order);

        // Create order items and update stock
        for (Cart cart : cartItems) {

            Product product = productRepository
                    .findById(cart.getProductId())
                    .orElseThrow(() ->
                            new RuntimeException("Product not found"));

            product.setQuantity(
                    product.getQuantity() - cart.getQuantity()
            );

            productRepository.save(product);

            OrderItem orderItem = new OrderItem();

            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(product.getId());
            orderItem.setQuantity(cart.getQuantity());
            orderItem.setPrice(product.getPrice());

            orderItemRepository.save(orderItem);
        }

        // Create payment
        Payment payment = new Payment();

        payment.setOrderId(savedOrder.getId());
        payment.setAmount(totalAmount);
        payment.setPaymentMethod("COD");
        payment.setPaymentStatus("PENDING");

        paymentRepository.save(payment);

        // Create notification
        Notification notification = new Notification();

        notification.setUserId(userId);
        notification.setOrderId(savedOrder.getId());
        notification.setMessage(
                "Your order has been placed successfully"
        );
        notification.setRead(false);

        notificationRepository.save(notification);

        // Clear cart
        for (Cart cart : cartItems) {
            cartRepository.delete(cart);
        }

        return savedOrder;
    }
}