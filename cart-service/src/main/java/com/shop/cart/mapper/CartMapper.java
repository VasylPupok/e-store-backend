package com.shop.cart.mapper;

import com.shop.cart.entity.Cart;
import com.shop.cart.entity.CartItem;
import com.shop.common.dto.CartDto;
import com.shop.common.dto.CartItemDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface CartMapper {
    CartMapper INSTANCE = Mappers.getMapper(CartMapper.class);

    CartDto toDto(Cart cart);

    CartItemDto toItemDto(CartItem cartItem);

    @Mapping(target = "version", ignore = true)
    Cart toEntity(CartDto cartDto);

    @Mapping(target = "id", ignore = true)
    CartItem toItemEntity(CartItemDto cartItemDto);
}
