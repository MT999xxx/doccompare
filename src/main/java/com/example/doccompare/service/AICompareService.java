package com.example.doccompare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * AI语义比对服务，负责调用AI模型API进行文档语义层面的比对
 */
@Service
public class AICompareService {

    private final RestTemplate restTemplate;

    @Value("${ai.api.url:#{null}}")
    private String aiApiUrl;

    @Value("${ai.api.key:#{null}}")
    private String aiApiKey;

    @Value("${ai.api.secret:#{null}}") // 添加Secret Key配置
    private String aiApiSecret;

    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        if (aiApiUrl != null && aiApiKey != null && aiApiSecret != null) {
            System.out.println("AI服务已配置: " + aiApiUrl);
        } else {
            System.out.println("警告: AI服务未正确配置，将使用基础比对");
        }
    }

    public AICompareService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * 使用AI进行语义比对
     */
    public String semanticCompare(String testContent, List<String> standardContents) {
        List<Map<String, Object>> results = new ArrayList<>();  // 注意这里改为Map<String, Object>

        // 将测试内容分段
        List<String> testParagraphs = splitIntoParagraphs(testContent);

        // 对每个测试段落进行语义比对
        for (String paragraph : testParagraphs) {
            if (paragraph.trim().isEmpty()) {
                continue;
            }

            // 找到语义上最匹配的标准段落
            Map<String, Object> bestMatch = findBestSemanticMatch(paragraph, standardContents);

            if (bestMatch != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> differences = (List<Map<String, Object>>) bestMatch.get("differences");

                // 只处理存在差异的段落
                if (differences != null && !differences.isEmpty()) {
                    Map<String, Object> result = new HashMap<>();  // 使用Map<String, Object>
                    result.put("testParagraph", paragraph);
                    result.put("standardParagraph", bestMatch.get("paragraph"));
                    result.put("differences", differences);  // 直接存储differences对象
                    results.add(result);
                }
            }
        }

        // 构建输出HTML
        return buildResultHtml(results);
    }
    /**
     * 调用AI API进行语义匹配
     */
    private Map<String, Object> findBestSemanticMatch(String paragraph, List<String> standardContents) {
        try {
            if (aiApiUrl == null || aiApiKey == null) {
                return fallbackToSimilarityMatch(paragraph, standardContents);
            }

            // 准备请求体 - 按照DeepSeek API格式
            Map<String, Object> requestBody = new HashMap<>();

            // 构建消息
            List<Map<String, String>> messages = new ArrayList<>();

            // 系统消息 - 明确指示AI的任务和响应格式
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个专业的政府采购文档比对专家，擅长识别文本中的语义差异，尤其是针对时间表述（如'5个工作日'与'14个工作日'）、百分比（如'30%'与'50%'）和其他重要条款的差异。你的任务是精确识别测试文本与标准文本间的差异，并仅返回存在差异的部分。如果没有差异，返回空数组。");
            messages.add(systemMessage);

            // 用户消息 - 更精确的指导和格式要求
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");

            StringBuilder prompt = new StringBuilder();
            prompt.append("请比对以下测试文本与标准文本，找出所有语义差异：\n\n");
            prompt.append("测试文本：'").append(paragraph).append("'\n\n");
            prompt.append("标准文本：\n");

            for (int i = 0; i < standardContents.size(); i++) {
                prompt.append((i+1) + ". '").append(standardContents.get(i)).append("'\n");
            }

            prompt.append("\n特别关注以下差异类型：\n");
            prompt.append("1. 时间表述差异（如'5个工作日'与'14个工作日'、'30天'与'两个月'）\n");
            prompt.append("2. 百分比差异（如'30%'与'50%'）\n");
            prompt.append("3. 金额或数量表述差异\n");
            prompt.append("4. 责任主体差异\n\n");

            // 修改提示词部分
            prompt.append("请以JSON格式返回结果，格式如下：\n");
            prompt.append("{\n");
            prompt.append("  \"paragraph\": \"最匹配的标准文本\",\n");
            prompt.append("  \"differences\": [\n");
            prompt.append("    {\n");
            prompt.append("      \"type\": \"差异类型\",\n");
            prompt.append("      \"standardText\": \"标准文本中的完整句子或条款\",\n");
            prompt.append("      \"testText\": \"测试文本中的完整句子或条款\",\n");
            prompt.append("      \"summary\": \"用一句简洁的话概括这项差异\"\n");
            prompt.append("    }\n");
            prompt.append("  ]\n");
            prompt.append("}\n\n");
            prompt.append("重要：如果没有检测到差异，请返回空的differences数组。不要输出任何不相关内容。");

            userMessage.put("content", prompt.toString());
            messages.add(userMessage);

            // 设置DeepSeek API参数
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", messages);
            requestBody.put("stream", false);
            requestBody.put("temperature", 0.1); // 降低随机性，使结果更确定性

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + aiApiKey);

            // 发送请求
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(aiApiUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                // DeepSeek API返回格式处理
                if (responseBody.containsKey("choices") && ((List)responseBody.get("choices")).size() > 0) {
                    Map<String, Object> choice = (Map<String, Object>) ((List)responseBody.get("choices")).get(0);
                    if (choice.containsKey("message")) {
                        Map<String, Object> message = (Map<String, Object>) choice.get("message");
                        String content = (String) message.get("content");

                        // 解析返回的JSON内容
                        try {
                            // 查找JSON部分
                            int startIndex = content.indexOf('{');
                            int endIndex = content.lastIndexOf('}') + 1;

                            if (startIndex >= 0 && endIndex > startIndex) {
                                String jsonStr = content.substring(startIndex, endIndex);
                                // 使用Jackson解析JSON
                                ObjectMapper mapper = new ObjectMapper();
                                return mapper.readValue(jsonStr, Map.class);
                            }
                        } catch (Exception e) {
                            System.out.println("解析DeepSeek返回JSON失败: " + e.getMessage());
                        }
                    }
                }
            }

            return fallbackToSimilarityMatch(paragraph, standardContents);
        } catch (Exception e) {
            e.printStackTrace();
            return fallbackToSimilarityMatch(paragraph, standardContents);
        }
    }
    /**
     * 解析AI返回的JSON响应
     */
    private Map<String, Object> parseAIResponse(String aiResponse) {
        try {
            // 尝试找到JSON结构的开始和结束
            int startIndex = aiResponse.indexOf('{');
            int endIndex = aiResponse.lastIndexOf('}') + 1;

            if (startIndex >= 0 && endIndex > startIndex) {
                String jsonStr = aiResponse.substring(startIndex, endIndex);
                System.out.println("提取的JSON: " + jsonStr);

                // 使用Jackson解析JSON
                Map<String, Object> result = objectMapper.readValue(jsonStr, Map.class);
                return result;
            } else {
                System.out.println("无法在AI响应中找到有效的JSON结构");
            }
        } catch (Exception e) {
            System.out.println("解析AI响应异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取百度API的access_token
     */
    private String getAccessToken() {
        try {
            String url = "https://aip.baidubce.com/oauth/2.0/token";
            String param = "grant_type=client_credentials&client_id=" + aiApiKey + "&client_secret=" + aiApiSecret;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>(param, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                if (responseBody.containsKey("access_token")) {
                    return (String) responseBody.get("access_token");
                } else {
                    System.out.println("获取access_token失败，返回内容: " + responseBody);
                }
            } else {
                System.out.println("获取access_token请求失败, 状态码: " + response.getStatusCodeValue());
            }
        } catch (Exception e) {
            System.out.println("获取access_token异常: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 回退到基于相似度的匹配（当AI API不可用时）
     */
    private Map<String, Object> fallbackToSimilarityMatch(String paragraph, List<String> standardContents) {
        double bestSimilarity = 0.0;
        String bestMatch = null;

        for (String standardContent : standardContents) {
            List<String> standardParagraphs = splitIntoParagraphs(standardContent);

            for (String standardParagraph : standardParagraphs) {
                double similarity = calculateSimilarity(paragraph, standardParagraph);
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = standardParagraph;
                }
            }
        }

        if (bestSimilarity > 0.4 && bestMatch != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("paragraph", bestMatch);

            // 简单地检测不同的片段
            List<Map<String, Object>> differences = detectBasicDifferences(paragraph, bestMatch);
            result.put("differences", differences);

            return result;
        }

        return null;
    }

    /**
     * 基础的差异检测，用于回退处理
     */
    private List<Map<String, Object>> detectBasicDifferences(String test, String standard) {
        List<Map<String, Object>> differences = new ArrayList<>();

        // 时间模式匹配
        Map<String, List<String>> timePatterns = new HashMap<>();

        // 天数模式
        timePatterns.put("days", Arrays.asList(
                "(\\d+)\\s*天", "(\\d+)\\s*日", "三十天", "二十天", "十天", "七天", "7天",
                "\\d+天内", "30天", "三十天内"
        ));

        // 月份模式
        timePatterns.put("months", Arrays.asList(
                "(\\d+)\\s*个?\\s*月", "一个月", "两个月", "2个月", "三个月", "数月",
                "\\d+个月内", "两个月内", "2个月内"
        ));

        // 检测标准文档中的时间表述
        Map<String, String> standardTimeExpressions = extractTimeExpressions(standard, timePatterns);

        // 检测测试文档中的时间表述
        Map<String, String> testTimeExpressions = extractTimeExpressions(test, timePatterns);

        // 比较两者的差异
        for (String timeType : standardTimeExpressions.keySet()) {
            String standardExpr = standardTimeExpressions.get(timeType);
            String testExpr = testTimeExpressions.get(timeType);

            if (testExpr != null && !testExpr.equals(standardExpr)) {
                Map<String, Object> diff = new HashMap<>();
                diff.put("type", "time_expression");
                diff.put("standardText", standardExpr);
                diff.put("testText", testExpr);
                differences.add(diff);
            }
        }

        // 检测跨类型差异（天 vs 月）
        if (standardTimeExpressions.containsKey("days") && testTimeExpressions.containsKey("months")) {
            Map<String, Object> diff = new HashMap<>();
            diff.put("type", "time_expression");
            diff.put("standardText", standardTimeExpressions.get("days"));
            diff.put("testText", testTimeExpressions.get("months"));
            differences.add(diff);
        } else if (standardTimeExpressions.containsKey("months") && testTimeExpressions.containsKey("days")) {
            Map<String, Object> diff = new HashMap<>();
            diff.put("type", "time_expression");
            diff.put("standardText", standardTimeExpressions.get("months"));
            diff.put("testText", testTimeExpressions.get("days"));
            differences.add(diff);
        }

        return differences;
    }

    /**
     * 从文本中提取时间表述
     */
    private Map<String, String> extractTimeExpressions(String text, Map<String, List<String>> patterns) {
        Map<String, String> expressions = new HashMap<>();

        for (String timeType : patterns.keySet()) {
            for (String pattern : patterns.get(timeType)) {
                java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher m = p.matcher(text);

                if (m.find()) {
                    expressions.put(timeType, m.group(0));
                    break;
                }
            }
        }

        return expressions;
    }

    /**
     * 标记语义差异
     */
    private String markSemanticDifferences(String paragraph, List<Map<String, Object>> differences) {
        if (differences == null || differences.isEmpty()) {
            return escapeHtml(paragraph);
        }

        String result = escapeHtml(paragraph);

        // 对于每个差异，添加高亮标记
        for (Map<String, Object> diff : differences) {
            String type = (String) diff.get("type");
            String testText = (String) diff.get("testText");
            String standardText = (String) diff.get("standardText");

            if (testText != null && !testText.isEmpty()) {
                // 创建更详细的提示信息
                String tooltipText = "";
                if ("time_expression".equals(type)) {
                    tooltipText = "时间表述不符：标准要求 \"" + standardText + "\"";
                } else if ("percentage".equals(type)) {
                    tooltipText = "百分比不符：标准要求 \"" + standardText + "\"";
                } else if ("amount".equals(type)) {
                    tooltipText = "金额/数量不符：标准要求 \"" + standardText + "\"";
                } else if ("responsibility".equals(type)) {
                    tooltipText = "责任主体不符：标准要求 \"" + standardText + "\"";
                } else {
                    tooltipText = "内容不符：标准要求 \"" + standardText + "\"";
                }

                // 替换为高亮显示
                result = result.replace(
                        testText,
                        "<span class=\"highlight\" title=\"" + tooltipText + "\">" + testText + "</span>"
                );
            }
        }

        return result;
    }
    /**
     * 构建结果HTML
     */
    /**
     * 构建结果HTML
     */
    private String buildResultHtml(List<Map<String, Object>> results) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"comparison-result\">");

        // 收集所有差异
        List<Map<String, Object>> allDifferences = new ArrayList<>();

        // 从所有结果中提取差异
        for (Map<String, Object> result : results) {
            if (result.containsKey("differences")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> differences = (List<Map<String, Object>>) result.get("differences");
                if (differences != null) {
                    allDifferences.addAll(differences);
                }
            }
        }

        // 如果找到差异，以编号列表形式显示
        if (!allDifferences.isEmpty()) {
            html.append("<h4 class=\"mb-3\">发现以下差异：</h4>");
            html.append("<ol class=\"differences-list\">");

            for (Map<String, Object> diff : allDifferences) {
                String standardText = (String) diff.get("standardText");
                String testText = (String) diff.get("testText");
                String summary = (String) diff.get("summary");

                html.append("<li class=\"mb-3\">");
                if (summary != null && !summary.isEmpty()) {
                    html.append(summary);
                } else {
                    html.append("<strong>").append(testText).append("</strong> - 应为: ").append(standardText);
                }
                html.append("</li>");
            }

            html.append("</ol>");
        } else {
            html.append("<div class=\"alert alert-success\"><i class=\"bi bi-check-circle me-2\"></i>文档检测完毕，未发现问题</div>");
        }

        html.append("</div>");
        return html.toString();
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
     * 计算两个文本的相似度（余弦相似度）
     */
    private double calculateSimilarity(String text1, String text2) {
        // 分词
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
     * 对文本进行分词
     */
    private String[] tokenize(String text) {
        text = text.toLowerCase();
        text = text.replaceAll("[\\p{P}\\p{S}]", "");
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
     * HTML转义
     */
    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}