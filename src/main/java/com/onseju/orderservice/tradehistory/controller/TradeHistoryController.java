package com.onseju.orderservice.tradehistory.controller;


import com.onseju.orderservice.global.security.UserDetailsImpl;
import com.onseju.orderservice.tradehistory.dto.TradeHistoryResponse;
import com.onseju.orderservice.tradehistory.service.TradeHistoryNotificationService;
import com.onseju.orderservice.tradehistory.service.TradeHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collection;

@RestController
@RequestMapping("/api/histories")
@RequiredArgsConstructor
public class TradeHistoryController {

    private final TradeHistoryService tradeHistoryService;

    private final TradeHistoryNotificationService tradeHistoryNotificationService;

    @GetMapping
    public Collection<TradeHistoryResponse> getAllTradeHistory(@AuthenticationPrincipal UserDetailsImpl user) {
        return tradeHistoryService.getAllTradeHistory(user.getMember().getId());
    }

    @GetMapping("/stream")
    public ResponseEntity<SseEmitter> subscribe(@AuthenticationPrincipal UserDetailsImpl user) {
        return ResponseEntity.ok(tradeHistoryNotificationService.subscribe(user.getMember().getId()));
    }
}
