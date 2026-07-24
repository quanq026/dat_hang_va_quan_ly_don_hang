package com.rikkei.course141.ss1.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.rikkei.course141.ss1.dto.request.OrderItemRequest;
import com.rikkei.course141.ss1.dto.request.OrderRequest;
import com.rikkei.course141.ss1.dto.response.ApiResponse;
import com.rikkei.course141.ss1.model.Order;
import com.rikkei.course141.ss1.model.OrderItem;
import com.rikkei.course141.ss1.model.Product;
import com.rikkei.course141.ss1.model.User;
import com.rikkei.course141.ss1.repository.OrderItemRepository;
import com.rikkei.course141.ss1.repository.OrderRepository;
import com.rikkei.course141.ss1.repository.ProductRepository;
import com.rikkei.course141.ss1.repository.UserRepository;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderController(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                         ProductRepository productRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @PostMapping public ResponseEntity<ApiResponse<Order>> create(@Valid @RequestBody OrderRequest dto, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElseThrow();
        Order order = Order.builder().user(user).createdDate(LocalDateTime.now()).status("PENDING").totalMoney(BigDecimal.ZERO).build();
        order = orderRepository.save(order);
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest item : dto.getItems()) {
            Product p = productRepository.findById(item.getProductId()).orElseThrow();
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(line);
            orderItemRepository.save(OrderItem.builder().order(order).product(p).quantity(item.getQuantity()).priceBuy(line).build());
        }
        order.setTotalMoney(total);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(orderRepository.save(order)));
    }

    @GetMapping("/my") public ResponseEntity<ApiResponse<List<Order>>> myOrders(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(orderRepository.findByUserEmail(auth.getName())));
    }

    @GetMapping public ResponseEntity<ApiResponse<List<Order>>> allOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderRepository.findAll()));
    }

    @PutMapping("/{id}/status") public ResponseEntity<ApiResponse<Order>> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderRepository.findById(id).orElseThrow();
        order.setStatus(status);
        return ResponseEntity.ok(ApiResponse.success(orderRepository.save(order)));
    }
}
