package com.mictech.service;

import com.mictech.api.model.Item;
import com.mictech.api.model.Order;
import com.mictech.api.model.OrderItem;
import com.mictech.api.model.OrderRequest;
import com.mictech.controller.ItemController;
import com.mictech.controller.OrderController;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ExtendWith(OutputCaptureExtension.class)
public class VirtualThreadShippingTest {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadShippingTest.class);

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
        orderController.purchaseOrder(createdOrder.getId(), true);

        // 4. Verify logs
        assertThat(output).contains("Order shipping on virtual thread");
    }

    @Test
    void testShippingWithoutVirtualThreads(CapturedOutput output) {
        // 1. Create an item
        Item newItem = new Item().name("Non-Virtual Thread Item").quantity(50);
        ResponseEntity<Item> createdItemEntity = itemController.createItem(newItem);
        Item createdItem = createdItemEntity.getBody();
        assertThat(createdItem).isNotNull();

        // 2. Create an order
        OrderRequest createRequest = new OrderRequest().deliveryAddress("456 Non-Virtual Lane");
        createRequest.setItems(Collections.singletonList(new OrderItem().itemId(createdItem.getId()).quantity(5)));
        ResponseEntity<Order> createdOrderEntity = orderController.createOrder(createRequest);
        Order createdOrder = createdOrderEntity.getBody();
        assertThat(createdOrder).isNotNull();

        // 3. Purchase the order without virtual threads
        orderController.purchaseOrder(createdOrder.getId(), false);

        // 4. Verify logs
        assertThat(output).contains("Order shipping: item type");
        assertThat(output).doesNotContain("Order shipping on virtual thread");
    }

    @Test
    void testVirtualVsPlatformThreadPerformance() {
        // === Test with Platform Threads ===
        Item platformItem = new Item().name("Platform Thread Item").quantity(2000);
        ResponseEntity<Item> platformItemEntity = itemController.createItem(platformItem);
        Item createdPlatformItem = platformItemEntity.getBody();
        assertThat(createdPlatformItem).isNotNull();

        OrderRequest platformRequest = new OrderRequest().deliveryAddress("789 Platform Plaza");
        platformRequest.setItems(Collections.singletonList(new OrderItem().itemId(createdPlatformItem.getId()).quantity(1000)));
        ResponseEntity<Order> platformOrderEntity = orderController.createOrder(platformRequest);
        Order createdPlatformOrder = platformOrderEntity.getBody();
        assertThat(createdPlatformOrder).isNotNull();

        Instant startPlatform = Instant.now();
        orderController.purchaseOrder(createdPlatformOrder.getId(), false);
        Instant endPlatform = Instant.now();
        long platformDuration = Duration.between(startPlatform, endPlatform).toMillis();
        log.info("Platform thread shipping took: {} ms", platformDuration);

        // === Test with Virtual Threads ===
        Item virtualItem = new Item().name("Virtual Thread Performance Item").quantity(2000);
        ResponseEntity<Item> virtualItemEntity = itemController.createItem(virtualItem);
        Item createdVirtualItem = virtualItemEntity.getBody();
        assertThat(createdVirtualItem).isNotNull();

        OrderRequest virtualRequest = new OrderRequest().deliveryAddress("101 Virtual Vista");
        virtualRequest.setItems(Collections.singletonList(new OrderItem().itemId(createdVirtualItem.getId()).quantity(1000)));
        ResponseEntity<Order> virtualOrderEntity = orderController.createOrder(virtualRequest);
        Order createdVirtualOrder = virtualOrderEntity.getBody();
        assertThat(createdVirtualOrder).isNotNull();

        Instant startVirtual = Instant.now();
        orderController.purchaseOrder(createdVirtualOrder.getId(), true);
        Instant endVirtual = Instant.now();
        long virtualDuration = Duration.between(startVirtual, endVirtual).toMillis();
        log.info("Virtual thread shipping took: {} ms", virtualDuration);

        // === Comparison ===
        if (virtualDuration < platformDuration) {
            log.info("Virtual threads were faster by {} ms", platformDuration - virtualDuration);
        } else {
            log.info("Platform threads were faster by {} ms", virtualDuration - platformDuration);
        }
    }
}
