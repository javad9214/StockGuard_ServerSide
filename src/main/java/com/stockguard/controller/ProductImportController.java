package com.stockguard.controller;

import com.stockguard.service.ProductImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ProductImportController {

    private final ProductImportService importService;

    @PostMapping("/products")
    public String importProducts(@RequestParam String filePath) {
        try {
            importService.importFromJson(filePath);
            return "Import completed successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Import failed: " + e.getMessage();
        }
    }
}
