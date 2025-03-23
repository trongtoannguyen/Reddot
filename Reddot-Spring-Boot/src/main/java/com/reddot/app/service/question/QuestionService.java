package com.reddot.app.service.question;

import com.reddot.app.dto.request.QuestionCreateDTO;
import com.reddot.app.dto.request.QuestionUpdateDTO;
import com.reddot.app.dto.response.QuestionDTO;
import com.reddot.app.entity.User;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.ResourceNotFoundException;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

//TODO: implement sorting and filtering
@Component
public interface QuestionService {
    /**
     * Create a new question
     *
     * @param user the id of the user creating the question
     * @param dto  QuestionPostDTO object containing the question details
     * @return QuestionDTO object containing the created question details
     * @throws ResourceNotFoundException if the user is not found
     */
    QuestionDTO questionCreate(User user, QuestionCreateDTO dto) throws ResourceNotFoundException;

    /**
     * Get the questions identified by a list of ids.
     *
     * @param ids           the id of the question
     * @param includeHidden whether to include hidden questions and related hidden associations
     * @return QuestionDTO object containing the question details
     */
    // TODO: https://api.stackexchange.com/docs/questions-by-ids
    List<QuestionDTO> questionGetByIds(List<Integer> ids, boolean includeHidden);

    /**
     * Get questions identified by id in a list.
     * Method returns some user-specific properties related to the question.
     *
     * @param ids  a list of ids of the questions
     * @param user the user requesting the question
     * @return QuestionDTO object containing the question details
     * @throws ResourceNotFoundException if the question is not found
     */
    List<QuestionDTO> questionGetByIdsWithUser(List<Integer> ids, @NonNull User user) throws ResourceNotFoundException;

    /**
     * Get all questions on the site.
     *
     * @return List of QuestionDTO objects containing the question details
     */
    default List<QuestionDTO> questionGetAll() {
        return questionGetAll(false);
    }

    /**
     * Get all questions on the site.
     *
     * @return List of QuestionDTO objects containing the question details
     */
    List<QuestionDTO> questionGetAll(boolean includeHidden);

    /**
     * Get the questions asked by the users identified by user id.
     * <p>
     * This method useful when fetching questions in profile page of a specific user.
     *
     * @param userId        the id of the user
     * @param sort          the sorting order.
     * @param includeHidden whether to include hidden questions and related hidden associations
     * @return List of QuestionDTO objects containing the question details
     */
    List<QuestionDTO> questionGetAllOfUserId(Integer userId, String sort, boolean includeHidden) throws ResourceNotFoundException;

    /**
     * Get all questions on the site.
     * Method returns some user-specific properties related to the questions.
     *
     * @param user          the user requesting the questions
     * @param includeHidden whether to include hidden questions and related hidden associations
     * @return List of QuestionDTO objects containing the question details
     * @throws ResourceNotFoundException if the user is not found
     */
    List<QuestionDTO> questionGetAllWithUser(@NonNull User user, boolean includeHidden) throws ResourceNotFoundException;

    /**
     * TODO: should implement review by mod before updating
     * Edits the given question.
     *
     * @param user the user requesting the question update
     * @param dto  QuestionPostDTO object containing the new question details
     * @return the updated question.
     * @throws ResourceNotFoundException if the question is not found
     * @throws BadRequestException       if the user is not permitted to edit the question
     */
    QuestionDTO questionUpdate(User user, QuestionUpdateDTO dto) throws ResourceNotFoundException, BadRequestException;

    /**
     * Deletes the given question.
     *
     * @param id   the id of the question
     * @param user the user requesting the question
     * @throws ResourceNotFoundException if the question is not found
     * @throws BadRequestException       if the user is not permitted to delete the question
     */
    void questionDelete(Integer id, User user) throws ResourceNotFoundException, BadRequestException;

    QuestionDTO questionBookmark(Integer id, Integer userId);

    QuestionDTO questionUnBookmark(Integer id, Integer userId);

    List<QuestionDTO> searchByKeyword(String content);

    List<QuestionDTO> searchByDisplayName(String displayName);

    boolean isQuestionUpvotedByUser(Integer questionId, Integer userId);

    boolean isQuestionDownvotedByUser(Integer questionId, Integer userId);

    boolean isQuestionBookmarkedByUser(Integer questionId, Integer userId);

}