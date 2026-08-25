package com.stockguard.service.impl;

import com.stockguard.data.dto.InvoicePullDTO;
import com.stockguard.data.dto.SyncedInvoiceDTO;
import com.stockguard.data.dto.UserInvoiceDTO;
import com.stockguard.data.dto.UserInvoiceItemDTO;
import com.stockguard.data.dto.UserInvoiceResponseDTO;
import com.stockguard.data.entity.UserInvoice;
import com.stockguard.data.entity.UserInvoiceProduct;
import com.stockguard.repository.UserInvoiceRepository;
import com.stockguard.repository.UserProductRepository;
import com.stockguard.service.UserInvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInvoiceServiceImpl implements UserInvoiceService {

    // One transaction per push batch — beyond this the device should split,
    // otherwise a retry re-sends and re-processes an unboundedly large batch
    private static final int MAX_PUSH_BATCH = 1000;

    private final UserInvoiceRepository userInvoiceRepository;
    private final UserProductRepository userProductRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<UserInvoiceResponseDTO> getUserInvoices(Long userId, Pageable pageable) {
        return userInvoiceRepository.findByUserIdAndIsDeletedFalse(userId, pageable)
                .map(UserInvoiceResponseDTO::summary);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserInvoiceResponseDTO> getUserInvoiceById(Long userId, Long invoiceId) {
        return userInvoiceRepository.findWithItemsByIdAndUserId(invoiceId, userId)
                .map(UserInvoiceResponseDTO::detail);
    }

    @Override
    @Transactional
    public List<SyncedInvoiceDTO> pushInvoices(Long userId, List<UserInvoiceDTO> batch) {
        if (batch == null || batch.isEmpty()) {
            return List.of();
        }
        if (batch.size() > MAX_PUSH_BATCH) {
            throw new IllegalArgumentException("Push batch is limited to " + MAX_PUSH_BATCH + " invoices per request");
        }

        validateBatch(batch);
        validateProductOwnership(userId, batch);

        List<SyncedInvoiceDTO> result = new ArrayList<>(batch.size());
        for (UserInvoiceDTO dto : batch) {
            // Upsert by (userId, localId): a retried batch after a network drop
            // updates the same server row instead of duplicating the invoice
            UserInvoice invoice = userInvoiceRepository.findByUserIdAndLocalId(userId, dto.getLocalId())
                    .orElseGet(UserInvoice::new);

            invoice.setUserId(userId);
            invoice.setLocalId(dto.getLocalId());
            invoice.setPrefix(dto.getPrefix() != null ? dto.getPrefix() : "INV");
            invoice.setInvoiceNumber(dto.getInvoiceNumber());
            invoice.setInvoiceDate(dto.getInvoiceDate());
            invoice.setInvoiceType(dto.getInvoiceType());
            invoice.setCustomerId(dto.getCustomerId());
            invoice.setTotalAmount(dto.getTotalAmount());
            invoice.setTotalProfit(dto.getTotalProfit());
            invoice.setTotalDiscount(dto.getTotalDiscount() != null ? dto.getTotalDiscount() : 0L);
            invoice.setStatus(dto.getStatus());
            invoice.setPaymentMethod(dto.getPaymentMethod());
            invoice.setNotes(dto.getNotes());
            invoice.setIsDeleted(Boolean.TRUE.equals(dto.getIsDeleted()));
            applyItems(invoice, dto.getItems());

            UserInvoice saved = userInvoiceRepository.save(invoice);
            result.add(new SyncedInvoiceDTO(saved.getLocalId(), saved.getId()));
        }

        log.info("Pushed {} invoices for user {}", result.size(), userId);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public InvoicePullDTO pullInvoices(Long userId, long sinceEpochMillis, Pageable pageable) {
        // Cursor is captured before the query: anything committed after this
        // moment has updatedAt >= serverTime and is caught by the next pull
        long serverTime = System.currentTimeMillis();
        LocalDateTime since = LocalDateTime.ofInstant(Instant.ofEpochMilli(sinceEpochMillis), ZoneId.systemDefault());

        Page<Long> idPage = userInvoiceRepository.findIdsByUserIdAndUpdatedAtGreaterThanEqual(userId, since, pageable);
        List<UserInvoiceResponseDTO> content = List.of();
        if (!idPage.isEmpty()) {
            Map<Long, UserInvoice> invoicesById = userInvoiceRepository.findWithItemsByIdIn(idPage.getContent())
                    .stream()
                    .collect(Collectors.toMap(UserInvoice::getId, Function.identity()));
            content = idPage.getContent().stream()
                    .map(invoicesById::get)
                    .map(UserInvoiceResponseDTO::detail)
                    .toList();
        }

        return InvoicePullDTO.of(serverTime, content, idPage);
    }

    @Override
    @Transactional
    public void deleteUserInvoice(Long userId, Long invoiceId) {
        log.info("Deleting invoice {} for user {}", invoiceId, userId);

        UserInvoice invoice = userInvoiceRepository.findWithItemsByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        invoice.setIsDeleted(true);
        userInvoiceRepository.save(invoice);
    }

    private void validateBatch(List<UserInvoiceDTO> batch) {
        for (UserInvoiceDTO dto : batch) {
            if (dto.getLocalId() == null) {
                throw new IllegalArgumentException("localId is required for every pushed invoice");
            }
            if (dto.getInvoiceNumber() == null) {
                throw new IllegalArgumentException("invoiceNumber is required (localId=" + dto.getLocalId() + ")");
            }
            if (dto.getInvoiceDate() == null) {
                throw new IllegalArgumentException("invoiceDate is required (localId=" + dto.getLocalId() + ")");
            }
            if (dto.getItems() != null) {
                for (UserInvoiceItemDTO item : dto.getItems()) {
                    if (item.getProductId() == null || item.getQuantity() == null
                            || item.getPriceAtSale() == null || item.getCostPriceAtTransaction() == null) {
                        throw new IllegalArgumentException(
                                "Each item needs productId, quantity, priceAtSale and costPriceAtTransaction (localId="
                                        + dto.getLocalId() + ")");
                    }
                }
            }
        }
    }

    /**
     * Line items must reference the user's own products — a foreign user's
     * product id would silently poison per-product sales reports. Deleted
     * products are allowed: invoices are historical and carry price snapshots.
     */
    private void validateProductOwnership(Long userId, List<UserInvoiceDTO> batch) {
        Set<Long> productIds = batch.stream()
                .filter(dto -> dto.getItems() != null && !Boolean.TRUE.equals(dto.getIsDeleted()))
                .flatMap(dto -> dto.getItems().stream())
                .map(UserInvoiceItemDTO::getProductId)
                .collect(Collectors.toSet());
        if (productIds.isEmpty()) {
            return;
        }

        Set<Long> owned = new HashSet<>(userProductRepository.findIdsByUserIdAndIdIn(userId, productIds));
        List<Long> missing = productIds.stream()
                .filter(id -> !owned.contains(id))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException("Products not found for this user: " + missing);
        }
    }

    private void applyItems(UserInvoice invoice, List<UserInvoiceItemDTO> items) {
        List<UserInvoiceProduct> entities = new ArrayList<>();
        if (items != null && !Boolean.TRUE.equals(invoice.getIsDeleted())) {
            for (UserInvoiceItemDTO dto : items) {
                UserInvoiceProduct item = dto.toEntity();
                // The device may not compute totals itself — derive when absent
                if (item.getTotal() == null) {
                    item.setTotal((item.getPriceAtSale() - item.getDiscount()) * item.getQuantity());
                }
                entities.add(item);
            }
        }
        // Wholesale replace: orphanRemoval deletes the rows that dropped out
        invoice.setItems(entities);
    }
}
