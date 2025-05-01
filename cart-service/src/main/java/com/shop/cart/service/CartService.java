package com.shop.cart.service;

import com.shop.cart.client.ProductServiceClient;
import com.shop.cart.client.QueueServiceClient;
import com.shop.cart.client.ReservationServiceClient;
import com.shop.cart.dto.AddToCartRequest;
import com.shop.cart.dto.AddToCartResponse;
import com.shop.cart.entity.Cart;
import com.shop.cart.entity.CartItem;
import com.shop.cart.mapper.CartMapper;
import com.shop.cart.repository.CartRepository;
import com.shop.common.dto.CartDto;
import com.shop.common.dto.ProductDto;
import com.shop.common.dto.ReservationDto;
import com.shop.common.exception.ResourceNotFoundException;
import com.shop.common.messaging.ReservationEvent;
import com.shop.product.dto.ProductAvailabilityDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

import static com.shop.cart.config.RabbitMQConfig.CART_EXCHANGE;
import static com.shop.cart.config.RabbitMQConfig.CART_UPDATED_ROUTING_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final CartMapper cartMapper;
    private final ProductServiceClient productServiceClient;
    private final QueueServiceClient queueServiceClient;
    private final ReservationServiceClient reservationServiceClient;
    private final RabbitTemplate rabbitTemplate;

    public CartDto getCartByUserId(String userId) {
        Cart cart = getOrCreateCart(userId);
        return cartMapper.toDto(cart);
    }

    public AddToCartResponse addItemToCart(String userId, @Valid AddToCartRequest request) {
        // Перевіряємо доступність товару
        ProductAvailabilityDto availability = productServiceClient.getProductAvailability(request.getProductId());
        ProductDto product = productServiceClient.getProduct(request.getProductId());

        // Отримуємо або створюємо кошик користувача
        Cart cart = getOrCreateCart(userId);

        // Створюємо новий CartItem
        CartItem cartItem = createCartItem(product, request.getQuantity());

        // Якщо товар з обмеженим залишком і кількість доступного товару менша за запитану,
        // додаємо запит у чергу
        if (product.isLimited() && availability.getStockQuantity() - availability.getReservedQuantity() < request.getQuantity()) {
            // Додаємо запит у чергу через QueueService
            AddToCartResponse.QueueInfo queueInfo = queueServiceClient.addToQueue(
                    request.getProductId(),
                    userId,
                    request.getQuantity());

            // Повертаємо відповідь з інформацією про чергу
            return AddToCartResponse.builder()
                    .cartId(cart.getId())
                    .added(false)
                    .queueInfo(queueInfo)
                    .build();
        }

        // Якщо товар доступний, резервуємо його
        ReservationDto reservation = reserveProductAndAddToCart(cart, cartItem, userId);

        // Оновлюємо інформацію про резервацію товару
        cartItem.setReserved(true);
        cartItem.setReservationId(reservation.getId());
        cartItem.setReservationExpiresAt(reservation.getExpiresAt());

        // Додаємо товар у кошик
        cart.addItem(cartItem);
        cart.updateTimestamps();
        cartRepository.save(cart);

        // Публікуємо подію про оновлення кошика
        publishCartUpdatedEvent(cart);

        // Повертаємо відповідь про успішне додавання товару
        return AddToCartResponse.builder()
                .cartId(cart.getId())
                .added(true)
                .queueInfo(null)
                .build();
    }

    public void removeItemFromCart(String userId, String itemId) {
        Cart cart = findCartByUserId(userId);

        // Знаходимо товар у кошику
        Optional<CartItem> itemOpt = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst();

        if (itemOpt.isPresent()) {
            CartItem item = itemOpt.get();

            // Якщо товар зарезервований, скасовуємо резервацію
            if (item.isReserved() && item.getReservationId() != null) {
                // Скасовуємо резервацію
                reservationServiceClient.cancelReservation(item.getReservationId());

                // Звільняємо резервацію товару
                productServiceClient.releaseReservation(item.getProductId(), item.getQuantity());
            }

            // Видаляємо товар з кошика
            cart.removeItem(itemId);
            cart.updateTimestamps();
            cartRepository.save(cart);

            // Публікуємо подію про оновлення кошика
            publishCartUpdatedEvent(cart);
        } else {
            throw new ResourceNotFoundException("Item not found in cart: " + itemId);
        }
    }

    // Обробка події закінчення резервації
    public void handleReservationExpired(ReservationEvent event) {
        // Шукаємо кошик за його ID
        Optional<Cart> cartOpt = cartRepository.findById(event.getCartId());

        if (cartOpt.isPresent()) {
            Cart cart = cartOpt.get();

            // Шукаємо товар з вказаним reservationId
            cart.getItems().stream()
                    .filter(item -> item.getReservationId() != null &&
                            item.getReservationId().equals(event.getReservationId()))
                    .findFirst()
                    .ifPresent(item -> {
                        // Видаляємо товар з кошика
                        cart.removeItem(item.getId());
                        cart.updateTimestamps();
                        cartRepository.save(cart);

                        // Публікуємо подію про оновлення кошика
                        publishCartUpdatedEvent(cart);

                        log.info("Removed expired item {} from cart {}", item.getId(), cart.getId());
                    });
        }
    }

    // Обробка події завершення запиту в черзі
    public void handleQueueRequestCompleted(String userId, String productId, Integer quantity) {
        // Отримуємо інформацію про товар
        ProductDto product = productServiceClient.getProduct(productId);

        // Отримуємо або створюємо кошик користувача
        Cart cart = getOrCreateCart(userId);

        // Створюємо новий CartItem
        CartItem cartItem = createCartItem(product, quantity);

        // Резервуємо товар
        ReservationDto reservation = reserveProductAndAddToCart(cart, cartItem, userId);

        // Оновлюємо інформацію про резервацію товару
        cartItem.setReserved(true);
        cartItem.setReservationId(reservation.getId());
        cartItem.setReservationExpiresAt(reservation.getExpiresAt());

        // Додаємо товар у кошик
        cart.addItem(cartItem);
        cart.updateTimestamps();
        cartRepository.save(cart);

        // Публікуємо подію про оновлення кошика
        publishCartUpdatedEvent(cart);

        log.info("Added item from queue: productId={}, userId={}, cartId={}", productId, userId, cart.getId());
    }

    private Cart getOrCreateCart(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart newCart = Cart.builder()
                            .id(UUID.randomUUID().toString())
                            .userId(userId)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private Cart findCartByUserId(String userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user: " + userId));
    }

    private CartItem createCartItem(ProductDto product, Integer quantity) {
        return CartItem.builder()
                .id(UUID.randomUUID().toString())
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .quantity(quantity)
                .isReserved(false)
                .limitedProduct(product.isLimited())
                .build();
    }

    private ReservationDto reserveProductAndAddToCart(Cart cart, CartItem cartItem, String userId) {
        // Резервуємо товар в ProductService
        productServiceClient.reserveProduct(
                cartItem.getProductId(),
                cartItem.getQuantity(),
                userId);

        // Створюємо резервацію в ReservationService
        return reservationServiceClient.createReservation(
                cartItem.getProductId(),
                userId,
                cartItem.getQuantity(),
                cart.getId());
    }

    private void publishCartUpdatedEvent(Cart cart) {
        rabbitTemplate.convertAndSend(
                CART_EXCHANGE,
                CART_UPDATED_ROUTING_KEY,
                cartMapper.toDto(cart));

        log.info("Published cart updated event for cart id: {}", cart.getId());
    }
}
