# 포인트와 유료 게시글 처리 구조

이 문서는 현재 소스 코드를 기준으로 포인트 충전, 유료 게시글 구매, 구매 후 열람 처리를 설명한다. 구현으로 확인되지 않은 정책이나 외부 연동은 **확인 필요**로 표시한다.

## 한눈에 보는 흐름

```mermaid
flowchart TD
    A[마이페이지에서 포인트 충전] --> B[POST /api/point/charge]
    B --> C[user_tb.point 증가]

    D[유료 게시글 상세 요청] --> E{무료 글 또는 작성자 또는 구매자?}
    E -->|예| F[본문 표시]
    E -->|아니오| G[본문 제거 후 구매 버튼 표시]
    G --> H[POST /board/{id}/purchase]
    H --> I{유료 글 · 타인 글 · 미구매?}
    I -->|통과| J{보유 포인트 500 이상?}
    J -->|예| K[point에서 500 차감]
    K --> L[purchase_tb에 구매 이력 저장]
    L --> M[상세 페이지 재요청 → 본문 표시]
    J -->|아니오| N[포인트 부족 안내]
```

## 데이터 모델

|저장소|핵심 값|역할|
|---|---|---|
|`user_tb`|`point` (`int`, 기본값 0)|사용자의 현재 보유 포인트|
|`board_tb`|`premium` (`boolean`, 기본값 false)|유료 게시글 여부|
|`purchase_tb`|`user_id`, `board_id`, `price`, `created_at`|어떤 사용자가 어떤 글을 얼마에 구매했는지 기록|

`purchase_tb`에는 `(user_id, board_id)` 조합의 유니크 제약 `uk_user_board`가 있다. 따라서 데이터베이스 수준에서도 한 사용자가 같은 게시글을 두 번 구매할 수 없다. 관련 엔티티는 [User.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/user/User.java), [Board.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/Board.java), [Purchase.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/purchase/Purchase.java)에 있다.

현재 유료 게시글 가격은 `Board.PREMIUM_PRICE = 500`으로 고정이다. 구매 이력에는 당시 가격도 `price`로 저장한다.

## 1. 포인트 충전

마이페이지의 충전 버튼은 `POST /api/point/charge`에 `{ "amount": 숫자 }` JSON을 전송한다. API는 세션 로그인을 확인하고, 요청 금액을 검증한 뒤 `UserService.chargePoint()`를 호출한다.

|검사|처리|
|---|---|
|로그인 세션 없음|401 JSON 응답: `로그인이 필요합니다`|
|금액이 없거나 0 이하|400 오류|
|한 번에 100,000 초과|400 오류|
|정상 요청|`user.point += amount` 후 현재 포인트를 JSON으로 반환|

`User` 엔티티의 `chargePoint()`도 0 이하 금액을 한 번 더 막는다. `@Transactional`인 서비스 메서드 안에서 엔티티 값을 변경하므로, 트랜잭션 커밋 시 JPA 더티 체킹으로 `user_tb.point`가 갱신된다.

근거 코드: [UserApiController.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/user/UserApiController.java), [UserRequest.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/user/UserRequest.java), [UserService.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/user/UserService.java), [user/detail.mustache](/C:/khs/work_space/spring_class/spring_blog/src/main/resources/templates/user/detail.mustache).

**확인 필요:** 화면 주석과 코드 모두 이 API를 실제 결제 연동 전의 임시 충전 기능으로 설명한다. 외부 결제 승인, 결제 실패 처리, 환불, 충전 이력은 현재 코드에서 확인되지 않는다.

## 2. 유료 글 작성과 상세 열람

글 저장·수정 폼의 `premium` 체크 값이 `Board.premium`에 저장된다. 상세 조회 시 `BoardService.findById(id, sessionUserId)`는 다음 두 값을 계산한다.

- `owner`: 로그인한 사용자가 게시글 작성자인지
- `purchased`: 로그인한 사용자가 해당 글의 구매 이력을 갖는지

그 뒤 `BoardResponse.DetailDto`가 아래 규칙으로 `canRead`를 만든다.

```java
canRead = !premium || owner || purchased;
```

`canRead`가 `false`이면 DTO의 `content`를 `null`로 바꾼다. 따라서 템플릿에서 가리는 것에 그치지 않고, 서버가 뷰로 전달하는 본문도 제거한다. 화면은 본문 대신 500 P 구매 안내를 표시한다.

|방문자 상태|무료 글|유료 글|
|---|---|---|
|비로그인|본문 표시|본문 숨김, 로그인 유도|
|작성자|본문 표시|본문 표시|
|구매 이력이 있는 로그인 사용자|본문 표시|본문 표시|
|그 외 로그인 사용자|본문 표시|본문 숨김, 구매 버튼 표시|

근거 코드: [BoardService.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/BoardService.java), [BoardResponse.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/BoardResponse.java), [detail.mustache](/C:/khs/work_space/spring_class/spring_blog/src/main/resources/templates/board/detail.mustache).

## 3. 구매 처리

