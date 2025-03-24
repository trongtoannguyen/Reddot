package com.reddot.app.assembler.decorator;

import com.reddot.app.assembler.CommentAssembler;
import com.reddot.app.dto.response.CommentDTO;
import com.reddot.app.entity.Comment;
import com.reddot.app.entity.User;
import com.reddot.app.service.comment.CommentService;
import com.reddot.app.service.system.SystemAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public abstract class CommentMapperDecorator implements CommentAssembler {
    @Autowired
    @Qualifier("delegate")
    private CommentAssembler delegate;
    @Autowired
    @Lazy
    private CommentService commentService;

    @Override
    public CommentDTO toDTO(Comment comment) {
        CommentDTO dto = delegate.toDTO(comment);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (SystemAuthentication.isLoggedIn(authentication)) {
            User user = (User) authentication.getPrincipal();
            dto.setUpvoted(isCommentUpvoted(comment, user.getId()));
            dto.setDownvoted(isCommentDownvoted(comment, user.getId()));
        }
        return dto;
    }

    private Boolean isCommentUpvoted(Comment comment, Integer userId) {
        if (userId != null) {
            return commentService.isCommentUpvotedByUser(comment.getId(), userId);
        }
        return null;
    }

    private Boolean isCommentDownvoted(Comment comment, Integer userId) {
        if (userId != null) {
            return commentService.isCommentDownvotedByUser(comment.getId(), userId);
        }
        return null;
    }
}
