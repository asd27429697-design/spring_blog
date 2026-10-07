package com.tenco.spring_blog.controller;

import com.tenco.spring_blog._core.error.Exception400;
import com.tenco.spring_blog._core.error.Exception404;
import com.tenco.spring_blog._core.util.Define;
import com.tenco.spring_blog.user.User;
import com.tenco.spring_blog.user.UserPersistRepository;
import com.tenco.spring_blog.user.UserRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@RequiredArgsConstructor // DI
@Slf4j
@Controller // IoC (제어의 역전) 싱글톤 패턴으로 관리 됨.
public class UserController {

    private final UserPersistRepository userPersistRepository;

    // GET http://localhost:8080/join
    @GetMapping("/join")
    public String joinForm() {
        return "user/join-form";
    }

    // POST http://localhost:8080/join
    @PostMapping("/join")
    public String join(UserRequest.JoinDto joinDto, Model model) {
        // 1. 유효성 검사
        joinDto.validate();
        // 2. 사용자명 중복 체크
        User existingUser = userPersistRepository.findByUsername(joinDto.getUsername());
        if (existingUser != null) {
            throw new Exception400("이미 존재하는 사용자명입니다");
        }
        // 3. DTO 를 Entity 변환
        User user = joinDto.toEntity();
        // 4. DB 에 회원 정보 저장
        User userEntity = userPersistRepository.save(user);
        // 회원 가입 성공시 로그인 화면으로 이동
        return "redirect:/login";
    }


    // GET http://localhost:8080/login
    @GetMapping("/login")
    public String loginForm() {
        // templates/   <-- 콘텐츠 루트 경로
        return "user/login-form";
    }

    // POST http://localhost:8080/login
    // 로그인 처리 (예외적으로 POTS 요청)
    @PostMapping("/login")
    public String login(UserRequest.LoginDto loginDto, HttpSession session, Model model) {
        // 1. 입력 데이터 검증
        loginDto.validate();
        // 2. 사용자명과 비밀번호로 사용자 조회
        User sessionUser = userPersistRepository.findByUsernameAndPassword(loginDto.getUsername(),
                loginDto.getPassword());
        // 3. 로그인 성공/실패 처리
        if (sessionUser == null) {
            // 로그인 실패 : 일치하는 사용자 없음
            throw new Exception400("사용자명 또는 비밀번호가 올바르지 않습니다");
        }
        // 머스태치가 세션 값을 기본으로 읽지 않는 설정이 되어 있음
        // 머스태치지 파일에서 세션 메모리에 접근할 수 있도록 설정을 추가 해야 함. application.yml 공통
        // 4. 로그인 성공 : 세션에 사용자 정보를 저장
        sessionUser.setPassword(null);
        session.setAttribute(Define.SESSION_USER, sessionUser);
        // 5. 메인 페이지로 리다이렉트
        return "redirect:/";
    }


    // GET http://localhost:8080/user/update
    @GetMapping("/user/update")
    public String updateForm(Model model, HttpSession session) {
        // 1. 인증 검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        if (sessionUser == null) {
            return "redirect:/login";
        }
        User user = userPersistRepository.findById(sessionUser.getId());
        model.addAttribute("user", user);
        return "user/update-form";
    }

    // GET http://localhost:8080/user/update
    @PostMapping("/user/update")
    public String update(UserRequest.UpdateDto updateDto, Model model, HttpSession session) {
        // 1. 인증검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);
        if (sessionUser == null) {
            return "redirect:/login";
        }
        // 2. 권한 검사
        // 다른 사람의 정보는 처음부터 수정할 수 없음(대상이 실제로 있는지만 확인)
        User userEntity = userPersistRepository.findById(sessionUser.getId());
        if (userEntity == null) {
            throw new Exception404("사용자을 찾을 수 없습니다");
        }
        // 3. 유효성 검사
        updateDto.validate();
        // 4. 세션 동기화 : 수정된 정보를 세션에 반영
        User updateUser = userPersistRepository.updateById(sessionUser.getId(), updateDto);
        // 세션 동기화 처리
        updateUser.setPassword(null);
        session.setAttribute(Define.SESSION_USER, updateUser);
        // 5. 수정 성공 후 메인 페이지
        return "redirect:/";
    }

    // GET http://localhost:8080/logout
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

}
