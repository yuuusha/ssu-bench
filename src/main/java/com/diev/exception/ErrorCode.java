package com.diev.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // domain
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_ALREADY_EXISTS", "User with this email already exists."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid credentials."),
    USER_BLOCKED(HttpStatus.FORBIDDEN, "USER_BLOCKED", "User is blocked."),

    INVALID_REWARD(HttpStatus.BAD_REQUEST, "INVALID_REWARD", "Reward must be greater than zero."),
    INVALID_BALANCE(HttpStatus.BAD_REQUEST, "INVALID_BALANCE", "Balance must be greater than zero."),

    TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "Task not found."),
    TASK_NOT_EDITABLE(HttpStatus.CONFLICT, "TASK_NOT_EDITABLE", "Task cannot be updated in its current state."),
    TASK_NOT_PUBLISHABLE(HttpStatus.CONFLICT, "TASK_NOT_PUBLISHABLE", "Task can be published only from CREATED status."),
    TASK_ALREADY_DONE(HttpStatus.CONFLICT, "TASK_ALREADY_DONE", "Completed task cannot be cancelled."),
    TASK_NOT_OPEN_FOR_BIDS(HttpStatus.CONFLICT, "TASK_NOT_OPEN_FOR_BIDS", "Task is not open for bids."),
    CUSTOMER_CANNOT_BID(HttpStatus.CONFLICT, "CUSTOMER_CANNOT_BID", "Task owner cannot create a bid for own task."),
    BID_ALREADY_EXISTS(HttpStatus.CONFLICT, "BID_ALREADY_EXISTS", "Executor already has a bid for this task."),
    BID_NOT_CREATED(HttpStatus.CONFLICT, "BID_NOT_CREATED", "Bid not created."),
    BID_NOT_FOUND(HttpStatus.NOT_FOUND, "BID_NOT_FOUND", "Bid not found."),
    TASK_NOT_OPEN_FOR_SELECTION(HttpStatus.CONFLICT, "TASK_NOT_OPEN_FOR_SELECTION", "Task is not open for selecting bids."),
    EXECUTOR_ALREADY_SELECTED(HttpStatus.CONFLICT, "EXECUTOR_ALREADY_SELECTED", "Executor already selected."),
    EXECUTOR_NOT_ASSIGNED(HttpStatus.CONFLICT, "EXECUTOR_NOT_ASSIGNED", "Executor not assigned."),
    TASK_NOT_DONE(HttpStatus.CONFLICT, "TASK_NOT_DONE", "Task is not completed by executor."),
    TASK_NOT_IN_PROGRESS(HttpStatus.CONFLICT, "TASK_NOT_IN_PROGRESS", "Task is not in progress."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "INSUFFICIENT_BALANCE", "Not enough balance."),

    ONLY_OWNER_OR_ADMIN_CAN_UPDATE_USER(HttpStatus.FORBIDDEN, "ONLY_OWNER_OR_ADMIN_CAN_UPDATE_USER", "Only owner or admin can update this user."),
    ONLY_OWNER_CAN_DELETE(HttpStatus.FORBIDDEN, "ONLY_OWNER_CAN_DELETE", "Only owner can delete."),
    ONLY_OWNER_OR_ADMIN_CAN_VIEW_USER(HttpStatus.FORBIDDEN, "ONLY_OWNER_OR_ADMIN_CAN_VIEW_USER", "Only owner or admin can view this user."),
    ONLY_OWNER_CAN_UPDATE_TASK(HttpStatus.FORBIDDEN, "ONLY_OWNER_CAN_UPDATE_TASK", "Only task owner can update it."),
    ONLY_OWNER_CAN_PUBLISH_TASK(HttpStatus.FORBIDDEN, "ONLY_OWNER_CAN_PUBLISH_TASK", "Only task owner can publish it."),
    ONLY_OWNER_CAN_CANCEL(HttpStatus.FORBIDDEN, "ONLY_OWNER_CAN_CANCEL", "Only the task owner can cancel it."),
    ONLY_OWNER_CAN_SELECT_BID(HttpStatus.FORBIDDEN, "ONLY_OWNER_CAN_SELECT_BID", "Only task owner can select bid."),
    ONLY_CUSTOMER_CAN_CONFIRM(HttpStatus.FORBIDDEN, "ONLY_CUSTOMER_CAN_CONFIRM", "Only customer can confirm the task."),
    ONLY_ASSIGNED_EXECUTOR_CAN_COMPLETE(HttpStatus.FORBIDDEN, "ONLY_ASSIGNED_EXECUTOR_CAN_COMPLETE", "Only assigned executor can complete the task."),

    // framework / API
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed."),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "Invalid parameter."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "Missing required parameter."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access denied."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication required."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error."),
    REQUEST_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "REQUEST_TIMEOUT", "Request timed out.");

    private final HttpStatus status;
    private final String code;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String code, String defaultMessage) {
        this.status = status;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}