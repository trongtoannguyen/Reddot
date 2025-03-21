package com.reddot.app.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
public class CommentDTO {
    private Integer questionId;
    private Integer commentId;
    private Integer responseTo;
    private String status;
    private String text;
    private LocalDateTime creationDate;
    private LocalDateTime lastEditDate;
    private int upvotes;
    private int downvotes;
    private int score;
    private Boolean upvoted;
    private Boolean downvoted;
    private ShallowUserDTO author;
}