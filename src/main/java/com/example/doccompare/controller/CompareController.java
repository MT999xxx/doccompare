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
                // 使用预定义的标准文件目录
                try {
                    File directory = new File(standardsDir);
                    if (!directory.exists()) {
                        directory.mkdirs();
                    }
                    standardContents = fileService.readStandardDocuments(standardsDir);
                } catch (IOException e) {
                    // 如果读取预定义标准文件失败，尝试从资源目录读取
                    standardContents = fileService.readStandardDocumentsFromResources("standards");
                }
            }

            if (standardContents.isEmpty()) {
                model.addAttribute("error", "没有找到标准文档，请上传标准文档");
                return "compare";
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