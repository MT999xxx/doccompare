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

@Controller
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private MembershipService membershipService;

    @Autowired
    private UserService userService;

    // 查看会员套餐页面
    @GetMapping("/plans")
    public String viewPlans(Model model, HttpSession session) {
        // 获取当前用户
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // 获取所有会员套餐
        List<MembershipPlan> plans = membershipService.getAllPlans();
        model.addAttribute("plans", plans);

        System.out.println("加载了" + plans.size() + "个会员套餐");

        return "payment/plans";
    }

    // 处理套餐购买请求
    @PostMapping("/buy")
    public String buyPlan(@RequestParam("planId") int planId,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        try {
            // 创建支付记录并直接处理成功
            PaymentRecord paymentRecord = membershipService.createPayment(currentUser.getUsername(), planId);
            boolean success = membershipService.processPayment(paymentRecord.getId(), true);

            if (success) {
                redirectAttributes.addFlashAttribute("message", "购买成功！您的会员权益已激活。");
                // 更新session中的用户信息
                session.setAttribute("currentUser", userService.findByUsername(currentUser.getUsername()));
            } else {
                redirectAttributes.addFlashAttribute("error", "支付处理失败，请稍后重试。");
            }
            return "redirect:/payment/plans";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "处理订单时出错: " + e.getMessage());
            return "redirect:/payment/plans";
        }
    }

    // 查看支付历史
    @GetMapping("/history")
    public String viewPaymentHistory(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        List<PaymentRecord> payments = membershipService.getUserPayments(currentUser.getUsername());
        model.addAttribute("payments", payments);

        return "payment/history";
    }
}