구매 버튼은 `POST /board/{id}/purchase`를 호출한다. `/board/**`는 로그인 인터셉터의 보호 대상이므로, 로그인하지 않은 사용자는 구매 컨트롤러까지 도달할 수 없다.

`PurchaseService.purchase(boardId, userId)`는 하나의 쓰기 트랜잭션으로 다음 순서대로 처리한다.

1. 게시글 존재 여부를 조회한다. 없으면 404 오류다.
2. `premium`이 `true`인지 확인한다. 무료 글이면 구매를 거절한다.
3. 작성자 본인인지 확인한다. 작성자는 이미 본문을 볼 수 있으므로 구매를 거절한다.
4. 기존 구매 이력을 확인한다. 이미 구매했다면 거절한다.
5. 구매자 `User`를 조회하고 `deductPoint(500)`을 호출한다.
6. 포인트가 500 미만이면 `NotEnoughtPointException`을 발생시킨다.
7. 포인트 차감 후 `purchase_tb`에 구매 이력을 저장한다.

서비스 전체가 하나의 트랜잭션이므로, 1~6단계에서 예외가 발생하면 포인트 차감과 구매 이력 저장은 커밋되지 않는다. 성공하면 구매 요청은 상세 페이지로 리다이렉트되고, 새 요청에서 구매 이력이 조회되어 본문이 표시된다.

근거 코드: [BoardController.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/BoardController.java), [PurchaseService.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/purchase/PurchaseService.java), [PurchaseJpaRepository.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/purchase/PurchaseJpaRepository.java), [WebMvcConfig.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/_core/config/WebMvcConfig.java).

## 4. 예외와 삭제 정책

포인트가 부족하면 `User.deductPoint()`가 `NotEnoughtPointException`을 발생시키고, 전역 예외 처리기가 알림을 띄운 뒤 `/user/detail`로 이동시킨다.

게시글 삭제 시 구매 이력이 하나라도 있으면 삭제를 거절한다. 즉, 구매자가 있는 유료 글은 작성자가 삭제할 수 없다. `PurchaseJpaRepository`에는 구매 이력 일괄 삭제 메서드도 있지만, 현재 `BoardService.deleteById()`에서는 호출하지 않는다.

근거 코드: [GlobalExceptionHandler.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/_core/error/GlobalExceptionHandler.java), [BoardService.java](/C:/khs/work_space/spring_class/spring_blog/src/main/java/com/tenco/spring_blog/board/BoardService.java).

## 현재 구현에서 확인할 사항

아래 항목은 소스에서 정책 또는 대응 구현을 확인할 수 없으므로, 운영·과제 요구사항에 맞게 결정이 필요하다.

1. **실제 결제 연동:** 현재 충전 API만 호출하면 포인트가 증가한다. 결제 승인 검증과 충전·환불 이력 테이블은 없다.
2. **동시 구매 처리:** `User`에 `@Version`, 비관적 잠금, 잔액을 조건으로 차감하는 SQL이 없다. 같은 사용자가 여러 구매 요청을 동시에 보내면 잔액 갱신 충돌을 막는 방식이 필요할 수 있다.
3. **가격의 단일 관리:** 가격은 `Board.PREMIUM_PRICE`, `BoardResponse.DetailDto`, 상세 Mustache에 각각 500으로 표현돼 있다. 가격 변경 시 서로 달라질 수 있으므로 한 곳에서 전달하는 방식이 필요한지 확인해야 한다.
4. **구매 후 게시글 가격 상태 변경:** 작성자는 수정 화면에서 `premium` 값을 변경할 수 있다. 구매 이력이 있는 글을 무료·유료로 바꾸는 정책은 코드에서 제한하지 않는다.
5. **댓글 공개 정책:** 유료 글 본문을 볼 수 없는 방문자도 상세 화면의 댓글 목록은 조회된다. 댓글까지 구매자에게만 보일지 확인이 필요하다.
6. **부족 포인트 HTTP 상태:** 부족 포인트 예외 처리기는 자바스크립트 응답을 반환하지만 상태 코드를 명시하지 않는다. API 클라이언트까지 고려한다면 적절한 HTTP 상태 코드가 필요한지 확인해야 한다.

## 빠른 수동 확인 시나리오

초기 데이터 기준으로 `minsu`는 1,000 P, `jihun`은 500 P, `seoyeon`은 0 P로 설정돼 있다. `data.sql`의 유료 글과 구매 이력 주석을 사용하면 다음을 확인할 수 있다.

1. 미구매 유료 글을 비로그인으로 열면 본문 대신 로그인 버튼이 보인다.
2. `minsu`로 로그인해 미구매 유료 글을 사면 포인트가 500 감소하고 본문이 보인다.
3. 같은 글을 다시 구매하면 이미 구매했다는 오류가 난다.
4. `seoyeon`으로 미구매 유료 글을 사면 포인트 부족 안내가 나온다.
5. 구매 이력이 있는 글을 작성자가 삭제하려 하면 삭제가 거절된다.

근거 데이터: [data.sql](/C:/khs/work_space/spring_class/spring_blog/src/main/resources/db/data.sql).
