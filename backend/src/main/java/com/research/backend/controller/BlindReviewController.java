package com.research.backend.controller;

import com.research.backend.common.Result;
import com.research.backend.dto.BlindReviewConfigDto;
import com.research.backend.dto.PaperDesensitizedDto;
import com.research.backend.service.BlindReviewService;
import com.research.backend.util.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blind")
@RequiredArgsConstructor
public class BlindReviewController {

    private final BlindReviewService blindReviewService;
    private final JwtUtil jwtUtil;

    @GetMapping("/config")
    public Result<BlindReviewConfigDto> getGlobalConfig() {
        BlindReviewConfigDto config = blindReviewService.getGlobalConfig();
        return Result.success(config);
    }

    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<BlindReviewConfigDto> updateGlobalConfig(
            @Valid @RequestBody BlindReviewConfigDto configDto) {
        BlindReviewConfigDto updated = blindReviewService.updateGlobalConfig(configDto);
        return Result.success("配置更新成功", updated);
    }

    @PutMapping("/paper/{paperId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Boolean> toggleBlindReview(
            @PathVariable Long paperId,
            @RequestParam boolean enabled) {
        boolean result = blindReviewService.toggleBlindReview(paperId, enabled);
        return Result.success(enabled ? "双盲评审已开启" : "双盲评审已关闭", result);
    }

    @GetMapping("/paper/{paperId}")
    public Result<PaperDesensitizedDto> getDesensitizedPaper(
            @PathVariable Long paperId,
            @RequestHeader("Authorization") String authHeader) {
        Long viewerId = null;
        String viewerRole = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                viewerId = jwtUtil.getUserIdFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                if (role != null && role.startsWith("ROLE_")) {
                    viewerRole = role.substring(5);
                } else {
                    viewerRole = role;
                }
            } catch (Exception ignored) {
            }
        }

        PaperDesensitizedDto dto = blindReviewService.desensitizePaper(paperId, viewerId, viewerRole);
        return Result.success(dto);
    }

    @PostMapping("/reviewer/{paperId}/{reviewerId}/alias")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> assignReviewerAlias(
            @PathVariable Long paperId,
            @PathVariable Long reviewerId) {
        blindReviewService.assignReviewerAlias(paperId, reviewerId);
        return Result.success("匿名代号分配成功");
    }
}
