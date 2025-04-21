package com.example.doccompare.controller;

import com.example.doccompare.model.PaymentRecord;
import com.example.doccompare.model.User;
import com.example.doccompare.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class PaymentController {

    private final PaymentService paymentService;

    @Autowired
    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/payment-records")
    public String viewPaymentRecords(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "paymentType", required = false) String paymentType,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "startDate", required = false) String startDate,
            @RequestParam(name = "endDate", required = false) String endDate,
            Model model,
            HttpSession session) {

        // 检查是否管理员
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/login";
        }

        // 获取支付记录列表（这里需要实现分页和筛选逻辑）
        List<PaymentRecord> payments = paymentService.getPaymentRecords(
                username, paymentType, status, startDate, endDate, page);

        // 获取总页数和统计信息
        int totalPages = paymentService.getTotalPages(username, paymentType, status, startDate, endDate);
        double totalAmount = paymentService.getTotalAmount(username, paymentType, status, startDate, endDate);
        int successCount = paymentService.getSuccessCount(username, paymentType, status, startDate, endDate);
        int totalCount = paymentService.getTotalCount(username, paymentType, status, startDate, endDate);

        // 添加到模型
        model.addAttribute("payments", payments);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalAmount", totalAmount);
        model.addAttribute("successCount", successCount);
        model.addAttribute("totalCount", totalCount);

        return "admin/payment-records";
    }

    @PostMapping("/payment-records/update-status")
    public String updatePaymentStatus(
            @RequestParam("id") Long id,
            @RequestParam("status") String status,
            HttpSession session,
            Model model) {

        // 检查是否管理员
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || !currentUser.isAdmin()) {
            return "redirect:/login";
        }

        try {
            paymentService.updatePaymentStatus(id, status);
            model.addAttribute("message", "支付状态已成功更新");
        } catch (Exception e) {
            model.addAttribute("error", "更新支付状态失败: " + e.getMessage());
        }

        return "redirect:/admin/payment-records";
    }
}