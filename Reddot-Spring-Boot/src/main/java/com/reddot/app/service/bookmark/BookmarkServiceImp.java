package com.reddot.app.service.bookmark;

import com.reddot.app.assembler.BookmarkAssembler;
import com.reddot.app.assembler.QuestionAssembler;
import com.reddot.app.dto.request.SearchCriteria;
import com.reddot.app.dto.response.BookmarkDTO;
import com.reddot.app.dto.response.QuestionDTO;
import com.reddot.app.entity.Bookmark;
import com.reddot.app.entity.Question;
import com.reddot.app.entity.User;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.repository.BookmarkRepository;
import com.reddot.app.repository.QuestionRepository;
import com.reddot.app.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
public class BookmarkServiceImp implements BookmarkService {
    private static final String ERROR_OCCURRED_BY = "Error occurred by user with id: ";
    private final QuestionRepository questionRepository;
    private final QuestionAssembler questionAssembler;
    private final BookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final BookmarkAssembler bookmarkAssembler;

    public BookmarkServiceImp(QuestionRepository questionRepository, QuestionAssembler questionAssembler, BookmarkRepository bookmarkRepository, UserRepository userRepository, BookmarkAssembler bookmarkAssembler) {
        this.questionRepository = questionRepository;
        this.questionAssembler = questionAssembler;
        this.bookmarkRepository = bookmarkRepository;
        this.userRepository = userRepository;
        this.bookmarkAssembler = bookmarkAssembler;
    }

    @Override
    @Transactional
    public QuestionDTO bookmarkQuestion(Integer userId, Integer questionId) {
        try {
            Question question = questionRepository.findById(questionId).orElseThrow(ResourceNotFoundException::new);
            if (!bookmarkRepository.existsByUser_IdAndQuestion_Id(userId, questionId)) {

                // we can call user.getBookmarks() to initialize the lazy loading property, but it will make 2 queries to the database, not an ideal to optimize the performance.
                User user = userRepository.findByIdAndFetchBookmarksEagerly(userId).orElseThrow(() -> new ResourceNotFoundException("User with id `" + userId + "` not found"));
                Bookmark bookmark = new Bookmark(user, question);
                user.getBookmarks().add(bookmark);
                bookmarkRepository.save(bookmark);
            } else {
                log.error(ERROR_OCCURRED_BY + userId);
                throw new BadRequestException("Error occurred related to status of bookmarking question");
            }
            return questionAssembler.toDTO(question);
        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage(), e.getCause());
        }
    }

    @Override
    @Transactional
    public QuestionDTO unBookmarkQuestion(Integer userId, Integer questionId) {
        try {
            Question question = questionRepository.findById(questionId).orElseThrow(ResourceNotFoundException::new);
            if (bookmarkRepository.existsByUser_IdAndQuestion_Id(userId, questionId)) {
                User user = userRepository.findByIdAndFetchBookmarksEagerly(userId).orElseThrow(() -> new ResourceNotFoundException("User with id `" + userId + "` not found"));
                Bookmark bookmark = bookmarkRepository.findByUser_IdAndQuestion_Id(userId, questionId).orElseThrow(ResourceNotFoundException::new);
                user.getBookmarks().remove(bookmark);
                bookmarkRepository.delete(bookmark);
            } else {
                log.error(ERROR_OCCURRED_BY + userId);
                throw new BadRequestException("Error occurred related to status of un-bookmarking target question");
            }
            return questionAssembler.toDTO(question);
        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Page<BookmarkDTO> bookmarkGetAllByUserIds(SearchCriteria criteria, List<Integer> uIds) {
        try {
            Pageable pageable = createPageable(criteria);
            Page<Bookmark> bookmarks = bookmarkRepository.findByUser_IdIn(uIds, pageable);
            return bookmarks.map(bookmarkAssembler::toBookmarkDTO);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private Pageable createPageable(SearchCriteria criteria) {
        Sort sort = criteria.getSortDirection().equalsIgnoreCase(Sort.Direction.ASC.name()) ? Sort.by(criteria.getOrderBy()).ascending()
                : Sort.by(criteria.getOrderBy()).descending();
        return PageRequest.of(criteria.getPageNo() - 1, criteria.getPageSize(), sort);
    }

    @Override
    public boolean isBookmarkedByUser(Integer userId, Integer questionId) {
        return bookmarkRepository.existsByUser_IdAndQuestion_Id(userId, questionId);
    }
}
