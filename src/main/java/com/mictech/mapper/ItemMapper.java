package com.mictech.mapper;

import com.mictech.api.model.Item;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ItemMapper {
    Item toApi(com.mictech.model.Item item);
    com.mictech.model.Item toEntity(Item item);
    List<Item> toApiList(List<com.mictech.model.Item> items);
}
