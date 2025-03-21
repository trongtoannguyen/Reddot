package com.reddot.app.controller;

import com.reddot.app.dto.request.CommentPostDTO;
import com.reddot.app.dto.request.QuestionCreateDTO;
import com.reddot.app.dto.request.QuestionUpdateDTO;
import com.reddot.app.dto.response.CommentDTO;
import com.reddot.app.dto.response.QuestionDTO;
import com.reddot.app.dto.response.ServiceResponse;
import com.reddot.app.entity.User;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.service.comment.CommentService;
import com.reddot.app.service.question.QuestionService;
import com.reddot.app.service.system.SystemAuthentication;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;
    private final CommentService commentService;

    @Operation(summary = "Creates a comment on the given question. [auth required] ",
            description = """
                    In the API, any "low quality" checks that are triggered cause the write request to fail.
                    This includes situations where a CAPTCHA or guidance text would be displayed on the websites.
                    
                    Use an access_token with role_user to create a new comment.
                    
                    This method returns the created comment.
                    """)
    @PostMapping("/{id}/comments/add")
    public ResponseEntity<ServiceResponse<CommentDTO>> addComment(@PathVariable Integer id, @RequestBody CommentPostDTO dto) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        dto.setId(id);
        CommentDTO commentDTO = commentService.commentCreateOnQuestion(user, dto);
        return ResponseEntity.ok(new ServiceResponse<>(200, "Comment created successfully", commentDTO));
    }

    @Operation(summary = "Create a new question", description = """
            In the API, any "low quality" checks that are triggered cause the write request to fail.
            This includes situations where a CAPTCHA or guidance text would be displayed on the websites.
            
            Use an access_token with role_user to create a new question.
            
            This method returns the created question.""")
    @PostMapping("/add")
    public ResponseEntity<ServiceResponse<QuestionDTO>> createQuestion(@Valid @RequestBody QuestionCreateDTO dto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!SystemAuthentication.isLoggedIn(authentication)) {
                throw new BadRequestException("You must be logged in to create a question");
            }
            User user = (User) authentication.getPrincipal();
            QuestionDTO questionDTO = questionService.questionCreate(user, dto);
            return ResponseEntity.ok(new ServiceResponse<>(200, "Question created successfully", questionDTO));
        } catch (BadRequestException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Gets all the questions on the site.",
            description = """
                    Use this method to retrieve all questions include some user-specific properties related to the questions.
                    
                    This method returns a list of questions.""")
    @GetMapping
    public ResponseEntity<ServiceResponse<List<QuestionDTO>>> getAllQuestions() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            List<QuestionDTO> list;
            if (SystemAuthentication.isLoggedIn(authentication)) {
                User user = (User) authentication.getPrincipal();
                list = questionService.questionGetAllWithUser(user, false);
            } else {
                list = questionService.questionGetAll();
            }
            return ResponseEntity.ok(new ServiceResponse<>(200, "Questions retrieved successfully", list));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Operation(summary = "Returns questions identified in {ids}.",
            description = """
                    Use this method to retrieve a list of questions by a list of ids, include some user-specific properties related to the question.
                    
                    This method returns a list of questions.""")
    @GetMapping("/{ids}")
    public ResponseEntity<ServiceResponse<List<QuestionDTO>>> getQuestionByIds(@PathVariable List<Integer> ids) {
        try {
            List<QuestionDTO> dtos;
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (SystemAuthentication.isLoggedIn(authentication)) {
                User user = (User) authentication.getPrincipal();
                dtos = questionService.questionGetByIdsWithUser(ids, user);
            } else {
                dtos = questionService.questionGetByIds(ids, false);
            }
            return ResponseEntity.ok(new ServiceResponse<>(200, "Question retrieved successfully", dtos));
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Operation(summary = "Edit an existing question. [auth required]",
            description = """
                    Use this method to edit a question identified by its ID.
                    
                    This method returns the updated question.""")
    @PutMapping("/{id}/update")
    public ResponseEntity<ServiceResponse<QuestionDTO>> updateQuestion(@PathVariable Integer id, @RequestBody QuestionUpdateDTO dto) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            dto.setId(id);
            QuestionDTO questionDTO = questionService.questionUpdate(user, dto);
            return ResponseEntity.ok(new ServiceResponse<>(200, "Question updated successfully", questionDTO));
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Operation(summary = "Deletes the question identified in {id}. [auth required]",
            description = """
                    Note that only question owners can delete their questions, this is not equivalent to casting a delete vote.
                    Also be aware that there are conditions when even a question's owner cannot delete it, and this method will respect those limits.
                    
                    Use an access_token to delete a question.
                    
                    It is not possible to undelete a question.
                    
                    In practice, this method will never return an object.
                    """)
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ServiceResponse<String>> deleteQuestion(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        questionService.questionDelete(id, user);
        return ResponseEntity.ok(new ServiceResponse<>(200, "Question deleted successfully", "Question deleted successfully"));
    }

    @GetMapping("/search")
    public List<QuestionDTO> searchQuestions(
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "displayName", required = false) String displayName) {
        if (content != null && !content.isBlank()) {
            return questionService.searchByKeyword(content);
        } else if (displayName != null && !displayName.isBlank()) {
            return questionService.searchByDisplayName(displayName);
        } else {
            return questionService.questionGetAll();
        }
    }

    @Operation(summary = "Bookmarks the question identified in {id}. [auth required]",
            description = """
                    Use this method to bookmark a question.
                    
                    To undo bookmarking a question use the /questions/{id}/bookmark/undo method.
                    
                    Use an access_token to bookmark a question.
                    
                    This method returns the bookmarked question.
                    """)
    @PostMapping("/{id}/bookmark")
    public ResponseEntity<ServiceResponse<QuestionDTO>> bookmarkQuestion(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        QuestionDTO dto = questionService.questionBookmark(id, user.getId());
        return ResponseEntity.ok(new ServiceResponse<>(200, "Question bookmarked successfully", dto));
    }

    @Operation(summary = "Unbookmarks the question identified in {id}. [auth required]",
            description = """
                    Use this method to unbookmark a question.
                    
                    To bookmark a question use the /questions/{id}/bookmark method.
                    
                    Use an access_token to unbookmark a question.
                    
                    This method returns the unbookmarked question.
                    """)
    @PostMapping("/{id}/bookmark/undo")
    public ResponseEntity<ServiceResponse<QuestionDTO>> unBookmarkQuestion(@PathVariable Integer id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User user = (User) authentication.getPrincipal();
        QuestionDTO dto = questionService.questionUnBookmark(id, user.getId());
        return ResponseEntity.ok(new ServiceResponse<>(200, "Question unbookmarked successfully", dto));
    }
}