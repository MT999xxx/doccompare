package com.example.doccompare.service;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CompareService {

    /**
     * 将测试文档与多个标准文档进行对比，找出不匹配部分
     *
     * @param testContent 测试文档内容
     * @param standardContents 多个标准文档内容的列表
     * @return 标记了错误的HTML内容
     */
    public String compareWithStandards(String testContent, List<String> standardContents) {
        // 分段处理
        List<String> testParagraphs = splitIntoParagraphs(testContent);

        // 存储标准段落集合
        Set<String> standardParagraphs = new HashSet<>();
        for (String standardContent : standardContents) {
            standardParagraphs.addAll(splitIntoParagraphs(standardContent));
        }

        // 构建输出HTML
        StringBuilder resultHtml = new StringBuilder();
        resultHtml.append("<div class=\"comparison-result\">");

        for (String paragraph : testParagraphs) {
            // 如果是空段落，直接添加
            if (paragraph.trim().isEmpty()) {
                resultHtml.append("<p></p>");
                continue;
            }

            // 查找最相似的标准段落
            String closestParagraph = findClosestParagraph(paragraph, standardParagraphs);

            if (closestParagraph == null || calculateSimilarity(paragraph, closestParagraph) < 0.7) {
                // 如果没有相似段落或相似度太低，整段标记为错误
                resultHtml.append("<p class=\"error-paragraph\">").append(escapeHtml(paragraph)).append("</p>");
            } else {
                // 有相似段落，标记具体的差异
                resultHtml.append("<p>").append(markDifferences(paragraph, closestParagraph)).append("</p>");
            }
        }

        resultHtml.append("</div>");
        return resultHtml.toString();
    }

    /**
     * 将文本分割成段落
     */
    private List<String> splitIntoParagraphs(String text) {
        String[] paragraphs = text.split("\\n{2,}|\\r\\n{2,}");
        List<String> result = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String trimmed = paragraph.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * 计算两个文本的相似度 (余弦相似度)
     */
    public double calculateSimilarity(String text1, String text2) {
        // 将文本分割成词语或字符
        String[] tokens1 = tokenize(text1);
        String[] tokens2 = tokenize(text2);

        // 创建词频向量
        Map<String, Integer> vector1 = createFrequencyVector(tokens1);
        Map<String, Integer> vector2 = createFrequencyVector(tokens2);

        // 计算点积
        double dotProduct = 0.0;
        for (String token : vector1.keySet()) {
            if (vector2.containsKey(token)) {
                dotProduct += vector1.get(token) * vector2.get(token);
            }
        }

        // 计算向量模长
        double magnitude1 = calculateMagnitude(vector1);
        double magnitude2 = calculateMagnitude(vector2);

        // 计算余弦相似度
        if (magnitude1 > 0 && magnitude2 > 0) {
            return dotProduct / (magnitude1 * magnitude2);
        } else {
            return 0.0;
        }
    }

    /**
     * 对文本进行分词，对于中文，按字符分割
     */
    private String[] tokenize(String text) {
        // 对于中文文本，可以考虑按字符分割
        text = text.toLowerCase();
        // 移除标点符号
        text = text.replaceAll("[\\p{P}\\p{S}]", "");
        // 按单个字符分割
        return text.split("");
    }

    /**
     * 创建词频向量
     */
    private Map<String, Integer> createFrequencyVector(String[] tokens) {
        Map<String, Integer> vector = new HashMap<>();
        for (String token : tokens) {
            if (token.length() > 0) {
                vector.put(token, vector.getOrDefault(token, 0) + 1);
            }
        }
        return vector;
    }

    /**
     * 计算向量模长
     */
    private double calculateMagnitude(Map<String, Integer> vector) {
        double sumOfSquares = 0.0;
        for (int count : vector.values()) {
            sumOfSquares += count * count;
        }
        return Math.sqrt(sumOfSquares);
    }

    /**
     * 在一组标准段落中找到与给定段落最相似的段落
     */
    private String findClosestParagraph(String paragraph, Set<String> standardParagraphs) {
        String closestParagraph = null;
        double highestSimilarity = 0.0;

        for (String standardParagraph : standardParagraphs) {
            double similarity = calculateSimilarity(paragraph, standardParagraph);
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                closestParagraph = standardParagraph;
            }
        }

        return closestParagraph;
    }

    /**
     * 标记两个文本之间的差异
     */
    private String markDifferences(String text, String standardText) {
        // 字符级别的差异标记
        StringBuilder result = new StringBuilder();

        // 使用最长公共子序列算法找出共同部分
        int[][] lcs = computeLCS(text, standardText);

        // 根据LCS回溯并标记差异
        int i = text.length();
        int j = standardText.length();
        Stack<Character> stack = new Stack<>();

        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && text.charAt(i-1) == standardText.charAt(j-1)) {
                // 字符匹配，保留原字符
                stack.push(text.charAt(i-1));
                i--;
                j--;
            } else if (j > 0 && (i == 0 || lcs[i][j-1] >= lcs[i-1][j])) {
                // 标准文本中的字符在测试文本中缺失
                j--;
            } else {
                // 测试文本中的字符与标准不匹配
                stack.push('※'); // 标记为需要高亮的字符
                stack.push(text.charAt(i-1));
                i--;
            }
        }

        // 构建带有高亮标记的HTML
        boolean inHighlight = false;
        while (!stack.isEmpty()) {
            char c = stack.pop();
            if (c == '※') {
                if (!inHighlight) {
                    result.append("<span class=\"highlight\">");
                    inHighlight = true;
                }
            } else {
                result.append(escapeHtml(String.valueOf(c)));
                if (inHighlight && stack.size() > 0 && stack.peek() != '※') {
                    result.append("</span>");
                    inHighlight = false;
                }
            }
        }

        if (inHighlight) {
            result.append("</span>");
        }

        return result.toString();
    }

    /**
     * 计算最长公共子序列的动态规划表
     */
    private int[][] computeLCS(String text1, String text2) {
        int m = text1.length();
        int n = text2.length();
        int[][] dp = new int[m+1][n+1];

        for (int i = 1; i <= m; i++) {
            for (int j = 1; j <= n; j++) {
                if (text1.charAt(i-1) == text2.charAt(j-1)) {
                    dp[i][j] = dp[i-1][j-1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
                }
            }
        }

        return dp;
    }

    /**
     * 简单的HTML转义
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 提取文档中的所有段落，用于段落级比较
     */
    public List<Map<String, Object>> extractParagraphsForComparison(String content) {
        List<String> paragraphs = splitIntoParagraphs(content);
        List<Map<String, Object>> result = new ArrayList<>();

        for (String paragraph : paragraphs) {
            Map<String, Object> paragraphInfo = new HashMap<>();
            paragraphInfo.put("text", paragraph);
            paragraphInfo.put("hash", Integer.toString(paragraph.hashCode()));
            result.add(paragraphInfo);
        }

        return result;
    }
}