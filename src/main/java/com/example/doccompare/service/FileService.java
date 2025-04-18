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

            return extractTextFromInputStream(inputStream, fileName);
        }
    }

    /**
     * 从目录中读取所有标准文档内容
     */
    public List<String> readStandardDocuments(String standardDocsPath) throws IOException {
        List<String> contents = new ArrayList<>();
        File directory = new File(standardDocsPath);

        if (!directory.exists() || !directory.isDirectory()) {
            throw new IOException("标准文档目录不存在或不是一个目录");
        }

        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("无法读取标准文档目录");
        }

        for (File file : files) {
            if (file.isFile() && isDocumentFile(file.getName())) {
                contents.add(extractTextFromFile(file));
            }
        }

        return contents;
    }

    /**
     * 从资源目录读取标准文档
     */
    public List<String> readStandardDocumentsFromResources(String resourcePath) throws IOException {
        List<String> contents = new ArrayList<>();
        Resource resource = resourceLoader.getResource("classpath:" + resourcePath);

        if (resource.exists() && resource.isFile()) {
            // 单个文件
            try (InputStream inputStream = resource.getInputStream()) {
                contents.add(extractTextFromInputStream(inputStream, resource.getFilename()));
            }
        } else if (resource.exists() && resource.isReadable()) {
            // 目录
            File directory = resource.getFile();
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && isDocumentFile(file.getName())) {
                        contents.add(extractTextFromFile(file));
                    }
                }
            }
        }

        return contents;
    }

    /**
     * 从文件中提取文本
     */
    private String extractTextFromFile(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        try (FileInputStream fis = new FileInputStream(file)) {
            return extractTextFromInputStream(fis, fileName);
        }
    }

    /**
     * 从输入流中提取文本
     */
    private String extractTextFromInputStream(InputStream inputStream, String fileName) throws IOException {
        if (fileName.endsWith(".docx")) {
            // 处理DOCX文件
            try (XWPFDocument document = new XWPFDocument(inputStream)) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                return extractor.getText();
            }
        } else if (fileName.endsWith(".doc")) {
            // 处理DOC文件
            try (HWPFDocument document = new HWPFDocument(inputStream)) {
                WordExtractor extractor = new WordExtractor(document);
                return extractor.getText();
            }
        } else if (fileName.endsWith(".txt")) {
            // 处理纯文本文件
            return new String(inputStream.readAllBytes());
        } else {
            throw new IOException("不支持的文件格式: " + fileName);
        }
    }

    /**
     * 检查文件是否为支持的文档类型
     */
    private boolean isDocumentFile(String fileName) {
        fileName = fileName.toLowerCase();
        return fileName.endsWith(".doc") || fileName.endsWith(".docx") || fileName.endsWith(".txt");
    }
}