package com.example.doccompare.service;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private final ResourceLoader resourceLoader;

    public FileService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * 存储上传的文件并返回文件路径
     */
    public String storeFile(MultipartFile file) throws IOException {
        // 创建上传目录
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // 生成唯一文件名
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, filename);

        // 保存文件
        Files.copy(file.getInputStream(), filePath);

        return filePath.toString();
    }

    /**
     * 读取文件内容
     */
    public String readFileContent(String filePath) throws IOException {
        File file = new File(filePath);
        return extractTextFromFile(file);
    }

    /**
     * 读取MultipartFile内容
     */
    public String readFileContent(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            String fileName = file.getOriginalFilename();
            if (fileName == null) {
                throw new IOException("文件名不能为空");
            }

            System.out.println("读取上传文件内容: " + fileName);
            return extractTextFromInputStream(inputStream, fileName);
        }
    }

    /**
     * 从目录中读取所有标准文档内容
     */
    public List<String> readStandardDocuments(String standardDocsPath) throws IOException {
        List<String> contents = new ArrayList<>();
        File directory = new File(standardDocsPath);

        System.out.println("尝试读取标准文档目录: " + directory.getAbsolutePath());

        if (!directory.exists()) {
            System.out.println("目录不存在: " + directory.getAbsolutePath());
            return contents; // 返回空列表而不是抛出异常
        }

        if (!directory.isDirectory()) {
            System.out.println("路径不是目录: " + directory.getAbsolutePath());
            return contents; // 返回空列表而不是抛出异常
        }

        File[] files = directory.listFiles();
        if (files == null || files.length == 0) {
            System.out.println("目录为空: " + directory.getAbsolutePath());
            return contents;
        }

        System.out.println("找到 " + files.length + " 个文件");

        for (File file : files) {
            if (file.isFile()) {
                try {
                    System.out.println("尝试读取文件: " + file.getName());
                    String content = extractTextFromFile(file);
                    contents.add(content);
                    System.out.println("成功读取文件: " + file.getName() + ", 内容长度: " + content.length());
                } catch (Exception e) {
                    System.out.println("读取文件失败: " + file.getName() + " - " + e.getMessage());
                    // 继续处理下一个文件
                }
            }
        }

        System.out.println("成功读取 " + contents.size() + " 个标准文档");
        return contents;
    }

    /**
     * 从资源目录读取标准文档
     */
    public List<String> readStandardDocumentsFromResources(String resourcePath) throws IOException {
        List<String> contents = new ArrayList<>();

        System.out.println("尝试读取资源: " + resourcePath);

        try {
            // 第一种方法：使用ClassPathResource
            Resource[] resources = null;
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

            try {
                // 尝试匹配所有类型的文件
                resources = resolver.getResources("classpath*:" + resourcePath + "/*.*");
                System.out.println("找到 " + resources.length + " 个资源文件");

                for (Resource resource : resources) {
                    try (InputStream is = resource.getInputStream()) {
                        if (is != null) {
                            String fileName = resource.getFilename();
                            System.out.println("读取资源文件: " + fileName);
                            String content = extractTextFromInputStream(is, fileName);
                            contents.add(content);
                            System.out.println("成功添加资源内容: " + fileName);
                        }
                    } catch (Exception e) {
                        System.out.println("读取资源失败: " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                System.out.println("获取资源列表失败: " + e.getMessage());
            }

            // 如果第一种方法失败，尝试第二种方法
            if (contents.isEmpty()) {
                try {
                    resources = resolver.getResources("classpath*:/" + resourcePath + "/*.*");
                    System.out.println("第二种方式找到 " + resources.length + " 个资源文件");

                    for (Resource resource : resources) {
                        try (InputStream is = resource.getInputStream()) {
                            if (is != null) {
                                String fileName = resource.getFilename();
                                System.out.println("读取资源文件: " + fileName);
                                String content = extractTextFromInputStream(is, fileName);
                                contents.add(content);
                                System.out.println("成功添加资源内容: " + fileName);
                            }
                        } catch (Exception e) {
                            System.out.println("读取资源失败: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("第二种方式获取资源列表失败: " + e.getMessage());
                }
            }

            // 如果第二种方法失败，尝试第三种方法 - 直接加载特定文件
            if (contents.isEmpty()) {
                String[] defaultFiles = {"G-CG-00001.txt", "G-CG-00002.txt", "G-CG-00003.txt"};
                for (String fileName : defaultFiles) {
                    try {
                        Resource resource = resourceLoader.getResource("classpath:standards/" + fileName);
                        if (resource.exists()) {
                            try (InputStream is = resource.getInputStream()) {
                                String content = extractTextFromInputStream(is, fileName);
                                contents.add(content);
                                System.out.println("成功加载特定资源文件: " + fileName);
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("加载特定资源文件失败: " + fileName + " - " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("资源读取出错: " + e.getMessage());
        }

        // 如果仍然没有找到内容，创建默认内容
        if (contents.isEmpty()) {
            System.out.println("未找到任何标准文档资源，使用默认内容");
            String defaultContent = "这是一个系统内置的默认标准文档。\n" +
                    "请管理员通过管理界面上传正式的标准文档。\n" +
                    "标准文档通常包含政府采购法、招投标法等法规内容。";
            contents.add(defaultContent);
        }

        return contents;
    }

    /**
     * 从文件中提取文本
     */
    private String extractTextFromFile(File file) throws IOException {
        String fileName = file.getName();
        System.out.println("提取文件内容: " + fileName);

        try (FileInputStream fis = new FileInputStream(file)) {
            return extractTextFromInputStream(fis, fileName);
        }
    }

    /**
     * 从输入流中提取文本
     */
    private String extractTextFromInputStream(InputStream inputStream, String fileName) throws IOException {
        String lowercaseFileName = fileName.toLowerCase();

        try {
            if (lowercaseFileName.endsWith(".docx")) {
                // 处理DOCX文件
                System.out.println("处理DOCX文件");
                try (XWPFDocument document = new XWPFDocument(inputStream)) {
                    XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                    String text = extractor.getText();
                    System.out.println("提取的文本长度: " + text.length());
                    return text;
                }
            } else if (lowercaseFileName.endsWith(".doc")) {
                // 处理DOC文件
                System.out.println("处理DOC文件");
                try (HWPFDocument document = new HWPFDocument(inputStream)) {
                    WordExtractor extractor = new WordExtractor(document);
                    String text = extractor.getText();
                    System.out.println("提取的文本长度: " + text.length());
                    return text;
                }
            } else if (lowercaseFileName.endsWith(".txt")) {
                // 处理纯文本文件
                System.out.println("处理TXT文件");
                byte[] bytes = inputStream.readAllBytes();
                String text = new String(bytes);
                System.out.println("提取的文本长度: " + text.length());
                return text;
            } else {
                // 尝试当作TXT处理
                System.out.println("未知文件类型: " + lowercaseFileName + "，尝试当作TXT处理");
                byte[] bytes = inputStream.readAllBytes();
                String text = new String(bytes);
                System.out.println("提取的文本长度: " + text.length());
                return text;
            }
        } catch (Exception e) {
            System.out.println("提取文本失败: " + e.getMessage());
            throw new IOException("提取文本失败: " + e.getMessage(), e);
        }
    }
}