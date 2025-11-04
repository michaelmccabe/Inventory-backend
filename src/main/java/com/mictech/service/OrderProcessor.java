package com.mictech.service;

import com.mictech.api.model.Order;
import com.mictech.api.model.OrderRequest;
import com.mictech.exception.InsufficientStockException;
import com.mictech.repository.ItemRepository;
import com.mictech.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderProcessor {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    private final AsyncTaskExecutor taskExecutor;

    public OrderProcessor(OrderRepository orderRepository, ItemRepository itemRepository, AsyncTaskExecutor taskExecutor) {
        this.orderRepository = orderRepository;
        this.itemRepository = itemRepository;
        this.taskExecutor = taskExecutor;
    }

    @Transactional
    public Order createOrUpdateOrder(Long orderId, OrderRequest orderRequest) {
        com.mictech.model.Order dbOrder;
        if (orderId != null) {
            log.info("Updating order {}...", orderId);
            dbOrder = orderRepository.findById(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        } else {
            log.info("Creating a new order...");
            dbOrder = new com.mictech.model.Order();
        }

        dbOrder.setDeliveryAddress(orderRequest.getDeliveryAddress());
        dbOrder.setStatus(com.mictech.model.OrderStatus.SAVED);

        if (orderRequest.getItems() != null) {
            for (com.mictech.api.model.OrderItem apiOrderItem : orderRequest.getItems()) {
                com.mictech.model.Item item = itemRepository.findById(apiOrderItem.getItemId())
                        .orElseThrow(() -> new RuntimeException("Item not found: " + apiOrderItem.getItemId()));
                if (item.getQuantity() < apiOrderItem.getQuantity()) {
                    throw new InsufficientStockException("Insufficient stock for item: " + item.getName());
                }
            }

            dbOrder.getItems().clear();
            dbOrder.getItems().addAll(orderRequest.getItems().stream()
                    .map(apiOrderItem -> {
                        com.mictech.model.OrderItem dbOrderItem = new com.mictech.model.OrderItem();
                        dbOrderItem.setItemId(apiOrderItem.getItemId());
                        dbOrderItem.setQuantity(apiOrderItem.getQuantity());
                        return dbOrderItem;
                    })
                    .collect(Collectors.toList()));
        }

        dbOrder = orderRepository.save(dbOrder);
        log.info("Order {} has been saved.", dbOrder.getId());

        return mapToApiOrder(dbOrder);
    }

    @Transactional
    public Order purchaseOrder(Long orderId, boolean virtual) {
        log.info("Processing purchase for order {}...", orderId);
        com.mictech.model.Order dbOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (dbOrder.getStatus() == com.mictech.model.OrderStatus.PURCHASED) {
            throw new RuntimeException("Order " + orderId + " has already been purchased.");
        }

        boolean sufficientStock = true;
        if (dbOrder.getItems() != null) {
            for (com.mictech.model.OrderItem orderItem : dbOrder.getItems()) {
                com.mictech.model.Item item = itemRepository.findById(orderItem.getItemId())
                        .orElseThrow(() -> new RuntimeException("Item not found: " + orderItem.getItemId()));

                if (item.getQuantity() < orderItem.getQuantity()) {
                    sufficientStock = false;
                    break;
                }
            }
        }

        if (sufficientStock) {
            if (dbOrder.getItems() != null) {
                for (com.mictech.model.OrderItem orderItem : dbOrder.getItems()) {
                    com.mictech.model.Item item = itemRepository.findById(orderItem.getItemId()).get(); // Already checked
                    int newQuantity = item.getQuantity() - orderItem.getQuantity();
                    item.setQuantity(newQuantity);
                    itemRepository.save(item);
                    log.info("Updated inventory for item {}: new quantity is {}", item.getId(), newQuantity);
                }
            }
            dbOrder.setStatus(com.mictech.model.OrderStatus.PURCHASED);
            log.info("Order {} has been purchased.", dbOrder.getId());
        } else {
            dbOrder.setStatus(com.mictech.model.OrderStatus.HELD);
            log.warn("Order {} has been put on hold due to insufficient stock.", dbOrder.getId());
        }

        dbOrder = orderRepository.save(dbOrder);

        if (virtual) {
            shipItemsUsingVirtualThreads(dbOrder);
        } else {
            shipItems(dbOrder);
        }

        return mapToApiOrder(dbOrder);
    }

    /**
     * @deprecated Use {@link #shipItemsUsingVirtualThreads(com.mictech.model.Order)} instead.
     */
    @Deprecated
    private void shipItems(com.mictech.model.Order dbOrder) {
        if (dbOrder.getStatus() == com.mictech.model.OrderStatus.PURCHASED && dbOrder.getItems() != null) {
            for (com.mictech.model.OrderItem orderItem : dbOrder.getItems()) {
                com.mictech.model.Item item = itemRepository.findById(orderItem.getItemId())
                        .orElseThrow(() -> new RuntimeException("Item not found: " + orderItem.getItemId()));
                for (int i = 1; i <= orderItem.getQuantity(); i++) {
                    log.info("Order shipping: item type {} and count = {} of {}", item.getName(), i, orderItem.getQuantity());
                }
            }
        }
    }

    private void shipItemsUsingVirtualThreads(com.mictech.model.Order dbOrder) {
        if (dbOrder.getStatus() == com.mictech.model.OrderStatus.PURCHASED && dbOrder.getItems() != null) {
            for (com.mictech.model.OrderItem orderItem : dbOrder.getItems()) {
                com.mictech.model.Item item = itemRepository.findById(orderItem.getItemId())
                        .orElseThrow(() -> new RuntimeException("Item not found: " + orderItem.getItemId()));
                for (int i = 1; i <= orderItem.getQuantity(); i++) {
                    final int count = i;
                    taskExecutor.execute(() -> {
                        log.info("Order shipping on virtual thread: item type {} and count = {} of {}", item.getName(), count, orderItem.getQuantity());
                    });
                }
            }
        }
    }

    private Order mapToApiOrder(com.mictech.model.Order dbOrder) {
        Order apiOrder = new Order();
        apiOrder.setId(dbOrder.getId());
        apiOrder.setDeliveryAddress(dbOrder.getDeliveryAddress());
        apiOrder.setStatus(com.mictech.api.model.OrderStatus.fromValue(dbOrder.getStatus().name()));
        if (dbOrder.getItems() != null) {
            apiOrder.setItems(dbOrder.getItems().stream()
                    .map(dbOrderItem -> {
                        com.mictech.api.model.OrderItem apiOrderItem = new com.mictech.api.model.OrderItem();
                        apiOrderItem.setItemId(dbOrderItem.getItemId());
                        apiOrderItem.setQuantity(dbOrderItem.getQuantity());
                        return apiOrderItem;
                    })
                    .collect(Collectors.toList()));
        }
        return apiOrder;
    }
}
