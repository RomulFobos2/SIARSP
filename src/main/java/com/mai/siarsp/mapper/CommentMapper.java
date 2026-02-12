package com.mai.siarsp.mapper;

import com.mai.siarsp.dto.CommentDTO;
import com.mai.siarsp.models.Comment;
import com.mai.siarsp.models.Employee;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface CommentMapper {
    CommentMapper INSTANCE = Mappers.getMapper(CommentMapper.class);

    @Mapping(source = "author.id", target = "authorId")
    @Mapping(source = "author", target = "authorFullName", qualifiedByName = "authorFullName")
    CommentDTO toDTO(Comment comment);

    List<CommentDTO> toDTOList(List<Comment> comments);

    @Named("authorFullName")
    default String getAuthorFullName(Employee employee) {
        return employee.getFullName();
    }
}
