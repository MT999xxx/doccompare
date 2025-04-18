package com.example.doccompare.model;

import lombok.Data;

@Data
public class CompareResult {
    private String originalText;
    private String comparedText;
    private double similarityScore;
    private String highlightedText; // 高亮显示相似部分

    public CompareResult(String originalText, String comparedText) {
        this.originalText = originalText;
        this.comparedText = comparedText;
    }
}
