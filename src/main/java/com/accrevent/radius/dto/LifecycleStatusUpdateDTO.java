package com.accrevent.radius.dto;

import lombok.Data;

@Data
public class LifecycleStatusUpdateDTO {
    private String message;
    private Boolean isSystemComment;
    public LifecycleStatusUpdateDTO(String message, Boolean isSystemComment) {
        this.message = message;
        this.isSystemComment = isSystemComment;
    }
}
