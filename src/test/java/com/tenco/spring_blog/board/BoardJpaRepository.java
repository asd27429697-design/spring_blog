
package com.tenco.spring_blog.board;

import com.tenco.spring_blog.user.User; // 확인 필요: User 엔티티의 실제 패키지
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BoardJpaRepositoryTest {

    @Autowired
    private BoardJpaRepository boardJpaRepository;

    @Autowired
    private EntityManager em;

    // 게시글과 작성자를 함께 저장하는 테스트 준비 메서드
    private Board 게시글저장(String username, String title, String content) {
        User user = User.builder() // 확인 필요: User에 builder가 있다고 가정
                .username(username) // 확인 필요: username 필드가 있다고 가정
                .password("1234") // 확인 필요: password가 필수 필드라고 가정
                .build();

        em.persist(user);

        Board board = Board.builder() // 확인 필요: Board에 builder가 있다고 가정
                .title(title) // 확인 필요: title 필드가 있다고 가정
                .content(content) // 확인 필요: content 필드가 있다고 가정
                .user(user) // 확인 필요: Board가 User 연관관계를 가진다고 가정
                .build();

        em.persist(board);
        em.flush();

        return board;
    }

    @Test
    void findByIdJoinUser_존재하는_게시글_테스트() {
        // given: 테스트에서 사용할 게시글을 직접 저장한다.
        Board savedBoard = 게시글저장(
                "ssar", "스프링 공부", "JPA를 공부합니다"
        );

        // when: 저장한 게시글의 ID로 조회한다.
        Optional<Board> result =
                boardJpaRepository.findByIdJoinUser(savedBoard.getId());

        // then: 조회 결과에 게시글이 존재하는지 확인한다.
        assertThat(result).isPresent();
    }

    @Test
    void findByIdJoinUser_작성자_함께로딩_테스트() {
        // given: 작성자가 연결된 게시글을 저장한다.
        Board savedBoard = 게시글저장(
                "ssar", "스프링 공부", "JPA를 공부합니다"
        );
        Long boardId = savedBoard.getId();

        em.clear();

        PersistenceUnitUtil util = em.getEntityManagerFactory()
                .getPersistenceUnitUtil();

        // when: JOIN FETCH 쿼리로 게시글을 다시 조회한다.
        Board result = boardJpaRepository.findByIdJoinUser(boardId)
                .orElseThrow();

        // then: 게시글과 작성자 연관관계가 로딩되었는지 확인한다.
        assertThat(util.isLoaded(result, "user")).isTrue();
    }

    @Test
    void findByIdJoinUser_존재하지않는_게시글_테스트() {
        // given: DB에 저장하지 않은 ID를 사용한다.
        Long 없는게시글Id = -999L;

        // when: 존재하지 않는 ID로 조회한다.
        Optional<Board> result =
                boardJpaRepository.findByIdJoinUser(없는게시글Id);

        // then: 결과가 빈 Optional인지 확인한다.
        assertThat(result).isEmpty();
    }

    @Test
    void findAllJoinUser_페이지크기와_전체개수_테스트() {
        // given: 서로 다른 게시글 3개를 저장한다.
        게시글저장("ssar", "게시글1", "내용1");
        게시글저장("cos", "게시글2", "내용2");
        게시글저장("love", "게시글3", "내용3");

        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());

        // when: 첫 페이지를 크기 2로 조회한다.
        Page<Board> result = boardJpaRepository.findAllJoinUser(pageable);

        // then: 페이지 크기, 전체 개수, 전체 페이지 수를 확인한다.
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findAllJoinUser_정렬순서_테스트() {
        // given: 서로 다른 게시글 3개를 저장한다.
        게시글저장("ssar", "게시글1", "내용1");
        게시글저장("cos", "게시글2", "내용2");
        게시글저장("love", "게시글3", "내용3");

        Pageable pageable = PageRequest.of(
                0, 10, Sort.by(Sort.Direction.ASC, "id")
        );

        // when: ID 오름차순으로 조회한다.
        Page<Board> result = boardJpaRepository.findAllJoinUser(pageable);

        // then: 게시글 ID가 오름차순인지 확인한다.
        List<Long> ids = result.getContent().stream()
                .map(Board::getId)
                .toList();

        assertThat(ids).isSorted();
    }

    @Test
    void findAllJoinUser_작성자_함께로딩_테스트() {
        // given: 작성자가 연결된 게시글을 저장하고 영속성 컨텍스트를 비운다.
        게시글저장("ssar", "게시글1", "내용1");
        em.flush();
        em.clear();

        Pageable pageable = PageRequest.of(0, 10);
        PersistenceUnitUtil util = em.getEntityManagerFactory()
                .getPersistenceUnitUtil();

        // when: JOIN FETCH 페이징 쿼리로 게시글을 조회한다.
        Page<Board> result = boardJpaRepository.findAllJoinUser(pageable);

        // then: 조회된 게시글의 작성자 연관관계가 로딩되었는지 확인한다.
        assertThat(result.getContent()).isNotEmpty();
        assertThat(util.isLoaded(result.getContent().get(0), "user")).isTrue();
    }

    @Test
    void findAllKeywordJoinUser_제목일치_테스트() {
        // given: 제목에 검색어가 포함된 게시글과 포함되지 않은 게시글을 저장한다.
        게시글저장("ssar", "스프링 부트 공부", "서버 개발");
        게시글저장("cos", "자바 공부", "객체지향");

        Pageable pageable = PageRequest.of(0, 10);

        // when: 제목에 포함된 검색어로 조회한다.
        Page<Board> result =
                boardJpaRepository.findAllKeywordJoinUser(pageable, "스프링");

        // then: 제목이 일치하는 게시글만 반환되는지 확인한다.
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle())
                .contains("스프링");
    }

    @Test
    void findAllKeywordJoinUser_내용일치_테스트() {
        // given: 내용에만 검색어가 포함된 게시글을 저장한다.
        게시글저장("ssar", "개발 일지", "스프링 부트와 JPA");
        게시글저장("cos", "운동 일지", "매일 달리기");

        Pageable pageable = PageRequest.of(0, 10);

        // when: 내용에 포함된 검색어로 조회한다.
        Page<Board> result =
                boardJpaRepository.findAllKeywordJoinUser(pageable, "JPA");

        // then: 내용이 일치하는 게시글을 반환하는지 확인한다.
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getContent()).contains("JPA");
    }

    @Test
    void findAllKeywordJoinUser_제목과내용_불일치_테스트() {
        // given: 검색어가 제목과 내용 어디에도 없는 게시글을 저장한다.
        게시글저장("ssar", "스프링 공부", "자바를 공부합니다");

        Pageable pageable = PageRequest.of(0, 10);

        // when: 존재하지 않는 검색어로 조회한다.
        Page<Board> result =
                boardJpaRepository.findAllKeywordJoinUser(pageable, "파이썬");

        // then: 검색 결과가 없는지 확인한다.
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void findAllKeywordJoinUser_빈키워드_테스트() {
        // given: 검색 대상 게시글을 2개 저장한다.
        게시글저장("ssar", "스프링 공부", "JPA 공부");
        게시글저장("cos", "자바 공부", "객체지향 공부");

        Pageable pageable = PageRequest.of(0, 10);

        // when: 빈 문자열로 검색한다.
        Page<Board> result =
                boardJpaRepository.findAllKeywordJoinUser(pageable, "");

        // then: 일반적인 비어 있지 않은 제목과 내용이라면 모두 조회되는지 확인한다.
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }
}
