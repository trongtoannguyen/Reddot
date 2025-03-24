package com.reddot.app.assembler;

import com.reddot.app.dto.response.TagDTO;
import com.reddot.app.entity.Tag;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface TagAssembler {
    TagDTO toDTO(Tag tag);
}