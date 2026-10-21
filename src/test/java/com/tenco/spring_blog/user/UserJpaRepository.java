
package com.tenco.spring_blog.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceUnitUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserJpaRepositoryTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EntityManager em;

    private User 사용자저장(String username, String email) {
        User user = User.builder() // 확인 필요: User에 빌더가 있다고 가정
                .username(username) // 확인 필요: username 필드가 있다고 가정
                .password("encoded-password") // 확인 필요: password 필드가 있다고 가정
                .email(email) // 확인 필요: email 필드가 있다고 가정
                .build();

        em.persist(user);
        em.flush();

        return user;
    }

    @Test
    void findByUsernameWithRoles_사용자명일치_테스트() {
        // given: 조회할 사용자를 저장한다.
        User savedUser = 사용자저장("ssar", "ssar@example.com");

        // when: 사용자명으로 조회한다.
        Optional<User> result =
                userJpaRepository.findByUsernameWithRoles(savedUser.getUsername());

        // then: 조회 결과에 해당 사용자가 존재하는지 확인한다.
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("ssar");
    }

    @Test
    void findByUsernameWithRoles_사용자명불일치_테스트() {
        // given: 다른 사용자명을 가진 사용자를 저장한다.
        사용자저장("ssar", "ssar@example.com");

        // when: 존재하지 않는 사용자명으로 조회한다.
        Optional<User> result =
                userJpaRepository.findByUsernameWithRoles("unknown");

        // then: 결과가 비어 있는지 확인한다.
        assertThat(result).isEmpty();
    }

    @Test
    void findByUsernameWithRoles_역할함께로딩_테스트() {
        // given: 사용자를 저장하고 영속성 컨텍스트를 비운다.
        User savedUser = 사용자저장("ssar", "ssar@example.com");
        String username = savedUser.getUsername();
        em.clear();

        PersistenceUnitUtil util = em.getEntityManagerFactory()
                .getPersistenceUnitUtil();

        // when: JOIN FETCH 쿼리로 사용자를 조회한다.
        User result = userJpaRepository.findByUsernameWithRoles(username)
                .orElseThrow();

        // then: roles 연관관계가 로딩되었는지 확인한다.
        assertThat(util.isLoaded(result, "roles")).isTrue();
    }

    @Test
    void findByUsername_사용자명일치_테스트() {
        // given: 사용자를 저장한다.
        사용자저장("ssar", "ssar@example.com");

        // when: 등록된 사용자명으로 조회한다.
        Optional<User> result = userJpaRepository.findByUsername("ssar");

        // then: 해당 사용자가 조회되는지 확인한다.
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("ssar");
    }

    @Test
    void findByUsername_사용자명불일치_테스트() {
        // given: 사용자를 저장한다.
        사용자저장("ssar", "ssar@example.com");

        // when: 다른 사용자명으로 조회한다.
        Optional<User> result = userJpaRepository.findByUsername("cos");

        // then: 조회 결과가 비어 있는지 확인한다.
        assertThat(result).isEmpty();
    }

    @Test
    void findByEmail_이메일일치_테스트() {
        // given: 이메일을 가진 사용자를 저장한다.
        사용자저장("ssar", "ssar@example.com");

        // when: 등록된 이메일로 조회한다.
        Optional<User> result =
                userJpaRepository.findByEmail("ssar@example.com");

        // then: 해당 사용자가 조회되는지 확인한다.
        assertThat(result).isPresent();
        assertThat(result.get().getEmail())
                .isEqualTo("ssar@example.com");
    }

    @Test
    void findByEmail_이메일불일치_테스트() {
        // given: 사용자를 저장한다.
        사용자저장("ssar", "ssar@example.com");

        // when: 등록되지 않은 이메일로 조회한다.
        Optional<User> result =
                userJpaRepository.findByEmail("unknown@example.com");

        // then: 조회 결과가 비어 있는지 확인한다.
        assertThat(result).isEmpty();
    }
}
