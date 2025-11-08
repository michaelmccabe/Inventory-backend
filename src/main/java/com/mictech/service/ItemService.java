package com.mictech.service;

import com.mictech.model.Item;
import com.mictech.repository.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

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
            throw new RuntimeException("Item with name '" + item.getName() + "' already exists");
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
                throw new RuntimeException("Item with name '" + item.getName() + "' already exists");
            }
        });
        
        existingItem.setName(item.getName());
        existingItem.setQuantity(item.getQuantity());
        return itemRepository.save(existingItem);
    }
}
