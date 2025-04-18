package com.example.doccompare.controller;

import com.example.doccompare.model.Regulation;
import com.example.doccompare.model.Role;
import com.example.doccompare.model.User;
import com.example.doccompare.service.RegulationService;
import com.example.doccompare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final RegulationService regulationService;

    @Autowired
    public AdminController(UserService userService, RegulationService regulationService) {
        this.userService = userService;
        this.regulationService = regulationService;
    }

    // 检查是否是管理员
    private boolean isAdmin(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        return currentUser != null && currentUser.isAdmin();
    }

    // 获取当前用户
    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("currentUser");
    }

    @GetMapping("/users")
    public String userManagement(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("isSuperAdmin", getCurrentUser(session).isSuperAdmin());
        return "admin/users";
    }

    @PostMapping("/add-credits")
    public String addCredits(@RequestParam String username,
                             @RequestParam int credits,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        userService.addCredits(username, credits);
        redirectAttributes.addFlashAttribute("message", "已成功为用户 " + username + " 添加 " + credits + " 积分");
        return "redirect:/admin/users";
    }

    @PostMapping("/set-role")
    public String setUserRole(@RequestParam String username,
                              @RequestParam Role role,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {

        User admin = getCurrentUser(session);
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/login";
        }

        if (userService.setUserRole(username, role, admin.getUsername())) {
            redirectAttributes.addFlashAttribute("message", "已成功更新用户 " + username + " 的角色");
        } else {
            redirectAttributes.addFlashAttribute("error", "权限不足或用户不存在");
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/toggle-status")
    public String toggleUserStatus(@RequestParam String username,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User admin = getCurrentUser(session);
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/login";
        }

        if (userService.toggleUserStatus(username, admin.getUsername())) {
            redirectAttributes.addFlashAttribute("message", "已成功更新用户 " + username + " 的状态");
        } else {
            redirectAttributes.addFlashAttribute("error", "无法更改超级管理员状态或用户不存在");
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/regulations")
    public String regulationManagement(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        List<Regulation> regulations = regulationService.getAllRegulations();
        // 确保即使没有规章制度，也至少返回一个空列表而不是null
        if (regulations == null) {
            regulations = new ArrayList<>();
        }
        model.addAttribute("regulations", regulations);
        return "admin/regulations"; // 现在应该是 "regulations"，因为文件在 resources/admin/
    }

    @GetMapping("/regulations/add")
    public String addRegulationForm(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        return "admin/regulation-form";
    }

    @PostMapping("/regulations/add")
    public String addRegulation(@RequestParam String title,
                                @RequestParam String content,
                                @RequestParam String category,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        User admin = getCurrentUser(session);
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/login";
        }

        regulationService.addRegulation(title, content, category, admin.getUsername());
        redirectAttributes.addFlashAttribute("message", "规章制度已成功添加");

        return "redirect:/admin/regulations";
    }

    @GetMapping("/regulations/edit/{id}")
    public String editRegulationForm(@PathVariable Long id, HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        Regulation regulation = regulationService.getRegulationById(id);
        if (regulation == null) {
            return "redirect:/admin/regulations";
        }

        model.addAttribute("regulation", regulation);
        return "admin/regulation-form";
    }

    @PostMapping("/regulations/edit/{id}")
    public String updateRegulation(@PathVariable Long id,
                                   @RequestParam String title,
                                   @RequestParam String content,
                                   @RequestParam String category,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        User admin = getCurrentUser(session);
        if (admin == null || !admin.isAdmin()) {
            return "redirect:/login";
        }

        if (regulationService.updateRegulation(id, title, content, category, admin.getUsername())) {
            redirectAttributes.addFlashAttribute("message", "规章制度已成功更新");
        } else {
            redirectAttributes.addFlashAttribute("error", "规章制度不存在");
        }

        return "redirect:/admin/regulations";
    }

    @PostMapping("/regulations/toggle/{id}")
    public String toggleRegulationStatus(@PathVariable Long id,
                                         HttpSession session,
                                         RedirectAttributes redirectAttributes) {

        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        if (regulationService.toggleRegulationStatus(id)) {
            redirectAttributes.addFlashAttribute("message", "规章制度状态已更新");
        } else {
            redirectAttributes.addFlashAttribute("error", "规章制度不存在");
        }

        return "redirect:/admin/regulations";
    }

    @GetMapping("/session-info")
    @ResponseBody
    public String getSessionInfo(HttpSession session) {
        StringBuilder info = new StringBuilder();
        User user = (User) session.getAttribute("currentUser");
        info.append("Session ID: ").append(session.getId()).append("<br>");
        info.append("User: ").append(user != null ? user.getUsername() : "未登录").append("<br>");
        info.append("Is Admin: ").append(user != null && user.isAdmin()).append("<br>");
        return info.toString();
    }

    @GetMapping("/test")
    public String testPage(HttpSession session, Model model) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        model.addAttribute("regulations", regulationService.getAllRegulations());
        return "admin/test";
    }
}