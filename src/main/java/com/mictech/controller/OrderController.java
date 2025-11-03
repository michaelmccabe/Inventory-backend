package com.mictech.controller;

import com.mictech.api.OrdersApi;
import com.mictech.api.model.Order;
import com.mictech.api.model.OrderRequest;
import com.mictech.service.OrderProcessor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController implements OrdersApi {

    private final OrderProcessor orderProcessor;

    public OrderController(OrderProcessor orderProcessor) {
        this.orderProcessor = orderProcessor;
    }

    @Override
    public ResponseEntity<Order> createOrder(OrderRequest orderRequest) {
        Order createdOrder = orderProcessor.createOrUpdateOrder(null, orderRequest);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Order> updateOrder(Long id, OrderRequest orderRequest) {
        Order updatedOrder = orderProcessor.createOrUpdateOrder(id, orderRequest);
        return ResponseEntity.ok(updatedOrder);
    }

    @Override
    public ResponseEntity<Order> purchaseOrder(Long id) {
        Order purchasedOrder = orderProcessor.purchaseOrder(id);
        return ResponseEntity.ok(purchasedOrder);
    }
}
