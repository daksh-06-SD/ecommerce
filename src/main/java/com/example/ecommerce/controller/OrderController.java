package com.example.ecommerce.controller;

import com.example.ecommerce.model.Cart;
import com.example.ecommerce.model.Order;
import com.example.ecommerce.model.OrderItem;
import com.example.ecommerce.model.Product;
import com.example.ecommerce.repository.CartRepository;
import com.example.ecommerce.repository.OrderItemRepository;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;


    public OrderController(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CartRepository cartRepository,
            ProductRepository productRepository) {

        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }


    // =========================================================
    // PLACE ORDER
    // =========================================================

    @PostMapping
    public Order placeOrder(@RequestBody Order order) {

        if (order.getUserId() == null) {

            throw new RuntimeException(
                    "User ID is required"
            );
        }

        // =====================================================
        // DEFAULT STATUS
        // =====================================================

        order.setStatus(
                "PLACED"
        );

        // =====================================================
        // ORDER DATE + EXPECTED DELIVERY DATE
        // =====================================================

        LocalDate today = LocalDate.now();

        order.setOrderDate(today);

        // Expected delivery after 4 days
        order.setExpectedDeliveryDate(
                today.plusDays(4)
        );

        Long userId = order.getUserId();


        // Get cart

        List<Cart> cartItems =
                cartRepository.findByUserId(userId);


        if (cartItems.isEmpty()) {

            throw new RuntimeException(
                    "Cart is empty"
            );
        }



        // =====================================================
        // CHECK STOCK
        // =====================================================

        for (Cart cart : cartItems) {

            Product product =
                    productRepository
                            .findById(cart.getProductId())
                            .orElse(null);


            if (product == null) {

                throw new RuntimeException(
                        "Product not found: "
                                + cart.getProductId()
                );
            }


            if (cart.getQuantity() <= 0) {

                throw new RuntimeException(
                        "Invalid quantity for product: "
                                + product.getName()
                );
            }


            if (product.getQuantity()
                    < cart.getQuantity()) {

                throw new RuntimeException(
                        "Not enough stock for product: "
                                + product.getName()
                                + ". Available stock: "
                                + product.getQuantity()
                );
            }
        }


        // =====================================================
        // CALCULATE SUBTOTAL
        // =====================================================

        double subtotal = 0;


        for (Cart cart : cartItems) {

            Product product =
                    productRepository
                            .findById(cart.getProductId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Product not found"
                                    )
                            );


            subtotal +=
                    product.getPrice()
                            * cart.getQuantity();
        }


        // =====================================================
        // GST 18%
        // =====================================================

        double tax =
                subtotal * 0.18;


        double finalAmount =
                subtotal + tax;


        order.setTotalAmount(
                finalAmount
        );


        // =====================================================
        // DEFAULT STATUS
        // =====================================================

        order.setStatus(
                "PLACED"
        );


        // =====================================================
        // DEFAULT PAYMENT
        // =====================================================

        if (order.getPaymentMethod() == null ||
                order.getPaymentMethod().isBlank()) {

            order.setPaymentMethod(
                    "COD"
            );
        }


        // =====================================================
        // SAVE ORDER
        // =====================================================

        Order savedOrder =
                orderRepository.save(order);


        // =====================================================
        // CREATE ORDER ITEMS
        // + REDUCE STOCK
        // =====================================================

        for (Cart cart : cartItems) {

            Product product =
                    productRepository
                            .findById(cart.getProductId())
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Product not found"
                                    )
                            );


            OrderItem orderItem =
                    new OrderItem();


            orderItem.setOrderId(
                    savedOrder.getId()
            );


            orderItem.setProductId(
                    product.getId()
            );


            orderItem.setQuantity(
                    cart.getQuantity()
            );


            orderItem.setPrice(
                    product.getPrice()
            );


            orderItemRepository.save(
                    orderItem
            );


            // Reduce stock

            int remainingStock =
                    product.getQuantity()
                            - cart.getQuantity();


            product.setQuantity(
                    remainingStock
            );


            productRepository.save(
                    product
            );
        }


        // =====================================================
        // CLEAR CART
        // =====================================================

        cartRepository.deleteAll(
                cartItems
        );


        return savedOrder;
    }


    // =========================================================
    // GET USER ORDERS
    // =========================================================

    @GetMapping("/user/{userId}")
    public List<Order> getUserOrders(
            @PathVariable Long userId) {

        return orderRepository.findByUserId(
                userId
        );
    }


    // =========================================================
    // GET ORDER BY ID
    // =========================================================

    @GetMapping("/{id}")
    public Order getOrderById(
            @PathVariable Long id) {

        return orderRepository
                .findById(id)
                .orElse(null);
    }


    // =========================================================
    // CUSTOMER - CANCEL ORDER
    // =========================================================

    @PutMapping("/user/{orderId}/cancel")
    public Order cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long userId) {


        // =====================================================
        // FIND ORDER
        // =====================================================

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElse(null);


        if (order == null) {

            throw new RuntimeException(
                    "Order not found"
            );
        }


        // =====================================================
        // CHECK OWNER
        // =====================================================

        if (order.getUserId() == null ||
                !order.getUserId().equals(userId)) {

            throw new RuntimeException(
                    "You are not allowed to cancel this order"
            );
        }


        // =====================================================
        // CURRENT STATUS
        // =====================================================

        String currentStatus =
                order.getStatus();


        if ("CANCELLED".equalsIgnoreCase(
                currentStatus)) {

            throw new RuntimeException(
                    "Order is already cancelled"
            );
        }


        if ("SHIPPED".equalsIgnoreCase(
                currentStatus)) {

            throw new RuntimeException(
                    "Shipped orders cannot be cancelled"
            );
        }


        if ("DELIVERED".equalsIgnoreCase(
                currentStatus)) {

            throw new RuntimeException(
                    "Delivered orders cannot be cancelled"
            );
        }


        // =====================================================
        // GET ORDER ITEMS
        // =====================================================

        List<OrderItem> orderItems =
                orderItemRepository
                        .findByOrderId(orderId);


        // =====================================================
        // RESTORE STOCK
        // =====================================================

        restoreStock(orderItems);


        // =====================================================
        // UPDATE STATUS
        // =====================================================

        order.setStatus(
                "CANCELLED"
        );


        return orderRepository.save(
                order
        );
    }


    // =========================================================
    // ADMIN - GET ALL ORDERS
    // =========================================================

    @GetMapping("/admin/all")
    public List<Order> getAllOrders() {

        return orderRepository.findAll();
    }


    // =========================================================
    // ADMIN - UPDATE ORDER STATUS
    // =========================================================

    @PutMapping("/admin/{id}/status")
    public Order updateOrderStatus(
            @PathVariable Long id,
            @RequestParam String status) {


        // =====================================================
        // FIND ORDER
        // =====================================================

        Order order =
                orderRepository
                        .findById(id)
                        .orElse(null);


        if (order == null) {

            throw new RuntimeException(
                    "Order not found"
            );
        }


        // =====================================================
        // NORMALIZE STATUS
        // =====================================================

        String newStatus =
                status.toUpperCase().trim();


        // =====================================================
        // VALID STATUS
        // =====================================================

        if (!newStatus.equals("PLACED") &&
                !newStatus.equals("PROCESSING") &&
                !newStatus.equals("SHIPPED") &&
                !newStatus.equals("DELIVERED") &&
                !newStatus.equals("CANCELLED")) {

            throw new RuntimeException(
                    "Invalid order status"
            );
        }


        String oldStatus =
                order.getStatus();


        // =====================================================
        // ALREADY CANCELLED
        // =====================================================

        if ("CANCELLED".equalsIgnoreCase(oldStatus) &&
                "CANCELLED".equals(newStatus)) {

            throw new RuntimeException(
                    "Order is already cancelled"
            );
        }


        // =====================================================
        // CANCEL ORDER
        // =====================================================

        if ("CANCELLED".equals(newStatus) &&
                !"CANCELLED".equalsIgnoreCase(oldStatus)) {

            List<OrderItem> orderItems =
                    orderItemRepository
                            .findByOrderId(id);


            restoreStock(orderItems);
        }


        // =====================================================
        // PREVENT CHANGING CANCELLED ORDER
        // =====================================================

        if ("CANCELLED".equalsIgnoreCase(oldStatus) &&
                !"CANCELLED".equals(newStatus)) {

            throw new RuntimeException(
                    "Cancelled order cannot be changed"
            );
        }


        // =====================================================
        // UPDATE STATUS
        // =====================================================

        order.setStatus(
                newStatus
        );


        return orderRepository.save(
                order
        );
    }


    // =========================================================
    // RESTORE PRODUCT STOCK
    // =========================================================

    private void restoreStock(
            List<OrderItem> orderItems) {


        for (OrderItem item : orderItems) {

            Product product =
                    productRepository
                            .findById(
                                    item.getProductId()
                            )
                            .orElse(null);


            if (product == null) {
                continue;
            }


            int restoredStock =
                    product.getQuantity()
                            + item.getQuantity();


            product.setQuantity(
                    restoredStock
            );


            productRepository.save(
                    product
            );
        }
    }
}