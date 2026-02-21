package com.stockguard.controller;

import com.stockguard.data.dto.productImporter.ImportResult;
import com.stockguard.service.ProductImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ProductImportController {

    private final ProductImportService importService;

    @PostMapping("/products")
    public String importProducts(@RequestParam(defaultValue = "products.json") String fileName) {
        try {
            importService.importFromJson(fileName);
            return "Import completed successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Import failed: " + e.getMessage();
        }
    }

    @PostMapping("/snapp")
    public ResponseEntity<?> importSnapp(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            importService.importFromSnapp(file.getInputStream());
            return ResponseEntity.ok("Snapp import started");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read uploaded file", e);
        }
    }


}
