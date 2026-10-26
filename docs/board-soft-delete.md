# 게시글 소프트 삭제 구현 문서

## 목적

게시글 삭제 요청이 데이터베이스 행을 실제로 삭제하지 않고, 삭제 상태만 기록하도록 변경했다. 게시글·댓글·구매 이력을 보존하면서 일반 사용자의 목록과 상세 화면에서는 삭제된 글이 보이지 않게 한다.

## 변경된 데이터 구조

`board_tb`에 `deleted` 컬럼이 추가된다.

|컬럼|자료형|기본값|의미|
|---|---|---|---|
|`deleted`|`boolean`|`false`|`true`이면 삭제 처리된 게시글|

[Board.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/Board.java)는 `deleted` 필드와 `delete()` 메서드를 제공한다. `delete()`는 엔티티의 삭제 상태를 `true`로 바꾸며, JPA 트랜잭션이 끝날 때 변경 감지로 데이터베이스에 반영된다.

```java
public void delete() {
    this.deleted = true;
}
```

## 삭제 요청 처리

기존에는 삭제할 때 댓글을 먼저 지우고 `boardJpaRepository.deleteById(id)`로 게시글 행을 제거했다. 이제는 다음 순서로 처리한다.

1. 삭제할 게시글을 조회한다. 이미 삭제된 게시글은 조회 대상에서 제외되므로 404가 된다.
2. 요청자가 게시글 작성자인지 확인한다. 다른 사용자는 403 오류다.
3. 구매 이력이 있으면 삭제를 거절한다. 기존 포인트·구매 보호 정책을 유지한다.
4. `board.delete()`를 호출해 `deleted`를 `true`로 변경한다.
5. 댓글과 구매 이력은 삭제하지 않는다.

처리 위치: [BoardService.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/BoardService.java)

## 삭제된 게시글의 접근 규칙

|기능|처리|
|---|---|
|게시글 목록|삭제된 글 제외|
|제목·내용 검색|삭제된 글 제외|
|게시글 상세 조회|404 응답|
|수정 화면·수정 요청|404 응답|
|다시 삭제 요청|404 응답|
|댓글 작성|404 응답|
|유료 글 구매|404 응답|
|기존 댓글·구매 이력|데이터베이스에 유지|

목록·검색·상세·수정·삭제 요청은 `findByIdJoinUser()`와 목록 쿼리에 있는 `b.deleted = false` 조건으로 삭제된 글을 제외한다. 댓글 작성과 구매는 `findByIdAndDeletedFalse()`로 활성 게시글만 조회한다.

처리 위치: [BoardJpaRepository.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/BoardJpaRepository.java), [ReplyService.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/reply/ReplyService.java), [PurchaseService.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/purchase/PurchaseService.java)

## 흐름

```mermaid
flowchart TD
    A[작성자가 POST /board/{id}/delete 요청] --> B[활성 게시글 조회]
    B -->|없음 또는 이미 삭제됨| C[404]
    B --> D{작성자인가?}
    D -->|아니오| E[403]
    D -->|예| F{구매 이력이 있는가?}
    F -->|예| G[400: 삭제 불가]
    F -->|아니오| H[deleted = true]
    H --> I[목록·검색·상세에서 제외]
    H --> J[댓글·구매 이력 유지]
```

## 확인 필요

현재 구현에는 관리자용 삭제 목록, 복구 기능, 삭제 시각 및 삭제자 기록이 없다. 운영 정책에서 필요하다면 다음을 추가할지 결정해야 한다.

- 관리자만 보는 삭제 게시글 목록
- `deleted = false`로 되돌리는 복구 기능
- `deletedAt`, `deletedBy` 컬럼을 통한 감사 이력
- 구매 이력이 있는 게시글도 소프트 삭제를 허용할지에 대한 정책 변경

## 확인 방법

1. 구매 이력이 없는 본인 게시글을 삭제한다.
2. 메인 목록과 검색 결과에서 해당 글이 사라졌는지 확인한다.
3. 이전 상세 주소로 접속해 404가 발생하는지 확인한다.
4. 삭제된 글 ID로 댓글 작성 또는 구매 요청을 보내 404가 발생하는지 확인한다.
5. 데이터베이스에서 해당 게시글 행이 남아 있고 `deleted = true`인지 확인한다.
