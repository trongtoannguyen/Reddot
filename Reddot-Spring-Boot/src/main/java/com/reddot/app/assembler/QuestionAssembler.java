package com.reddot.app.assembler;

import com.reddot.app.assembler.decorator.QuestionMapperDecorator;
import com.reddot.app.dto.response.QuestionDTO;
import com.reddot.app.entity.Question;
import org.mapstruct.DecoratedWith;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * QuestionAssembler class is used to convert Question entity to QuestionDTO record and vice versa.
 */
@Mapper(componentModel = "spring", uses = {UserAssembler.class, CommentAssembler.class}, injectionStrategy = InjectionStrategy.CONSTRUCTOR)
@DecoratedWith(QuestionMapperDecorator.class)
@Component("delegate")
public interface QuestionAssembler {
    /**
     * Reference resources:
     * <ol>
     *     <li>
     *         <a href="https://mapstruct.org/documentation/stable/reference/html/#invoking-custom-mapping-method">
     *             MapStruct - Invoking custom mapping method</a>
     *     </li>
     * </ol>
     */
    @Mapping(target = "questionId", source = "question.id")
    @Mapping(target = "author", source = "user")
    @Mapping(target = "commentCount", source = "question")
    @Mapping(target = "commentList", source = "question.comments")
    @Mapping(target = "lastEditDate", source = "question.updatedAt")
    @Mapping(target = "creationDate", source = "question.createdAt")
    @Mapping(target = "closeDate", source = "question.closedAt")
    @Mapping(target = "upvoted", ignore = true)
    @Mapping(target = "downvoted", ignore = true)
    @Mapping(target = "bookmarked", ignore = true)
    QuestionDTO toDTO(Question question);

    List<QuestionDTO> toDTOList(List<Question> list);

    // count total number of comments
    default Integer countComments(Question question) {
        return (question.getComments() == null) ? 0 : question.getComments().size();
    }
}
