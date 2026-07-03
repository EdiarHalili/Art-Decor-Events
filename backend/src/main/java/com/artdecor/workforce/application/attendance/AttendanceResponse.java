package com.artdecor.workforce.application.attendance;

import java.time.Instant;

public record AttendanceResponse(
        String id,
        String scheduleId,
        String employeeId,
        String status,
        Instant checkedInAt,
        Instant checkedOutAt,
        int workedMinutes,
        int overtimeMinutes,
        boolean autoCheckout,
        boolean requiresApproval,
        String approvalReason
) {
}
