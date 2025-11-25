package com.stockguard.data.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// User Statistics DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatisticsDTO {

    private long totalUsers;
    private long activeUsers;
    private long lockedUsers;
    private long disabledUsers;
    private long newUsersToday;
    private long newUsersThisWeek;
    private long newUsersThisMonth;
}