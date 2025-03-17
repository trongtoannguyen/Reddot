package com.reddot.app.controller;

import com.reddot.app.dto.request.CommentPostDTO;
import com.reddot.app.dto.response.CommentDTO;
import com.reddot.app.dto.response.ServiceResponse;
import com.reddot.app.entity.User;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.service.comment.CommentService;
import com.reddot.app.service.system.SystemAuthentication;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Reply a given comment. [auth required]",
            description = """
                    Reply to an existing comment given by id path.
                    
                    This method returns the replied comment.
                    """)
    @PostMapping("{id}/reply")
    public ResponseEntity<ServiceResponse<CommentDTO>> replyComment(@PathVariable Integer id, @RequestBody CommentPostDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        dto.setId(id);
        CommentDTO reply = commentService.commentReply(user, dto);
        return ResponseEntity.ok(new ServiceResponse<>(200, "Comment replied successfully", reply));
    }

    @Operation(summary = "Get all comments on the site.",
            description = "This method returns a list of undeleted comments on the site.")
    @GetMapping
    public ResponseEntity<ServiceResponse<List<CommentDTO>>> getAllComments() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            List<CommentDTO> list;
            if (SystemAuthentication.isLoggedIn(authentication)) {
                User user = (User) authentication.getPrincipal();
                list = commentService.commentGetAllWithUser(user);
            } else {
                list = commentService.commentGetAll();
            }
            return ResponseEntity.ok(new ServiceResponse<>(200, "Comment retrieved successfully", list));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Operation(summary = "Returns the comments identified in {ids}.",
            description = "This method is most useful if you have a cache of comment ids obtained through other" +
                          " means (such as /questions/{id}/comments) but suspect the data may be stale.")
    @GetMapping("/{ids}")
    public ResponseEntity<ServiceResponse<List<CommentDTO>>> getCommentByIds(@PathVariable List<Integer> ids) {
        try {
            List<CommentDTO> dto;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (SystemAuthentication.isLoggedIn(authentication)) {
                User user = (User) authentication.getPrincipal();
                dto = commentService.commentGetByIdsWithUser(ids, user);
            } else {
                dto = commentService.commentGetByIds(ids);
            }
            return ResponseEntity.ok(new ServiceResponse<>(200, "Comment retrieved successfully", dto));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Update a comment identified by {id}. [auth required]",
            description = """
                    Edit an existing comment.
                    
                    This method returns the updated comment.
                    """)
    @PutMapping("/{id}/update")
    public ResponseEntity<ServiceResponse<CommentDTO>> updateComment(@PathVariable Integer id, @RequestBody CommentPostDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        dto.setId(id);
        CommentDTO commentDTO = commentService.commentUpdate(user, dto);
        return ResponseEntity.ok(new ServiceResponse<>(200, "Comment updated successfully", commentDTO));
    }

    @Operation(summary = " Delete a comment identified by its id. [auth required]",
            description = """
                    Delete an existing comment.
                    
                    In practice, this method never return an object, but it returns a success message. 
                    """)
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ServiceResponse<String>> deleteComment(@PathVariable Integer id) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            commentService.commentDelete(id, user);
            return ResponseEntity.ok(new ServiceResponse<>(200, "Comment deleted successfully", "Comment deleted successfully"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
