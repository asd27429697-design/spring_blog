package com.tenco.spring_blog._core.error;

// 401 Unauthorized 상황에서 사용할 사용자 정의 예외 클래스 (커스텀)
// RuntimeException 를 상속하여 언체크 예외로 만듬
public class Exception401 extends RuntimeException {
    // 예외 메세지를 받을 수 있도록 String 파라미터 설계
    public Exception401(String msg) {
        super(msg); // 부모 클래스의 메세지 설정
    }

}
