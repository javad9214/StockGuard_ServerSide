package com.stockguard.controller;

import com.stockguard.data.dto.ApiResponse;
import com.stockguard.data.dto.InvoicePullDTO;
import com.stockguard.data.dto.PagedResponse;
import com.stockguard.data.dto.SyncedInvoiceDTO;
import com.stockguard.data.dto.UserInvoiceDTO;
import com.stockguard.data.dto.UserInvoiceResponseDTO;
import com.stockguard.service.UserInvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class UserInvoiceController {

    private final UserInvoiceService userInvoiceService;

    /**
     * Get user's invoices (without items)
     * GET /api/invoices
     */
    @GetMapping
    public PagedResponse<UserInvoiceResponseDTO> getUserInvoices(@PageableDefault(size = 20) Pageable pageable) {
        Long userId = getCurrentUserId();
        Page<UserInvoiceResponseDTO> page = userInvoiceService.getUserInvoices(userId, pageable);

        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    /**
     * Get invoice with line items
     * GET /api/invoices/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserInvoiceResponseDTO> getInvoiceById(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        return userInvoiceService.getUserInvoiceById(userId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Push sync: upload a batch of device invoices (new, changed or deleted)
     * POST /api/invoices/sync
     */
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<List<SyncedInvoiceDTO>>> pushInvoices(@RequestBody List<UserInvoiceDTO> batch) {
        try {
            Long userId = getCurrentUserId();
            List<SyncedInvoiceDTO> result = userInvoiceService.pushInvoices(userId, batch);

            return ResponseEntity.ok(ApiResponse.success("Invoices synced successfully", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Invoice sync failed", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Invoice sync failed", e.getMessage()));
        }
    }

    /**
     * Pull sync: invoices changed on the server since a cursor
     * GET /api/invoices/sync?since=0&page=0&size=50
     *
     * since is the serverTime from the previous pull (epoch millis, 0 for a
     * full first sync). Response includes soft-deleted invoices so the device
     * mirrors deletions; serverTime becomes the next cursor.
     */
    @GetMapping("/sync")
    public ResponseEntity<ApiResponse<InvoicePullDTO>> pullInvoices(
            @RequestParam(defaultValue = "0") long since,
            @PageableDefault(size = 50) Pageable pageable) {
        try {
            Long userId = getCurrentUserId();
            InvoicePullDTO result = userInvoiceService.pullInvoices(userId, since, pageable);

            return ResponseEntity.ok(ApiResponse.success("Invoices pulled successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Invoice pull failed", e.getMessage()));
        }
    }

    /**
     * Delete invoice (soft delete)
     * DELETE /api/invoices/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(@PathVariable Long id) {
        try {
            Long userId = getCurrentUserId();
            userInvoiceService.deleteUserInvoice(userId, id);

            return ResponseEntity.ok(ApiResponse.success("Invoice deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Delete failed", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Delete failed", e.getMessage()));
        }
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }

        throw new IllegalStateException("User not authenticated");
    }
}
