package com.likelion.backend.controller;

import com.likelion.backend.dto.request.JoinRequestDto;
import com.likelion.backend.dto.request.LoginRequestDto;
import com.likelion.backend.dto.response.JoinResponseDto;
import com.likelion.backend.dto.response.LoginResponseDto;
import com.likelion.backend.dto.response.MyResponseDto;
import com.likelion.backend.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class JoinController {
    private final MemberService memberService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto dto) {
        return ResponseEntity.ok(memberService.login(dto));
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestBody JoinRequestDto joinRequestDto){
        try{
            JoinResponseDto response = memberService.join(joinRequestDto);
            return ResponseEntity.ok(response);
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<MyResponseDto> getMyInfo(@PathVariable Long id){
        return ResponseEntity.ok(memberService.my(id));
    }

    @DeleteMapping("/member/{id}")
    public ResponseEntity<String> deleteMember(@PathVariable Long id){
        memberService.deleteMember(id);
        return ResponseEntity.ok("회원이 성공적으로 삭제되었습니다.");
    }

}
