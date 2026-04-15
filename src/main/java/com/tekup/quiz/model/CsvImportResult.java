package com.tekup.quiz.model;

import java.util.List;
import java.util.Objects;

public class CsvImportResult {
    private final int importedCount;
    private final List<String> errors;

    public CsvImportResult(int importedCount, List<String> errors) {
        if (importedCount < 0) {
            throw new IllegalArgumentException("importedCount must be non-negative");
        }
        this.importedCount = importedCount;
        this.errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }

    public int getImportedCount() {
        return importedCount;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
