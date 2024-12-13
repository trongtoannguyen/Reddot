package com.reddot.app.repository;

import com.reddot.app.entity.Vote;
import lombok.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VoteRepository extends JpaRepository<Vote, Integer> {
    // Tổng số upvotes cho các câu hỏi mà người dùng sở hữu
    @Query("SELECT SUM(q.upvotes) FROM questions q WHERE q.user.id = :userId")
    Long countUpvotesForQuestionsByUserId(@NonNull Integer userId);

    // Tổng số downvotes cho các câu hỏi mà người dùng sở hữu
    @Query("SELECT SUM(q.downvotes) FROM questions q WHERE q.user.id = :userId")
    Long countDownvotesForQuestionsByUserId(@NonNull Integer userId);

    // Đếm số upvotes cho các câu trả lời (comments) mà người dùng sở hữu
    @Query("SELECT COUNT(v) FROM votes v " +
            "JOIN v.comment c " +
            "WHERE c.user.id = :userId AND v.voteType.id = 1")
    Long countUpvotesForCommentsByUserId(@NonNull Integer userId);

    // Đếm số downvotes cho các câu trả lời (comments) mà người dùng sở hữu
    @Query("SELECT COUNT(v) FROM votes v " +
            "JOIN v.comment c " +
            "WHERE c.user.id = :userId AND v.voteType.id = 2")
    Long countDownvotesForCommentsByUserId(@NonNull Integer userId);
}
