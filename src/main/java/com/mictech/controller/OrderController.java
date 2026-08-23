package com.mictech.controller;

import com.mictech.api.OrdersApi;
import com.mictech.api.model.Order;
import com.mictech.api.model.OrderRequest;
import com.mictech.service.OrderProcessor;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class OrderController implements OrdersApi {

    private final OrderProcessor orderProcessor;

    public OrderController(OrderProcessor orderProcessor) {
        this.orderProcessor = orderProcessor;
    }

    @Override
    @Observed(name = "create.order", contextualName = "create-order")
    public ResponseEntity<Order> createOrder(OrderRequest orderRequest) {
        log.debug("createOrder {}", orderRequest);
        Order createdOrder = orderProcessor.createOrUpdateOrder(null, orderRequest);
        return new ResponseEntity<>(createdOrder, HttpStatus.CREATED);
    }

    @Override
    @Observed(name = "update.order", contextualName = "update-order")
    public ResponseEntity<Order> updateOrder(Long id, OrderRequest orderRequest) {
        log.debug("updateOrder id: {}, orderRequest:  {}", id, orderRequest);
        Order updatedOrder = orderProcessor.createOrUpdateOrder(id, orderRequest);
        return ResponseEntity.ok(updatedOrder);
    }

    @Override
    @Observed(name = "purchase.order", contextualName = "purchase-order")
    public ResponseEntity<Order> purchaseOrder(Long id) {
        Order purchasedOrder = orderProcessor.purchaseOrder(id);
        return ResponseEntity.ok(purchasedOrder);
    }

    @Override
    @Observed(name = "get.orders", contextualName = "get-all-orders")
    public ResponseEntity<List<Order>> getAllOrders() {
        List<Order> orders = orderProcessor.getAllOrders();
        return ResponseEntity.ok(orders);
    }

    @Override
    @Observed(name = "get.order", contextualName = "get-order")
    public ResponseEntity<Order> getOrderById(Long id) {
        return orderProcessor.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
