package com.example.doccompare.controller;

import com.example.doccompare.model.User;
import com.example.doccompare.service.AICompareService;
import com.example.doccompare.service.CompareService;
import com.example.doccompare.service.FileService;
import com.example.doccompare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Controller
public class CompareController {

    private final FileService fileService;
    private final CompareService compareService;
    private final AICompareService aiCompareService;
    private final UserService userService;

    @Value("${app.standards.dir:./standards}")
    private String standardsDir;

    @Value("${app.use.ai.comparison:true}")
    private boolean useAiComparison;

    @Value("${app.compare.credit.cost:1}")
    private int compareCredits;

    @Autowired
    public CompareController(FileService fileService, CompareService compareService,
                             AICompareService aiCompareService, UserService userService) {
        this.fileService = fileService;
        this.compareService = compareService;
        this.aiCompareService = aiCompareService;
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        // 检查是否已登录
        if (session.getAttribute("currentUser") == null) {
            return "redirect:/login";
        }
        return "index";
    }

    @GetMapping("/compare")
    public String compareForm(HttpSession session) {
        // 检查是否已登录
        if (session.getAttribute("currentUser") == null) {
            return "redirect:/login";
        }
        return "compare";
    }

    @PostMapping("/compare")
    public String compareFiles(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "standardFiles", required = false) List<MultipartFile> standardFiles,
                               @RequestParam(value = "useAI", required = false, defaultValue = "false") boolean useAI,
                               HttpSession session,
                               Model model) {

        // 检查是否已登录
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 检查积分是否足够（管理员和员工免费）
        if (!currentUser.isEmployee() && !currentUser.isAdmin() && !userService.hasEnoughCredits(currentUser.getUsername(), compareCredits)) {
            model.addAttribute("error", "积分不足，请充值后再试");
            return "compare";
        }

        try {
            // 读取上传的测试文件内容
            String testContent = fileService.readFileContent(file);

            // 构建标准内容列表
            List<String> standardContents = new ArrayList<>();

            // 处理标准文件
            if (standardFiles != null && !standardFiles.isEmpty()) {
                // 使用用户上传的标准文件
                for (MultipartFile standardFile : standardFiles) {
                    if (!standardFile.isEmpty()) {
                        standardContents.add(fileService.readFileContent(standardFile));
                    }
                }
            } else {
                // 检查当前工作目录
                System.out.println("当前工作目录: " + new File(".").getAbsolutePath());

                // 检查项目根目录下的standards文件夹
                File rootStandards = new File("./standards");
                System.out.println("根目录standards存在: " + rootStandards.exists() + ", 路径: " + rootStandards.getAbsolutePath());
                if (rootStandards.exists()) {
                    File[] files = rootStandards.listFiles();
                    if (files != null) {
                        System.out.println("根目录standards包含 " + files.length + " 个文件");
                        for (File f : files) {
                            System.out.println(" - " + f.getName());
                        }
                    }
                }

                // 检查src/main/resources/standards目录
                File resourceStandards = new File("src/main/resources/standards");
                System.out.println("resources/standards存在: " + resourceStandards.exists() + ", 路径: " + resourceStandards.getAbsolutePath());

                // 检查classpath
                try {
                    URL resourceUrl = getClass().getClassLoader().getResource("standards");
                    System.out.println("Classpath standards URL: " + (resourceUrl != null ? resourceUrl.toString() : "null"));
                } catch (Exception e) {
                    System.out.println("检查classpath失败: " + e.getMessage());
                }

                // 开始尝试读取标准文档...
                System.out.println("开始尝试读取标准文档...");

                // 1. 首先尝试读取用户配置的标准目录
                System.out.println("1. 尝试从配置的标准目录读取: " + standardsDir);
                try {
                    File directory = new File(standardsDir);
                    if (!directory.exists()) {
                        directory.mkdirs();
                        System.out.println("创建标准文档目录: " + directory.getAbsolutePath());
                    }
                    List<String> configStandards = fileService.readStandardDocuments(standardsDir);
                    if (!configStandards.isEmpty()) {
                        standardContents.addAll(configStandards);
                        System.out.println("从配置目录找到 " + configStandards.size() + " 个标准文档");
                    } else {
                        System.out.println("配置目录中没有找到标准文档");
                    }
                } catch (Exception e) {
                    System.out.println("从配置目录读取失败: " + e.getMessage());
                }

                // 2. 从源代码目录读取
                if (standardContents.isEmpty()) {
                    String srcPath = "src/main/java/com/example/doccompare/standards";
                    System.out.println("2. 尝试从源代码目录读取: " + srcPath);
                    try {
                        List<String> srcStandards = fileService.readStandardDocuments(srcPath);
                        if (!srcStandards.isEmpty()) {
                            standardContents.addAll(srcStandards);
                            System.out.println("从源代码目录找到 " + srcStandards.size() + " 个标准文档");
                        } else {
                            System.out.println("源代码目录中没有找到标准文档");
                        }
                    } catch (Exception e) {
                        System.out.println("从源代码目录读取失败: " + e.getMessage());
                    }
                }

                // 3. 直接从资源classpath读取标准文档
                if (standardContents.isEmpty()) {
                    System.out.println("3. 尝试从classpath资源读取标准文档");
                    try {
                        List<String> resourceStandardss = fileService.readStandardDocumentsFromResources("standards");
                        if (!resourceStandardss.isEmpty()) {
                            resourceStandardss.addAll(resourceStandardss);
                            System.out.println("从classpath资源找到 " + resourceStandardss.size() + " 个标准文档");
                        } else {
                            System.out.println("classpath资源中没有找到标准文档");
                        }
                    } catch (Exception e) {
                        System.out.println("从classpath资源读取失败: " + e.getMessage());
                    }
                }

                // 4. 创建默认的标准文档作为兜底
                if (standardContents.isEmpty()) {
                    System.out.println("4. 所有方法都未找到标准文档，创建默认文档");
                    try {
                        // 创建一个默认的标准文档
                        File directory = new File(standardsDir);
                        if (!directory.exists()) {
                            directory.mkdirs();
                        }

                        File defaultDoc = new File(directory, "默认标准文档.txt");
                        if (!defaultDoc.exists()) {
                            Files.writeString(defaultDoc.toPath(),
                                    "这是系统自动生成的默认标准文档。\n" +
                                            "请管理员上传正式的标准文档以替换此文件。\n" +
                                            "标准文档应当包含政府采购、招投标等相关规定的内容。");
                            System.out.println("已创建默认标准文档: " + defaultDoc.getAbsolutePath());
                        }

                        // 重新读取标准文档目录
                        standardContents = fileService.readStandardDocuments(standardsDir);
                        System.out.println("使用默认文档后找到 " + standardContents.size() + " 个标准文档");
                    } catch (Exception e) {
                        System.out.println("创建默认文档失败: " + e.getMessage());

                        // 最后的兜底方案：直接添加一个内存中的默认文档
                        String defaultContent = "这是系统内存生成的默认标准文档。\n" +
                                "请管理员上传正式的标准文档。\n" +
                                "标准文档应当包含政府采购、招投标等相关规定的内容。";
                        standardContents.add(defaultContent);
                        System.out.println("已添加内存中的默认标准文档");
                    }
                }
            }

            if (standardContents.isEmpty()) {
                // 最后的安全网 - 即使前面所有的尝试都失败了，也要确保有一个默认文档
                String finalDefaultContent = "这是系统最终兜底生成的默认标准文档。\n" +
                        "请管理员上传正式的标准文档。\n" +
                        "标准文档应当包含政府采购、招投标等相关规定的内容。";
                standardContents.add(finalDefaultContent);
                System.out.println("使用最终兜底默认文档");

                // 这里不再返回错误页面，而是使用默认文档继续处理
                System.out.println("使用内置默认文档继续处理比对");
            }

            // 进行文档比对
            String comparisonResult;

            // 根据设置选择比对方式
            if (useAI || useAiComparison) {
                // 使用AI语义比对
                comparisonResult = aiCompareService.semanticCompare(testContent, standardContents);
                model.addAttribute("comparisonMethod", "AI语义比对");
            } else {
                // 使用传统比对
                comparisonResult = compareService.compareWithStandards(testContent, standardContents);
                model.addAttribute("comparisonMethod", "文本比对");
            }

            // 添加结果到模型
            model.addAttribute("fileName", file.getOriginalFilename());
            model.addAttribute("fileSize", formatFileSize(file.getSize()));
            model.addAttribute("standardsCount", standardContents.size());
            model.addAttribute("comparisonResult", comparisonResult);

            // 如果不是员工或管理员，扣除积分
            if (!currentUser.isEmployee() && !currentUser.isAdmin()) {
                userService.deductCredits(currentUser.getUsername(), compareCredits);
                model.addAttribute("creditsDeducted", compareCredits);
                model.addAttribute("remainingCredits", userService.getUserByUsername(currentUser.getUsername()).getCredits());
            }

            return "result";
        } catch (Exception e) {
            model.addAttribute("error", "文件处理错误: " + e.getMessage());
            e.printStackTrace();
            return "compare";
        }
    }

    /**
     * 格式化文件大小
     */
    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }
}