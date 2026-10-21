
package com.tenco.spring_blog.reply;

import com.tenco.spring_blog.board.Board; // 확인 필요: Board 엔티티 패키지
import com.tenco.spring_blog.board.BoardJpaRepository;
import com.tenco.spring_blog.user.User; // 확인 필요: User 엔티티 패키지
import com.tenco.spring_blog.user.UserJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReplyJpaRepositoryTest {

    @Autowired
    private ReplyJpaRepository replyJpaRepository;

    @Autowired
    private BoardJpaRepository boardJpaRepository;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager em;

    // 테스트용 사용자 저장
    private User 사용자저장(String username) {
        User user = User.builder() // 확인 필요: User에 빌더가 있다고 가정
                .username(username) // 확인 필요: username 필드가 있다고 가정
                .password("encoded-password") // 확인 필요: password 필드가 있다고 가정
                .email(username + "@example.com") // 확인 필요: email 필드가 있다고 가정
                .build();

        return userJpaRepository.save(user);
    }

    // 테스트용 게시글 저장
    private Board 게시글저장(User user, String title) {
        Board board = Board.builder() // 확인 필요: Board에 빌더가 있다고 가정
                .title(title) // 확인 필요: title 필드가 있다고 가정
                .content("테스트 게시글 내용") // 확인 필요: content 필드가 있다고 가정
                .user(user) // 확인 필요: Board에 user 연관관계가 있다고 가정
                .build();

        return boardJpaRepository.save(board);
    }

    // 테스트용 댓글 저장
    private Reply 댓글저장(Board board, User user, String content) {
        Reply reply = Reply.builder() // 확인 필요: Reply에 빌더가 있다고 가정
                .board(board) // 확인 필요: Reply에 board 연관관계가 있다고 가정
                .user(user) // 확인 필요: Reply에 user 연관관계가 있다고 가정
                .content(content) // 확인 필요: 댓글 내용 필드가 content라고 가정
                .build();

        return replyJpaRepository.save(reply);
    }

    @Test
    void findAllByBoardIdJoinUser_댓글존재_테스트() {
        // given: 게시글과 해당 게시글에 달린 댓글을 저장한다.
        User user = 사용자저장("ssar");
        Board board = 게시글저장(user, "테스트 게시글");
        댓글저장(board, user, "첫 번째 댓글");

        em.flush();
        em.clear();

        // when: 게시글 ID로 댓글 목록을 조회한다.
        List<Reply> result =
                replyJpaRepository.findAllByBoardIdJoinUser(board.getId());

        // then: 해당 게시글의 댓글이 반환되는지 확인한다.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("첫 번째 댓글");
    }

    @Test
    void findAllByBoardIdJoinUser_다른게시글_댓글제외_테스트() {
        // given: 두 게시글에 각각 댓글을 저장한다.
        User user = 사용자저장("ssar");
        Board board1 = 게시글저장(user, "첫 번째 게시글");
        Board board2 = 게시글저장(user, "두 번째 게시글");

        댓글저장(board1, user, "첫 번째 게시글 댓글");
        댓글저장(board2, user, "두 번째 게시글 댓글");

        em.flush();
        em.clear();

        // when: 첫 번째 게시글의 댓글만 조회한다.
        List<Reply> result =
                replyJpaRepository.findAllByBoardIdJoinUser(board1.getId());

        // then: 첫 번째 게시글의 댓글만 반환되는지 확인한다.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("첫 번째 게시글 댓글");
    }

    @Test
    void findAllByBoardIdJoinUser_댓글없음_테스트() {
        // given: 댓글이 없는 게시글을 저장한다.
        User user = 사용자저장("ssar");
        Board board = 게시글저장(user, "댓글 없는 게시글");

        // when: 해당 게시글의 댓글 목록을 조회한다.
        List<Reply> result =
                replyJpaRepository.findAllByBoardIdJoinUser(board.getId());

        // then: 빈 목록이 반환되는지 확인한다.
        assertThat(result).isEmpty();
    }

    @Test
    void findAllByBoardIdJoinUser_작성자함께로딩_테스트() {
        // given: 작성자가 연결된 댓글을 저장한 뒤 영속성 컨텍스트를 비운다.
        User user = 사용자저장("ssar");
        Board board = 게시글저장(user, "테스트 게시글");
        댓글저장(board, user, "테스트 댓글");

        em.flush();
        em.clear();

        PersistenceUnitUtil util = em.getEntityManagerFactory()
                .getPersistenceUnitUtil();

        // when: JOIN FETCH 쿼리로 댓글 목록을 조회한다.
        List<Reply> result =
                replyJpaRepository.findAllByBoardIdJoinUser(board.getId());

        // then: 댓글의 user 연관관계가 로딩되었는지 확인한다.
        assertThat(result).isNotEmpty();
        assertThat(util.isLoaded(result.get(0), "user")).isTrue();
    }

    @Test
    void findAllByBoardIdJoinUser_오래된순서_테스트() {
        // given: 같은 게시글에 댓글을 순서대로 저장한다.
        User user = 사용자저장("ssar");
        Board board = 게시글저장(user, "테스트 게시글");

        Reply first = 댓글저장(board, user, "첫 번째 댓글");
        Reply second = 댓글저장(board, user, "두 번째 댓글");
        Reply third = 댓글저장(board, user, "세 번째 댓글");

        em.flush();
        em.clear();

        // when: 게시글의 댓글 목록을 조회한다.
        List<Reply> result =
                replyJpaRepository.findAllByBoardIdJoinUser(board.getId());

        // then: 댓글 ID가 오름차순으로 반환되는지 확인한다.
        assertThat(result)
                .extracting(Reply::getId)
                .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void deleteByBoardId_해당게시글댓글삭제_테스트() {
        // given: 두 게시글에 각각 댓글을 저장한다.
        User user = 사용자저장("ssar");
        Board targetBoard = 게시글저장(user, "삭제 대상 게시글");
        Board otherBoard = 게시글저장(user, "다른 게시글");

        댓글저장(targetBoard, user, "삭제할 댓글");
        댓글저장(otherBoard, user, "유지할 댓글");

        em.flush();
        em.clear();

        // when: 삭제 대상 게시글의 댓글을 일괄 삭제한다.
        replyJpaRepository.deleteByBoardId(targetBoard.getId());
        em.flush();
        em.clear();

        // then: 대상 게시글의 댓글이 삭제되었는지 확인한다.
        List<Reply> result =
                replyJpaRepository.findAllByBoardIdJoinUser(targetBoard.getId());

        assertThat(result).isEmpty();
    }

    @Test
    void deleteByBoardId_다른게시글댓글유지_테스트() {
        // given: 두 게시글에 각각 댓글을 저장한다.
        User user = 사용자저장("ssar");
        Board targetBoard = 게시글저장(user, "삭제 대상 게시글");
        Board otherBoard = 게시글저장(user, "다른 게시글");

        댓글저장(targetBoard, user, "삭제할 댓글");
        댓글저장(otherBoard, user, "유지할 댓글");

        em.flush();
        em.clear();

        // when: 대상 게시글의 댓글만 삭제한다.
        replyJpaRepository.deleteByBoardId(targetBoard.getId());
        em.flush();
        em.clear();

        // then: 다른 게시글의 댓글은 남아 있는지 확인한다.
        List<Reply> result =
                replyJpaRepository.findAllByBoardIdJoinUser(otherBoard.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("유지할 댓글");
    }
}
