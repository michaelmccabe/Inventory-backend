package com.mictech.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mictech.api.model.Item;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class ItemControllerIntegrationTest {

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
    void testCreateAndGetItem() throws Exception {
        // 1. Create a new item
        Item newItem = new Item();
        newItem.setName("Test Item");
        newItem.setQuantity(100);

        MvcResult postResult = mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isCreated())
                .andReturn();

        // 2. Extract the ID from the response
        String responseBody = postResult.getResponse().getContentAsString();
        Item createdItem = objectMapper.readValue(responseBody, Item.class);
        Long itemId = createdItem.getId();

        // 3. Get the item by its new ID and verify its contents
        mockMvc.perform(get("/api/items/{id}", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(itemId.intValue())))
                .andExpect(jsonPath("$.name", is("Test Item")))
                .andExpect(jsonPath("$.quantity", is(100)));
    }

    @Test
    void testCreateItemWithDuplicateName() throws Exception {
        // 1. Create first item
        Item item1 = new Item();
        item1.setName("Unique Item");
        item1.setQuantity(50);

        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item1)))
                .andExpect(status().isCreated());

        // 2. Attempt to create second item with the same name
        Item item2 = new Item();
        item2.setName("Unique Item");
        item2.setQuantity(75);

        mockMvc.perform(post("/api/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(item2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$").value("Item with name 'Unique Item' already exists"));
    }
}
