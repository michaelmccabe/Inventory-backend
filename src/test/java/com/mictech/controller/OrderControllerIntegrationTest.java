package com.mictech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mictech.api.model.Item;
import com.mictech.api.model.Order;
import com.mictech.api.model.OrderItem;
import com.mictech.api.model.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class OrderControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testCreateOrder() throws Exception {
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setDeliveryAddress("123 Test Street");

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deliveryAddress", is("123 Test Street")))
                .andExpect(jsonPath("$.status", is("SAVED")));
    }

    @Test
    void testUpdateOrder() throws Exception {
        // 1. Create an order
        OrderRequest createRequest = new OrderRequest();
        createRequest.setDeliveryAddress("Initial Address");
        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Order createdOrder = objectMapper.readValue(createResult.getResponse().getContentAsString(), Order.class);

        // 2. Update the order
        OrderRequest updateRequest = new OrderRequest();
        updateRequest.setDeliveryAddress("Updated Address");
        mockMvc.perform(put("/api/orders/{id}", createdOrder.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryAddress", is("Updated Address")));
    }

    @Test
    void testPurchaseOrder() throws Exception {
        // 1. Create an item
        Item newItem = new Item().name("Test Item").quantity(100);
        MvcResult itemResult = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andReturn();
        Item createdItem = objectMapper.readValue(itemResult.getResponse().getContentAsString(), Item.class);
        Long itemId = createdItem.getId();

        // 2. Create an order
        OrderRequest createRequest = new OrderRequest();
        createRequest.setDeliveryAddress("123 Purchase Lane");
        OrderItem orderItem = new OrderItem().itemId(itemId).quantity(10);
        createRequest.setItems(Collections.singletonList(orderItem));

        MvcResult createResult = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Order createdOrder = objectMapper.readValue(createResult.getResponse().getContentAsString(), Order.class);

        // 3. Purchase the order
        mockMvc.perform(post("/api/orders/{id}/purchase", createdOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PURCHASED")));

        // 4. Verify item quantity
        mockMvc.perform(get("/api/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(90)));
    }

    @Test
    void testCreateOrderWithMultipleItems() throws Exception {
        // 1. Create items
        Item item1 = createItem("Item 1", 50);
        Item item2 = createItem("Item 2", 30);

        // 2. Create order request
        OrderRequest orderRequest = new OrderRequest().deliveryAddress("456 Multi Street");
        orderRequest.setItems(Arrays.asList(
                new OrderItem().itemId(item1.getId()).quantity(20),
                new OrderItem().itemId(item2.getId()).quantity(10)
        ));

        // 3. Create order
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items.length()", is(2)));
    }

    @Test
    void testCreateOrderInsufficientStock() throws Exception {
        // 1. Create an item
        Item item = createItem("Low Stock Item", 5);

        // 2. Create order request with quantity greater than stock
        OrderRequest orderRequest = new OrderRequest().deliveryAddress("789 Error Avenue");
        orderRequest.setItems(Collections.singletonList(new OrderItem().itemId(item.getId()).quantity(10)));

        // 3. Attempt to create order
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPurchaseOrderInsufficientStock() throws Exception {
        // 1. Create an item
        Item item = createItem("Volatile Item", 20);

        // 2. Create an order
        OrderRequest createRequest = new OrderRequest().deliveryAddress("101 Hold Street");
        createRequest.setItems(Collections.singletonList(new OrderItem().itemId(item.getId()).quantity(15)));
        Order createdOrder = createOrder(createRequest);

        // 3. Reduce stock so the order can't be fulfilled
        item.setQuantity(10);
        updateItem(item);

        // 4. Attempt to purchase the order
        mockMvc.perform(post("/api/orders/{id}/purchase", createdOrder.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("HELD")));

        // 5. Verify item quantity is unchanged
        mockMvc.perform(get("/api/items/{id}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", is(10)));
    }

    private Item createItem(String name, int quantity) throws Exception {
        Item item = new Item().name(name).quantity(quantity);
        MvcResult result = mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Item.class);
    }

    private Order createOrder(OrderRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), Order.class);
    }

    private void updateItem(Item item) throws Exception {
        mockMvc.perform(put("/api/items/{id}", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk());
    }
}
