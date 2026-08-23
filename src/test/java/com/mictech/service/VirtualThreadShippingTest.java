package com.mictech.service;

import com.mictech.TestClass;
import com.mictech.api.model.Item;
import com.mictech.api.model.Order;
import com.mictech.api.model.OrderItem;
import com.mictech.api.model.OrderRequest;
import com.mictech.controller.ItemController;
import com.mictech.controller.OrderController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
public class VirtualThreadShippingTest extends TestClass {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private OrderController orderController;

    @Autowired
    private ItemController itemController;

    @Test
    void testShippingWithVirtualThreads(CapturedOutput output) {
        // 1. Create an item
        Item newItem = new Item().name("Virtual Thread Item").quantity(50);
        ResponseEntity<Item> createdItemEntity = itemController.createItem(newItem);
        Item createdItem = createdItemEntity.getBody();
        assertThat(createdItem).isNotNull();

        // 2. Create an order
        OrderRequest createRequest = new OrderRequest().deliveryAddress("123 Virtual Lane");
        createRequest.setItems(Collections.singletonList(new OrderItem().itemId(createdItem.getId()).quantity(5)));
        ResponseEntity<Order> createdOrderEntity = orderController.createOrder(createRequest);
        Order createdOrder = createdOrderEntity.getBody();
        assertThat(createdOrder).isNotNull();

        // 3. Purchase the order with virtual threads
        orderController.purchaseOrder(createdOrder.getId());

        // 4. Verify logs
        assertThat(output).contains("Order shipping on virtual thread");
    }
}
