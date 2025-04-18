package com.example.doccompare.controller;

import com.example.doccompare.model.Role;
import com.example.doccompare.model.User;
import com.example.doccompare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;

@Controller
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        // 如果已经登录，直接跳转到首页
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {

        System.out.println("登录请求: 用户名=" + username + ", 密码=" + password);

        User user = userService.authenticate(username, password);

        if (user != null) {
            // 登录成功，设置会话
            session.setAttribute("currentUser", user);
            System.out.println("登录成功: " + username + ", 角色=" + user.getRole());
            return "redirect:/";
        } else {
            // 登录失败
            System.out.println("登录失败: " + username);
            redirectAttributes.addFlashAttribute("error", "用户名或密码错误");
            return "redirect:/login";
        }
    }

    @GetMapping("/register")
    public String registerForm(HttpSession session) {
        // 如果已经登录，直接跳转到首页
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/";
        }
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String password,
                           @RequestParam String email,
                           @RequestParam String phone,
                           @RequestParam(required = false) String company,
                           @RequestParam(required = false, defaultValue = "false") boolean isEmployee,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {

        // 检查用户名是否已存在
        if (userService.userExists(username)) {
            redirectAttributes.addFlashAttribute("error", "用户名已存在");
            return "redirect:/register";
        }

        // 注册用户
        User newUser = userService.register(username, password, email, phone, isEmployee ? "Company HQ" : company);

        if (newUser != null) {
            // 注册成功，设置会话并跳转到首页
            session.setAttribute("currentUser", newUser);
            redirectAttributes.addFlashAttribute("message", "注册成功");
            return "redirect:/";
        } else {
            // 注册失败
            redirectAttributes.addFlashAttribute("error", "注册失败，请稍后再试");
            return "redirect:/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 清除会话
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 刷新用户信息
        User updatedUser = userService.getUserByUsername(currentUser.getUsername());
        if (updatedUser != null) {
            session.setAttribute("currentUser", updatedUser);
            model.addAttribute("user", updatedUser);
        } else {
            model.addAttribute("user", currentUser);
        }

        return "profile";
    }
}