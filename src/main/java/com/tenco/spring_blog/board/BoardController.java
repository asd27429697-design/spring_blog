package com.tenco.spring_blog.board;

import com.tenco.spring_blog._core.error.Exception403;
import com.tenco.spring_blog._core.error.Exception404;
import com.tenco.spring_blog._core.util.Define;
import com.tenco.spring_blog.user.User;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RequiredArgsConstructor // DI 처리
@Controller
public class BoardController {

    private final BoardPersistRepository boardPersistRepository;

    @GetMapping({"/", "/board/list"})
    public String list(Model model) {
        List<Board> boardList = boardPersistRepository.findAll();
        model.addAttribute("boardList", boardList);
        return "board/list";
    }

    // GET http://localhost:8080/board/3
    // excludePathPatterns로 제외되어 로그인 접근 가능
    @GetMapping("/board/{id}")
    public String detail(@PathVariable(name = "id") Long id, Model model) {
        Board boardEntity = boardPersistRepository.findById(id);
        if (boardEntity == null) {
            throw new Exception404("게시글을 찾을 수 없습니다");
        }
        model.addAttribute("board", boardEntity);
        return "board/detail";
    }


    // GET http://localhost:8080/board/save (화면 요청)
    @GetMapping("/board/save")
    public String saveForm(HttpSession session) {
        // 인터셉터에서 인증 검사 진행 됨.
        return "board/save-form";
    }


    @PostMapping("/board/save")
    // Spring 폼 데이터를 객체로 변환하는 과정 (데이터 바인딩 메커니즘)
    // 폼 데이터 바인딩 : Spring이 HTTP  요청 파라미터를 객체로 자동 변환
    public String save(BoardRequest.SaveDto saveDto, HttpSession session) {
        // 1. 인증검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);

        // 2. 유효성 검사
        // 입력 데이터 검증
        saveDto.validate();
        // DTO 에서 Board 객체 생성
        Board board = saveDto.toEntity(sessionUser);
        // Board 저장
        Board savedBoard = boardPersistRepository.save(board);
        // 저장 성공시 메인 페이지 이동
        return "redirect:/";
    }

    // GET http://localhost:8080/board/1/update (화면 요청)
    @GetMapping("/board/{id}/update")
    public String updateForm(@PathVariable Long id, Model model, HttpSession session) {
        // 1. 인증 검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);

        // 2. 권한 체크를 위한 게시글 조회
        Board board = boardPersistRepository.findById(id);
        // 3. 권한 체크 : 본인이 작성한 게시글만 수정 가능
        if (!board.isOwner(sessionUser.getId())) {
            throw new Exception403("수정 권한이 없습니다");
        }
        model.addAttribute("board", board);
        return "board/update-form";
    }

    // POST http://localhost:8080/board/1/update (게시글 수정 기능 요청)
    @PostMapping("/board/{id}/update")
    public String update(@PathVariable Long id,
                         BoardRequest.UpdateDto updateDto, HttpSession session) {
        // 1. 인증 검사
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);

        // 2. 권한 검사
        Board boardEntity = boardPersistRepository.findById(id);

        if (!boardEntity.isOwner(sessionUser.getId())) {
            throw new Exception403("수정 권한이 없습니다");
        }
        // 3. 입력 데이터 검증
        updateDto.validate();
        // 4. 더티 체킹을 통한 수정 실행
        boardPersistRepository.updateById(id, updateDto);
        // PRG
        // 5. 수정 완료 후 해당 게시글 상세보기로 리다이텍트 처리
        return "redirect:/board/" + id;
    }

    // 게시글 삭제
    @PostMapping("/board/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) throws Exception403 {
        // 1. 인증 검사 (로그인 여부 확인)
        // 2. 권한 확인 -- 로그인 했지만 내가 작성한 글 인지 여부 확인
        // 2.1 - 관리자 광고성 게시글 .. 삭제도 가능 (권한)
        User sessionUser = (User) session.getAttribute(Define.SESSION_USER);

        // 2. 삭제할 게시글 조회 (권한 체크를 위해)
        Board boardEntity = boardPersistRepository.findById(id);
        // 3. 권한 체크 : 본인이 작성한 게시글만 삭제
        if (!boardEntity.isOwner(sessionUser.getId())) {
            throw new Exception403("삭제 권한이 없습니다");
        }
        // 4. 권한 확인 후 삭제 실행
        boardPersistRepository.deleteById(id);
        // 5. 삭제 성공 후 메인 페이지 리다이렉트
        return "redirect:/";
    }
}
