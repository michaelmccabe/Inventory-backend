package com.mictech.service;

import com.mictech.exception.DuplicateItemNameException;
import com.mictech.model.Item;
import com.mictech.repository.ItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ItemService {

    private final ItemRepository itemRepository;

    @Autowired
    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Optional<Item> getItemById(Long id) {
        return itemRepository.findById(id);
    }

    public Item createItem(Item item) {
        // Check if item with the same name already exists
        itemRepository.findByName(item.getName()).ifPresent(existingItem -> {
            log.warn("Attempted to create duplicate item with name: {}", item.getName());
            throw new DuplicateItemNameException("Item with name '" + item.getName() + "' already exists");
        });
        return itemRepository.save(item);
    }

    public void deleteItem(Long id) {
        itemRepository.deleteById(id);
    }

    public Item updateItem(Long id, Item item) {
        Item existingItem = itemRepository.findById(id).orElseThrow(() -> new RuntimeException("Item not found: " + id));
        
        // Check if another item with the same name already exists (excluding current item)
        itemRepository.findByName(item.getName()).ifPresent(foundItem -> {
            if (!foundItem.getId().equals(id)) {
                log.warn("Attempted to update item with existing name: {}", item.getName());
                throw new DuplicateItemNameException("Item with name '" + item.getName() + "' already exists");
            }
        });
        
        existingItem.setName(item.getName());
        existingItem.setQuantity(item.getQuantity());
        return itemRepository.save(existingItem);
    }
}
