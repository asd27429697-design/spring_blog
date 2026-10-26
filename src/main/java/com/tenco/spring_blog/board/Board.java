package com.tenco.spring_blog.board;


import com.tenco.spring_blog.user.User;
import com.tenco.spring_blog._core.util.MyDateUtil;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;

@Data
// 엔티티 클래스 만들기 : 데이터베이스 테이블 한 개를 자바 클래스로 그린 설계도
@Table(name = "board_tb")
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Board {

    // 유료 게시글 가격
    public static final int PREMIUM_PRICE = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // 별도 어노테이션이 없으면 필드명이 컬럼명이 됨.
    private String title;
    private String content;
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean premium;
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean deleted;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id") // board_tb 에 만들어질 외래키 컬럼 이름 설정
    private User user;
    @CreationTimestamp
    private Timestamp createdAt; // created_at 컬럼(스프링이 기본값이 스네이크 케이스로 자동 변환 해 줌)


    @Builder
    public Board(String title, String content, User user, boolean premium) {
        this.title = title;
        this.content = content;
        this.user = user;
        this.premium = premium;
    }


    public void update(BoardRequest.UpdateDto updateDto) {
        updateDto.validate();
        this.title = updateDto.getTitle();
        this.content = updateDto.getContent();
        this.premium = updateDto.isPremium();
    }

    // 실제 레코드는 보존하고 삭제 상태만 변경한다.
    public void delete() {
        this.deleted = true;
    }

    // 게시글 수정/삭제 권한 체크용 편의 메서드
    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
    }


    // 시간을 포맷을 메서드를 추가
    public String getTime() {
        return MyDateUtil.timestampFormat(createdAt);
    }

}
