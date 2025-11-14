package com.likelion.backend.controller;

import com.likelion.backend.dto.request.JoinRequestDto;
import com.likelion.backend.dto.response.JoinResponseDto;
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
    public ResponseEntity<?> login(@RequestBody JoinRequestDto joinRequestDto){
        try{
            memberService.login(joinRequestDto);
            return ResponseEntity.ok(memberService.login(joinRequestDto));
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/my/{id}")
    public ResponseEntity<MyResponseDto> getMyInfo(@PathVariable Long id){
        return ResponseEntity.ok(memberService.my(id));
    }
}
