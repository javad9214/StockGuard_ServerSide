package com.stockguard.data.dto.auth.response;

import com.stockguard.data.dto.auth.AdminUserDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Paginated User List Response
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUserListResponse {

    private java.util.List<AdminUserDTO> users;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int pageSize;
}