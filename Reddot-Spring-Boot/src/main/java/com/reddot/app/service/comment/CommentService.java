package com.reddot.app.service.comment;

import com.reddot.app.dto.request.CommentPostDTO;
import com.reddot.app.dto.response.CommentDTO;
import com.reddot.app.entity.Comment;
import com.reddot.app.entity.User;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.ResourceNotFoundException;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface CommentService {
    /**
     * Create a new comment on the given question.
     * Auth required to create a new comment.
     *
     * @return This method returns the created comment.
     * @throws ResourceNotFoundException if the author or question is not found
     */
    CommentDTO commentCreateOnQuestion(@NonNull User author, CommentPostDTO dto) throws ResourceNotFoundException;

    /**
     * Create a new comment on the given comment.
     * Auth required to create a new comment.
     *
     * @return This method returns the created comment.
     * @throws ResourceNotFoundException if the author or comment is not found
     */
    CommentDTO commentReply(@NonNull User author, CommentPostDTO dto) throws ResourceNotFoundException;

    /**
     * Get all comments on the site.
     *
     * @return a list of CommentDTO objects containing the comment details.
     */
    List<CommentDTO> commentGetAll();

    /**
     * Get all comments on the site with user-specific properties.
     *
     * @param user the user requesting the comments.
     * @return a list of CommentDTO objects containing the comment details.
     * @throws ResourceNotFoundException if the user is not found
     */
    List<CommentDTO> commentGetAllWithUser(@NonNull User user) throws ResourceNotFoundException;

    /**
     * Get the comments identified by a set of comment ids.
     *
     * @param ids a set ids of the comments.
     * @return a list of CommentDTO objects containing the comment details.
     */
    List<CommentDTO> commentGetByIds(List<Integer> ids);

    /**
     * Get the comment identified by a set of comment ids with user-specific properties.
     *
     * @param ids  a set ids of the comments.
     * @param user the user requesting the comments.
     * @return a list of CommentDTO objects containing the comment details.
     */
    List<CommentDTO> commentGetByIdsWithUser(List<Integer> ids, @NonNull User user) throws ResourceNotFoundException;

    CommentDTO commentUpdate(User user, CommentPostDTO dto) throws ResourceNotFoundException, BadRequestException;

    void commentDelete(Integer id, User user) throws ResourceNotFoundException, BadRequestException;

    Boolean isCommentUpvotedByUser(Integer commentId, Integer userId);

    Boolean isCommentDownvotedByUser(Integer commentId, Integer userId);

    List<Comment> filterPublicComment(List<Comment> comments);
}
