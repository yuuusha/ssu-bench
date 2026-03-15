package com.diev.handler;

import com.diev.entity.Bid;
import com.diev.service.BidService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bids")
public class BidController {

    private final BidService bidService;

    public BidController(BidService bidService) {
        this.bidService = bidService;
    }

    @PostMapping
    public Bid createBid(
            @RequestParam UUID taskId,
            @RequestParam UUID executorId
    ) {

        return bidService.createBid(taskId, executorId);
    }

    @GetMapping("/task/{taskId}")
    public List<Bid> getBids(@PathVariable UUID taskId) {

        return bidService.getBids(taskId);
    }

    @PostMapping("/{bidId}/select")
    public void selectBid(
            @PathVariable UUID bidId,
            @RequestParam UUID taskId,
            @RequestParam UUID customerId
    ) {

        bidService.selectBid(taskId, bidId, customerId);
    }

    @PostMapping("/task/{taskId}/complete")
    public void markCompleted(
            @PathVariable UUID taskId,
            @RequestParam UUID executorId
    ) {

        bidService.markCompleted(taskId, executorId);
    }
}