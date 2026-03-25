package com.diev.controller;

import com.diev.entity.Bid;
import com.diev.security.CurrentUserId;
import com.diev.service.BidService;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bids")
@RequiredArgsConstructor
@Validated
public class BidController {

    private final BidService bidService;

    @PostMapping
    public Bid createBid(
            @RequestParam UUID taskId,
            @CurrentUserId UUID executorId
    ) {
        return bidService.createBid(taskId, executorId);
    }

    @GetMapping("/task/{taskId}")
    public List<Bid> getBids(@PathVariable UUID taskId,
                             @RequestParam(defaultValue = "20") @Positive int limit,
                             @RequestParam(defaultValue = "0") @PositiveOrZero int offset) {
        return bidService.getBids(taskId, limit, offset);
    }

    @PostMapping("/{bidId}/select")
    public void selectBid(
            @PathVariable UUID bidId,
            @CurrentUserId UUID customerId
    ) {
        bidService.selectBid(bidId, customerId);
    }

    @PostMapping("/task/{taskId}/complete")
    public void markCompleted(
            @PathVariable UUID taskId,
            @CurrentUserId UUID executorId
    ) {
        bidService.markCompleted(taskId, executorId);
    }
}