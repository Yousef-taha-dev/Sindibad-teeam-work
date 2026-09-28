package com.sindibad.App.dto;

public record GuideApprovalRequestDTO(
        Long guideId,
        String approvalStatus
) {}