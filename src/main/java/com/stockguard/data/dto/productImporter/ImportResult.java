package com.stockguard.data.dto.productImporter;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ImportResult {
    int created;
    int skipped;
    int failed;
}