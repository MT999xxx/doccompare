package com.example.doccompare.controller;

import com.example.doccompare.model.MembershipPlan;
import com.example.doccompare.model.PaymentRecord;
import com.example.doccompare.model.User;
import com.example.doccompare.service.MembershipService;
import com.example.doccompare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private final MembershipService membershipService;
    private final UserService userService;

    @Autowired
    public PaymentController(MembershipService membershipService, UserService userService) {
        this.membershipService = membershipService;
        this.userService = userService;
    }

    @GetMapping("/plans")
    public String viewPlans(HttpSession session, Model model) {
        System.out.println("访问会员套餐页面");

        User currentUser = (User) session.getAttribute("currentUser");
        System.out.println("当前用户: " + (currentUser != null ? currentUser.getUsername() : "未登录"));

        if (currentUser == null) {
            return "redirect:/login";
        }

        // 员工和管理员不需要购买会员
        if (currentUser.isEmployee() || currentUser.isAdmin()) {
            model.addAttribute("message", "您是公司员工或管理员，可以免费使用所有功能");
            return "payment/free-user";
        }

        try {
            List<MembershipPlan> plans = membershipService.getActivePlans();

            // 添加调试日志
            System.out.println("获取到 " + plans.size() + " 个会员套餐");
            for (MembershipPlan plan : plans) {
                System.out.println("套餐: " + plan.getId() + " - " + plan.getName() + " - ¥" + plan.getPrice());
            }

            model.addAttribute("plans", plans);
            model.addAttribute("user", currentUser);

            return "payment/plans";
        } catch (Exception e) {
            // 添加详细的异常日志
            e.printStackTrace();
            model.addAttribute("error", "获取会员套餐时出错: " + e.getMessage());
            return "error";
        }
    }
    @GetMapping("/history")
    public String viewPaymentHistory(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<PaymentRecord> records = membershipService.getUserPaymentRecords(currentUser.getUsername());
        model.addAttribute("records", records);

        return "payment/history";
    }

    @PostMapping("/buy/{planId}")
    public String buyPlan(@PathVariable Long planId, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 员工和管理员不需要购买
        if (currentUser.isEmployee() || currentUser.isAdmin()) {
            redirectAttributes.addFlashAttribute("message", "您是公司员工或管理员，可以免费使用所有功能");
            return "redirect:/payment/plans";
        }

        MembershipPlan plan = membershipService.getPlanById(planId);
        if (plan == null || !plan.isActive()) {
            redirectAttributes.addFlashAttribute("error", "套餐不存在或已下架");
            return "redirect:/payment/plans";
        }

        PaymentRecord record = membershipService.createPayment(currentUser.getUsername(), planId);
        redirectAttributes.addFlashAttribute("paymentId", record.getId());

        return "redirect:/payment/checkout";
    }

    @GetMapping("/checkout")
    public String checkout(@ModelAttribute("paymentId") Long paymentId, HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        if (paymentId == null) {
            return "redirect:/payment/plans";
        }

        PaymentRecord record = membershipService.getPaymentById(paymentId);
        if (record == null || !record.getUsername().equals(currentUser.getUsername())) {
            return "redirect:/payment/plans";
        }

        model.addAttribute("payment", record);

        return "payment/checkout";
    }

    @PostMapping("/process")
    public String processPayment(@RequestParam Long paymentId, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        PaymentRecord record = membershipService.getPaymentById(paymentId);
        if (record == null || !record.getUsername().equals(currentUser.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "支付记录不存在");
            return "redirect:/payment/plans";
        }

        // 模拟支付成功
        String transactionId = UUID.randomUUID().toString();
        boolean success = membershipService.processPayment(paymentId, transactionId);

        if (success) {
            redirectAttributes.addFlashAttribute("message", "支付成功！");
            return "redirect:/payment/success";
        } else {
            redirectAttributes.addFlashAttribute("error", "支付处理失败");
            return "redirect:/payment/plans";
        }
    }

    @GetMapping("/success")
    public String paymentSuccess(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 刷新用户信息
        User updatedUser = userService.getUserByUsername(currentUser.getUsername());
        session.setAttribute("currentUser", updatedUser);

        model.addAttribute("user", updatedUser);

        return "payment/success";
    }

    @GetMapping("/cancel")
    public String cancelPayment(@RequestParam Long paymentId, HttpSession session, RedirectAttributes redirectAttributes) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        PaymentRecord record = membershipService.getPaymentById(paymentId);
        if (record == null || !record.getUsername().equals(currentUser.getUsername())) {
            redirectAttributes.addFlashAttribute("error", "支付记录不存在");
            return "redirect:/payment/plans";
        }

        membershipService.cancelPayment(paymentId);
        redirectAttributes.addFlashAttribute("message", "支付已取消");

        return "redirect:/payment/plans";
    }

    // 管理员查看所有支付记录
    @GetMapping("/admin/records")
    public String adminViewRecords(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/login";
        }

        List<PaymentRecord> records = membershipService.getAllPaymentRecords();
        model.addAttribute("records", records);

        return "admin/payment-records";
    }
}