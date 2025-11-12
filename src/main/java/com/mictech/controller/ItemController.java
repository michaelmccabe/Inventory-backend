package com.mictech.controller;

import com.mictech.api.ItemsApi;
import com.mictech.api.model.Item;
import com.mictech.mapper.ItemMapper;
import com.mictech.service.ItemService;
import io.micrometer.observation.annotation.Observed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class ItemController implements ItemsApi {

    private final ItemService itemService;
    private final ItemMapper itemMapper;

    @Autowired
    public ItemController(ItemService itemService, ItemMapper itemMapper) {
        this.itemService = itemService;
        this.itemMapper = itemMapper;
    }

    @Override
    @Observed(name = "create.item", contextualName = "create-item")  // Manual span
    public ResponseEntity<Item> createItem(Item item) {
        log.debug("createItem {}", item);
        com.mictech.model.Item createdItem = itemService.createItem(itemMapper.toEntity(item));
        return new ResponseEntity<>(itemMapper.toApi(createdItem), HttpStatus.CREATED);
    }

    @Override
    @Observed(name = "delete.item", contextualName = "delete-item")
    public ResponseEntity<Void> deleteItem(Long id) {
        log.debug("deleteItem id: {}", id);
        itemService.deleteItem(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    @Observed(name = "get.items", contextualName = "get-all-items")
    public ResponseEntity<List<Item>> getAllItems() {
        log.debug("getAllItems");
        List<com.mictech.model.Item> items = itemService.getAllItems();
        return new ResponseEntity<>(itemMapper.toApiList(items), HttpStatus.OK);
    }

    @Override
    @Observed(name = "get.item", contextualName = "get-item")
    public ResponseEntity<Item> getItemById(Long id) {
        log.debug("getItemById id: {}", id);
        return itemService.getItemById(id)
                .map(item -> new ResponseEntity<>(itemMapper.toApi(item), HttpStatus.OK))
                .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @Override
    @Observed(name = "update.item", contextualName = "update-item")
    public ResponseEntity<Item> updateItem(Long id, Item item) {
        log.debug("updateItem id: {}", id);
        com.mictech.model.Item updatedItem = itemService.updateItem(id, itemMapper.toEntity(item));
        return new ResponseEntity<>(itemMapper.toApi(updatedItem), HttpStatus.OK);
    }
}
