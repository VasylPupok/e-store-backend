// файл: reservation-service/src/main/java/com/shop/reservation/mapper/
package com.shop.reservation.mapper;

import com.shop.common.dto.ReservationDto;
import com.shop.reservation.entity.Reservation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    ReservationMapper INSTANCE = Mappers.getMapper(ReservationMapper.class);

    @Mapping(target = "status", expression = "java(mapStatus(reservation.getStatus()))")
    ReservationDto toDto(Reservation reservation);

    @Mapping(target = "status", expression = "java(mapStatus(reservationDto.getStatus()))")
    Reservation toEntity(ReservationDto reservationDto);

    default ReservationDto.ReservationStatus mapStatus(Reservation.ReservationStatus status) {
        if (status == null) return null;
        return switch (status) {
            case ACTIVE -> ReservationDto.ReservationStatus.ACTIVE;
            case EXPIRED -> ReservationDto.ReservationStatus.EXPIRED;
            case COMPLETED -> ReservationDto.ReservationStatus.COMPLETED;
            case CANCELLED -> ReservationDto.ReservationStatus.COMPLETED; // мапимо CANCELLED на COMPLETED для DTO
        };
    }

    default Reservation.ReservationStatus mapStatus(ReservationDto.ReservationStatus status) {
        if (status == null) return null;
        return switch (status) {
            case ACTIVE -> Reservation.ReservationStatus.ACTIVE;
            case EXPIRED -> Reservation.ReservationStatus.EXPIRED;
            case COMPLETED -> Reservation.ReservationStatus.COMPLETED;
        };
    }
}