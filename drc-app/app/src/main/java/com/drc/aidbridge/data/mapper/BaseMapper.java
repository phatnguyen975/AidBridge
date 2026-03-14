package com.drc.aidbridge.data.mapper;

import java.util.ArrayList;
import java.util.List;

/**
 * BaseMapper — A generic interface for mapping between DTOs and Domain Models.
 *
 * @param <D> The type of the DTO.
 * @param <E> The type of the Domain Model.
 */
public interface BaseMapper<D, E> {

    // Convert from DTO (Network) to Model (UI/Domain)
    E mapToDomain(D dto);

    // Convert from Model (UI/Domain) back to DTO (Network)
    D mapToDto(E domainModel);

    // Convert a list of DTOs to a list of Domain Models
    default List<E> mapToDomainList(List<D> dtoList) {
        if (dtoList == null) {
            return new ArrayList<>();
        }

        List<E> list = new ArrayList<>();
        for (D dto : dtoList) {
            list.add(mapToDomain(dto));
        }

        return list;
    }

    // Convert a list of Domain Models back to a list of DTOs
    default List<D> mapToDtoList(List<E> domainList) {
        if (domainList == null) {
            return new ArrayList<>();
        }

        List<D> list = new ArrayList<>();
        for (E domainModel : domainList) {
            list.add(mapToDto(domainModel));
        }

        return list;
    }
}
