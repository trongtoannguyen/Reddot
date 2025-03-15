package com.reddot.app.service.question;

import com.reddot.app.assembler.QuestionAssembler;
import com.reddot.app.dto.request.QuestionCreateDTO;
import com.reddot.app.dto.request.QuestionUpdateDTO;
import com.reddot.app.dto.response.QuestionDTO;
import com.reddot.app.entity.Question;
import com.reddot.app.entity.Tag;
import com.reddot.app.entity.User;
import com.reddot.app.entity.enumeration.ROLENAME;
import com.reddot.app.entity.enumeration.VOTETYPE;
import com.reddot.app.exception.BadRequestException;
import com.reddot.app.exception.ResourceNotFoundException;
import com.reddot.app.repository.QuestionRepository;
import com.reddot.app.repository.TagRepository;
import com.reddot.app.repository.UserRepository;
import com.reddot.app.service.bookmark.BookmarkService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
//TODO: implement filter hidden question method
public class QuestionServiceImp implements QuestionService {
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAssembler questionAssembler;
    private final TagRepository tagRepository;
    private final BookmarkService bookmarkService;

    private static boolean isOwner(User user, Question question) {
        if (user == null || question == null) {
            return false;
        }
        return question.getUser().getId().equals(user.getId());
    }

    private static boolean isSuperUser(User user) {
        if (user == null) {
            return false;
        }
        return user.getRoles().stream().anyMatch(role -> role.getName().equals(ROLENAME.ROLE_ADMIN)
                                                         || role.getName().equals(ROLENAME.ROLE_MODERATOR));
    }

