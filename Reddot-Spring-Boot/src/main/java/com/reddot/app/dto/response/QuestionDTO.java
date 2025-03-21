package com.reddot.app.dto.response;

import com.reddot.app.entity.enumeration.STATUS;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This type represents a question on the site.
 * This type is heavily inspired by the question page itself, and can optionally return comments accordingly.
 * The upvoted, downvoted, and bookmarked fields can only be queried for with an access_token.
 */
@Getter
@Setter
@ToString
public class QuestionDTO {
    // TODO: Design Improvements: Avoid Direct Entity Usage in DTOs (CommentDTO)
    private Integer questionId;
    private STATUS status;
    private String title;
    private String body;
    private LocalDateTime creationDate;
    private LocalDateTime lastEditDate;
    private LocalDateTime closeDate;
    private int upvotes;
    private int downvotes;
    private int score;
    private int commentCount;
    private boolean upvoted;
    private boolean downvoted;
    private boolean bookmarked;
    private Set<TagDTO> tags = new HashSet<>();
    private List<CommentDTO> commentList = new ArrayList<>();
    private ShallowUserDTO author;
}
