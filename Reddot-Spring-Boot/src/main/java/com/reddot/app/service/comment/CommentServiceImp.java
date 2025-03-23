package com.reddot.app.service.comment;

import com.reddot.app.assembler.CommentAssembler;
import com.reddot.app.dto.request.CommentPostDTO;
import com.reddot.app.dto.response.CommentDTO;
import com.reddot.app.entity.BaseEntity;
import com.reddot.app.entity.Comment;
import com.reddot.app.entity.Question;
import com.reddot.app.entity.User;
import com.reddot.app.entity.enumeration.ROLENAME;
import com.reddot.app.entity.enumeration.VOTETYPE;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.repository.CommentRepository;
import com.reddot.app.repository.QuestionRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

import static com.reddot.app.entity.BaseEntity.filterList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImp implements CommentService {
    private final CommentRepository commentRepository;
    private final QuestionRepository questionRepository;
    private final CommentAssembler commentAssembler;

    @Override
    public CommentDTO commentCreateOnQuestion(@NonNull User author, CommentPostDTO dto) throws ResourceNotFoundException {
        try {
            Question question = questionRepository.findById(dto.getId()).orElseThrow(() -> new ResourceNotFoundException("Question with id `" + dto.getId() + "` not found"));
            Comment comment = new Comment(dto.getBody(), author);
            question.addComment(comment);
            commentRepository.save(comment);
            return commentAssembler.toDTO(comment);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommentDTO commentReply(@NonNull User author, CommentPostDTO dto) throws ResourceNotFoundException {
        try {
            Comment parent = getCommentByIds(List.of(dto.getId())).getFirst();
            Comment reply = new Comment(dto.getBody(), author);
            parent.addReply(reply);
            commentRepository.save(reply);
            return commentAssembler.toDTO(reply);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<CommentDTO> commentGetAll() {
        try {
            return commentAssembler.toDTOList(filterPublicComment(getAllComments()));
        } catch (Exception e) {
            log.error("An error occurred while fetching comments", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<CommentDTO> commentGetAllWithUser(@NonNull User user) throws ResourceNotFoundException {
        try {
            Assert.notNull(user, "User cannot be null");
            List<Comment> comments = filterPublicComment(getAllComments());
            List<CommentDTO> dtoList = commentAssembler.toDTOList(comments);
            dtoList.forEach(dto -> {
                dto.setUpvoted(isCommentUpvotedByUser(dto.getCommentId(), user.getId()));
                dto.setDownvoted(isCommentDownvotedByUser(dto.getCommentId(), user.getId()));
            });
            return dtoList;
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<CommentDTO> commentGetByIds(List<Integer> ids) {
        try {
            List<Comment> comments = filterPublicComment(getCommentByIds(ids));
            return commentAssembler.toDTOList(comments);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<CommentDTO> commentGetByIdsWithUser(List<Integer> ids, @NonNull User user) throws ResourceNotFoundException {
        try {
            List<Comment> comments = filterPublicComment(getCommentByIds(ids));
            List<CommentDTO> dtoList = commentAssembler.toDTOList(comments);

            // custom logic for user-specific properties
            dtoList.forEach(dto -> {
                dto.setUpvoted(isCommentUpvotedByUser(dto.getCommentId(), user.getId()));
                dto.setDownvoted(isCommentDownvotedByUser(dto.getCommentId(), user.getId()));
            });
            return dtoList;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public CommentDTO commentUpdate(User user, CommentPostDTO dto) throws ResourceNotFoundException, BadRequestException {
        try {
            Comment comment = getCommentByIds(List.of(dto.getId())).getFirst();
            //FIXME: FOLLOWING HELPER METHODS SHOULD BE REFACTORED INTO A SEPARATE SERVICE
            boolean isOwner = isOwner(user, comment);
            boolean isSuperUser = isSuperUser(user);
            if (!isOwner && !isSuperUser) {
                throw new BadRequestException("You are not permitted to edit this comment");
            }
            comment.setText(dto.getBody());
            commentRepository.save(comment);
            return commentAssembler.toDTO(comment);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void commentDelete(Integer id, User user) throws ResourceNotFoundException, BadRequestException {
        try {
            Assert.notNull(user, "User cannot be null");
            Comment comment = getCommentByIds(List.of(id)).getFirst();
            boolean isOwner = isOwner(user, comment);
            boolean isSuperUser = isSuperUser(user);
            if (!(isOwner || isSuperUser)) {
                throw new BadRequestException("You are not permitted to delete this comment");
            }
            comment.softRm();
            commentRepository.save(comment);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Boolean isCommentUpvotedByUser(Integer commentId, Integer userId) {
        return commentRepository.existsByIdAndVotes_UserIdAndVotes_VoteTypeId(commentId, userId, VOTETYPE.UPVOTE.getDirection());
    }

    @Override
    public Boolean isCommentDownvotedByUser(Integer commentId, Integer userId) {
        return commentRepository.existsByIdAndVotes_UserIdAndVotes_VoteTypeId(commentId, userId, VOTETYPE.DOWNVOTE.getDirection());
    }

    @Override
    public List<Comment> filterPublicComment(List<Comment> comments) {
        return filterList(comments, BaseEntity::isPublic);
    }

    private boolean isSuperUser(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(ROLENAME.ROLE_ADMIN)
                                                         || role.getName().equals(ROLENAME.ROLE_MODERATOR));
    }

    private boolean isOwner(User user, Comment comment) {
        assert user.getId() != null;
        return user.getId().equals(comment.getUser().getId());
    }

    private List<Comment> getAllComments() {
        return commentRepository.findAll();
    }

    private List<Comment> getCommentByIds(List<Integer> ids) {
        return commentRepository.findAllById(ids);
    }
}
