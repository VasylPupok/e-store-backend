package com.shop.reservation.dto;

import com.shop.common.dto.ProductDto;
import com.shop.common.dto.ReservationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDetailDto {
    private ReservationDto reservation;
    private ProductDto product;
    private Integer timeLeft;  // час у секундах
    private String cartId;
}
