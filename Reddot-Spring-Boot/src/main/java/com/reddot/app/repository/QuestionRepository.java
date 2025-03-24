package com.reddot.app.repository;

import com.reddot.app.entity.Question;
import lombok.NonNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    @Query("SELECT COUNT(q) FROM questions q WHERE q.user.id = :userId")
    Long countQuestionsByUserId(@NonNull Integer userId); // userId kiểu Integer

    @Query("SELECT q.id AS questionId, q.title AS questionTitle, q.upvotes AS upvotes " +
           "FROM questions q " +
           "LEFT JOIN votes v ON v.question.id = q.id " +
           "WHERE q.user.id = :userId " +
           "GROUP BY q.id " +
           "ORDER BY upvotes DESC")
    List<Object[]> findTopQuestionsByVotesForUser(@Param("userId") Integer userId, Pageable pageable);

    // Đếm số câu hỏi theo ngày
    @Query("SELECT COUNT(q) FROM questions q WHERE DATE(q.createdAt) = :date")
    long countQuestionsByDay(@Param("date") LocalDate date);

    // Đếm số câu hỏi theo tuần (ISO week)
    @Query("SELECT COUNT(q) FROM questions q WHERE YEAR(q.createdAt) = :year AND WEEK(q.createdAt) = :week")
    long countQuestionsByWeek(@Param("year") int year, @Param("week") int week);

    // Đếm số câu hỏi theo tháng
    @Query("SELECT COUNT(q) FROM questions q WHERE YEAR(q.createdAt) = :year AND MONTH(q.createdAt) = :month")
    long countQuestionsByMonth(@Param("year") int year, @Param("month") int month);

    // Đếm số câu hỏi theo năm
    @Query("SELECT COUNT(q) FROM questions q WHERE YEAR(q.createdAt) = :year")
    long countQuestionsByYear(@Param("year") int year);

    //tìm câu hỏi trending theo tag: ví dụ top câu hỏi java hay nhất
    @Query(value = "SELECT q.id AS questionId, q.title AS questionTitle, " +
                   "(q.upvotes - q.downvotes) AS voteScore, t.name AS tagName " +
                   "FROM questions q " +
                   "JOIN question_tags qt ON q.id = qt.question_id " +
                   "JOIN tags t ON qt.tag_id = t.id " +
                   "WHERE t.name = :tagName " +
                   "ORDER BY voteScore DESC " +
                   "LIMIT 5", nativeQuery = true)
    List<Object[]> findTopQuestionsByTag(@Param("tagName") String tagName);

    boolean existsByIdAndVotes_UserIdAndVotes_VoteTypeId(Integer questionId, Integer userId, int direction);

    Long countUpvotesForQuestionsByUserId(Integer id);

    @Query("SELECT q FROM questions q WHERE (LOWER(q.title) LIKE %:content% OR LOWER(q.body) LIKE %:content%) AND q.status = 'PUBLIC'")
    List<Question> findByKeyword(@Param("content") String content);

    @Query("SELECT q FROM questions q WHERE LOWER(q.user.person.displayName) LIKE LOWER(CONCAT('%', :displayName, '%')) AND q.status = 'PUBLIC'")
    List<Question> findByDisplayName(@Param("displayName") String displayName);

    List<Question> findByUserId(Integer userId);
}