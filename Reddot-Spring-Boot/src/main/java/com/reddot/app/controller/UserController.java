package com.reddot.app.controller;

import com.reddot.app.dto.request.SearchCriteria;
import com.reddot.app.dto.response.BookmarkDTO;
import com.reddot.app.dto.response.PaginatedResponse;
import com.reddot.app.dto.response.QuestionDTO;
import com.reddot.app.dto.response.ServiceResponse;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.service.bookmark.BookmarkService;
import com.reddot.app.service.question.QuestionService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final QuestionService questionService;
    private final BookmarkService bookmarkService;

    public UserController(QuestionService questionService, BookmarkService bookmarkService) {
        this.questionService = questionService;
        this.bookmarkService = bookmarkService;
    }

    /**
     * Get the questions that users in {ids} have bookmarked.
     *
     * @param ids List of user ids
     * @return List of questions that users have bookmarked
     */
    @GetMapping("/{ids}/bookmarks")
    public ResponseEntity<ServiceResponse<PaginatedResponse<BookmarkDTO>>> getUserBookmarks(
            @PathVariable("ids") List<Integer> ids,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false, defaultValue = "id") String orderBy,
            @RequestParam(required = false, defaultValue = "ASC") String sort) {
        try {
            SearchCriteria criteria = new SearchCriteria(page, size, orderBy, sort);
            Page<BookmarkDTO> bookmarks = bookmarkService.bookmarkGetAllByUserIds(criteria, ids);
            return new ResponseEntity<>(
                    new ServiceResponse<>(
                            HttpStatus.OK.value(),
                            "Retrieved bookmarks successfully",
                            new PaginatedResponse<>(bookmarks)
                    ),
                    HttpStatus.OK
            );
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }

    @GetMapping("/{userId}/questions")
    public ResponseEntity<ServiceResponse<List<QuestionDTO>>> getUserQuestions(
            @PathVariable Integer userId,
            @RequestParam(required = false, defaultValue = "score") String sort) {
        try {
            List<QuestionDTO> questions = questionService.questionGetAllByUserId(userId, sort);
            return new ResponseEntity<>(
                    new ServiceResponse<>(
                            HttpStatus.OK.value(),
                            "Retrieved questions successfully",
                            questions
                    ),
                    HttpStatus.OK
            );
        } catch (ResourceNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
    }
}

