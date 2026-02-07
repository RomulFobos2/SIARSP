package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.ZoneProductDTO;
import com.mai.siarsp.models.ZoneProduct;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ZoneProductMapper {
    ZoneProductMapper INSTANCE = Mappers.getMapper(ZoneProductMapper.class);

    @Mapping(source = "zone.id", target = "zoneId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(source = "product.article", target = "productArticle")
    @Mapping(source = "zoneProduct", target = "volumePerUnit", qualifiedByName = "volumePerUnit")
    @Mapping(source = "zoneProduct", target = "totalVolume", qualifiedByName = "totalVolume")
    ZoneProductDTO toDTO(ZoneProduct zoneProduct);

    List<ZoneProductDTO> toDTOList(List<ZoneProduct> zoneProducts);

    @Named("volumePerUnit")
    default double getVolumePerUnit(ZoneProduct zoneProduct) {
        return zoneProduct.getVolumePerUnit();
    }

    @Named("totalVolume")
    default double getTotalVolume(ZoneProduct zoneProduct) {
        return zoneProduct.getTotalVolume();
    }
}