    /**
     * Create a new question with a set of maximum 5 tags.
     *
     * @param creator the id of the user creating the question
     * @param dto     QuestionPostDTO object containing the question details
     * @return QuestionDTO the created question
     */
    @Transactional
    @Override
    public QuestionDTO questionCreate(User creator, QuestionCreateDTO dto) throws ResourceNotFoundException {
        try {
            Assert.notNull(creator, "User cannot be null");
            // question can have at most 5 tags
            Set<Tag> tags = new HashSet<>(5);
            if (!dto.getTags().isEmpty()) {
                for (String tagString : dto.getTags()) {
                    Tag tag = getTagByName(tagString);

                    // increment the number of tag usages
                    tag.incrementTagged();
                    tags.add(tag);
                }
                tagRepository.saveAll(tags);
            }
            Question question = Question.builder()
                    .body(dto.getBody())
                    .title(dto.getTitle())
                    .tags(tags)
                    .user(creator)
                    .build();
            questionRepository.save(question);
            QuestionDTO dto1 = questionAssembler.toDTO(question);

            // custom logic for user-specific properties
            dto1.setUpvoted(isQuestionUpvotedByUser(question.getId(), creator.getId()));
            dto1.setDownvoted(isQuestionDownvotedByUser(question.getId(), creator.getId()));
            dto1.setBookmarked(isQuestionBookmarkedByUser(question.getId(), creator.getId()));
            return dto1;
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<QuestionDTO> questionGetByIds(List<Integer> ids) {
        try {
            List<Question> list = getQuestionByIds(ids);
            return questionAssembler.toDTOList(list);
        } catch (Exception e) {
            log.error("An error occurred while retrieving the question: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<QuestionDTO> questionGetByIdsWithUser(List<Integer> ids, @NonNull User user) throws ResourceNotFoundException {
        try {
            List<Question> list = getQuestionByIds(ids);
            return getQuestionDTOS(user, list);
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while retrieving the question: ", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<QuestionDTO> questionGetAll() {
        try {
            List<Question> list = questionRepository.findAll().stream()
                    .filter(question -> question.getVisibility() != Question.Visibility.PRIVATE)
                    .collect(Collectors.toList());
            return questionAssembler.toDTOList(list);
        } catch (Exception e) {
            log.error("An error occurred while retrieving the questions", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<QuestionDTO> questionGetAllWithUser(@NonNull User user) throws ResourceNotFoundException {
        try {
            Assert.notNull(user, "User cannot be null");

            // get non-private or their own question
            List<Question> list = questionRepository.findAll().stream()
                    .filter(question -> question.getVisibility() != Question.Visibility.PRIVATE
                                        || question.getUser().getId().equals(user.getId()))
                    .toList();
            return getQuestionDTOS(user, list);
        } catch (ResourceNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while retrieving the questions", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public QuestionDTO questionUpdate(User user, QuestionUpdateDTO dto) throws ResourceNotFoundException, BadRequestException {
        try {
            Question question = getSingleQuestionById(dto.getId());

            // Check if the user is the owner, admin, or moderator
            boolean isOwner = isOwner(user, question);
            boolean isSuperUser = isSuperUser(user);
            if (!isOwner && !isSuperUser) {
                throw new BadRequestException("You are not permitted to edit this question");
            }
            question.setBody(dto.getBody());
            question.setTitle(dto.getTitle());
            Set<String> newTags = new HashSet<>(dto.getTags());
            List<String> newTagStrings = new ArrayList<>(newTags);

            // get the old tags
            Set<Tag> tags = question.getTags();
            List<String> tagStrings = new ArrayList<>();
            tags.stream().map(Tag::getName).forEach(tagStrings::add);

            // compare the tags with the new tags then increment tag usage
            for (String newTagStr : newTagStrings) {
                if (!tagStrings.contains(newTagStr)) {
                    Tag tag = getTagByName(newTagStr);
                    tag.incrementTagged();
                }
            }
            tags.clear();
            newTagStrings.forEach(newTagStr -> tags.add(getTagByName(newTagStr)));
            question.setTags(tags);
            questionRepository.save(question);
            return questionAssembler.toDTO(question);
        } catch (BadRequestException | ResourceNotFoundException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while updating the question", e);
            throw new RuntimeException(e);
        }

    }

    @Override
    public void questionDelete(Integer questionId, User user) throws ResourceNotFoundException, BadRequestException {
        try {
            Assert.notNull(user, "User cannot be null");
            Question question = getSingleQuestionById(questionId);

            // Check if the user is the owner, admin, or moderator
            boolean isOwner = isOwner(user, question);
            boolean isSuperUser = isSuperUser(user);
            if (!isOwner && !isSuperUser) {
                throw new BadRequestException("You are not permitted to delete this question");
            }
            if (question.isClosed()) {
                throw new BadRequestException("You cannot delete a closed question");
            }
            questionRepository.delete(question);
        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while deleting the question", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public QuestionDTO questionBookmark(Integer id, Integer userId) {
        try {
            return bookmarkService.bookmarkQuestion(userId, id);
        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while bookmarking the question", e);
            throw e;
        }
    }

    @Override
    public QuestionDTO questionUnBookmark(Integer id, Integer userId) {
        try {
            return bookmarkService.unBookmarkQuestion(userId, id);
        } catch (ResourceNotFoundException | BadRequestException e) {
            log.error(e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("An error occurred while un-bookmaking the question", e);
            throw e;
        }
    }

    @Override
    public boolean isQuestionUpvotedByUser(Integer questionId, Integer userId) {
        return questionRepository.existsByIdAndVotes_UserIdAndVotes_VoteTypeId(questionId, userId, VOTETYPE.UPVOTE.getDirection());
    }

    @Override
    public boolean isQuestionDownvotedByUser(Integer questionId, Integer userId) {
        return questionRepository.existsByIdAndVotes_UserIdAndVotes_VoteTypeId(questionId, userId, VOTETYPE.DOWNVOTE.getDirection());
    }

    @Override
    public boolean isQuestionBookmarkedByUser(Integer questionId, Integer userId) {
        return bookmarkService.isBookmarkedByUser(userId, questionId);
    }

    @Override
    public List<QuestionDTO> searchByKeyword(String content) {
        List<Question> questions = questionRepository.findByKeyword(content.toLowerCase());

        questions = questions.stream()
                .filter(question -> question.getVisibility() == Question.Visibility.PUBLIC)
                .toList();

        return questions.stream().map(questionAssembler::toDTO).collect(Collectors.toList());
    }

    @Override
    public List<QuestionDTO> searchByDisplayName(String displayName) {
        List<Question> questions = questionRepository.findByDisplayName(displayName.toLowerCase());

        questions = questions.stream()
                .filter(question -> question.getVisibility() == Question.Visibility.PUBLIC)
                .toList();

        return questions.stream().map(questionAssembler::toDTO).collect(Collectors.toList());
    }

    /**
     * Get all questions on the site with user id.
     *
     * @param userId the id of the user
     * @param sort   the sorting order, default is by score.
     * @return
     */
    @Override
    public List<QuestionDTO> questionGetAllByUserId(Integer userId, String sort) {
        User loggedInUser = getUserById(userId);
        if (loggedInUser == null) {
            throw new ResourceNotFoundException("User with id " + userId + " not found");
        }

        List<Question> questions = questionRepository.findByUserId(userId).stream()
                .filter(question -> question.getVisibility() == Question.Visibility.PUBLIC)
                .collect(Collectors.toList());

        if ("score".equalsIgnoreCase(sort)) {
            questions.sort((q1, q2) -> Integer.compare(q2.getScore(), q1.getScore()));
        } else if ("newest".equalsIgnoreCase(sort)) {
            questions.sort((q1, q2) -> q2.getCreatedAt().compareTo(q1.getCreatedAt()));
        } else {
            questions.sort((q1, q2) -> Integer.compare(q2.getScore(), q1.getScore()));
        }

        return getQuestionDTOS(loggedInUser, questions);
    }

    @Override
    public QuestionDTO toggleVisibility(Integer questionId, Integer userId) throws ResourceNotFoundException, BadRequestException {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question Not Found"));

        if (!question.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not permitted to change this question");
        }

        if (question.getVisibility() == Question.Visibility.PUBLIC) {
            question.setVisibility(Question.Visibility.PRIVATE);
        } else {
            question.setVisibility(Question.Visibility.PUBLIC);
        }

        questionRepository.save(question);

        return questionAssembler.toDTO(question);
    }

    private Question getSingleQuestionById(Integer id) {
        return questionRepository.findById(id).filter(question -> question.getVisibility() != Question.Visibility.PRIVATE).orElseThrow(() -> new ResourceNotFoundException("Question with id `" + id + "` not found"));
    }

    private List<Question> getQuestionByIds(List<Integer> ids) {
        return questionRepository.findAllById(ids);
    }

    private List<QuestionDTO> getQuestionDTOS(@NonNull User user, List<Question> list) {
        List<QuestionDTO> dtoList = questionAssembler.toDTOList(list);
        dtoList.forEach(questionDTO -> {
            questionDTO.setUpvoted(isQuestionUpvotedByUser(questionDTO.getQuestionId(), user.getId()));
            questionDTO.setDownvoted(isQuestionDownvotedByUser(questionDTO.getQuestionId(), user.getId()));
            questionDTO.setBookmarked(isQuestionBookmarkedByUser(questionDTO.getQuestionId(), user.getId()));
        });
        return dtoList;
    }

    private Tag getTagByName(String tagString) {
        return tagRepository.findByName(tagString).orElseThrow(() -> new ResourceNotFoundException("Tag with name `" + tagString + "` not found"));
    }

    private User getUserById(Integer userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User with id `" + userId + "` not found"));
    }

}