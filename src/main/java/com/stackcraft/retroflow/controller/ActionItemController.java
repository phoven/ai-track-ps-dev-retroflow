package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.controller.dto.ActionItemDtos.ActionItemResponse;
import com.stackcraft.retroflow.controller.dto.ActionItemDtos.UpdateActionItemRequest;
import com.stackcraft.retroflow.controller.mapper.ResponseMapper;
import com.stackcraft.retroflow.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PATCH /api/action-items/{id} {"priority"?, "completed"?} → 200 | 400 empty or invalid body | 404 |
 * 409 priority change in a CLOSED retro (D14), completed again (D7), or uncompleting a completed item (D8)
 */
@RestController
@RequestMapping("/api/action-items")
public class ActionItemController {

    private final FeedbackService feedbackService;

    public ActionItemController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PatchMapping("/{id}")
    public ActionItemResponse updateActionItem(@PathVariable Long id, @Valid @RequestBody UpdateActionItemRequest request) {
        return ResponseMapper.toResponse(feedbackService.updateActionItem(id, request.priority(), request.completed()));
    }

}
