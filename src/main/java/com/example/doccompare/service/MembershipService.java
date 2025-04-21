// MembershipService.java
package com.example.doccompare.service;

import com.example.doccompare.model.MembershipPlan;
import com.example.doccompare.model.PaymentRecord;
import com.example.doccompare.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class MembershipService {

    private final UserService userService;
    private Map<Long, MembershipPlan> plans = new ConcurrentHashMap<>();
    private Map<Long, PaymentRecord> payments = new ConcurrentHashMap<>();
    private AtomicLong planIdGenerator = new AtomicLong(1);
    private AtomicLong paymentIdGenerator = new AtomicLong(1);

    @Autowired
    public MembershipService(UserService userService) {
        this.userService = userService;
    }

    @PostConstruct
    public void init() {
        // 初始化会员套餐
        addPlan("月度会员", 29.9, 1, 30);
        addPlan("季度会员", 79.9, 3, 100);
        addPlan("年度会员", 299.9, 12, 500);

        // 初始化积分充值套餐
        addPlan("基础积分包", 49.9, 0, 100);
        addPlan("标准积分包", 129.9, 0, 300);
        addPlan("豪华积分包", 399.9, 0, 1000);

        System.out.println("已初始化 " + plans.size() + " 个会员套餐");
    }

    public MembershipPlan addPlan(String name, double price, int durationMonths, int credits) {
        Long id = planIdGenerator.getAndIncrement();
        MembershipPlan plan = new MembershipPlan(id, name, price, durationMonths, credits);
        plans.put(id, plan);
        return plan;
    }

    public List<MembershipPlan> getActivePlans() {
        return plans.values().stream()
                .filter(MembershipPlan::isActive)
                .collect(Collectors.toList());
    }

    public MembershipPlan getPlanById(Long id) {
        return plans.get(id);
    }

    public PaymentRecord createPayment(String username, Long planId) {
        MembershipPlan plan = plans.get(planId);
        if (plan == null) {
            return null;
        }

        Long id = paymentIdGenerator.getAndIncrement();
        PaymentRecord payment = new PaymentRecord();
        payment.setId(id);
        payment.setUsername(username);
        payment.setAmount(plan.getPrice());
        payment.setDescription(plan.getName());
//        payment.setPlanId(planId);
        payment.setPaymentTime(new Date());
        payment.setStatus("待支付");

        payments.put(id, payment);
        return payment;
    }

    public PaymentRecord getPaymentById(Long id) {
        return payments.get(id);
    }

    public List<PaymentRecord> getUserPaymentRecords(String username) {
        return payments.values().stream()
                .filter(p -> p.getUsername().equals(username))
                .sorted(Comparator.comparing(PaymentRecord::getPaymentTime).reversed())
                .collect(Collectors.toList());
    }

    public List<PaymentRecord> getAllPaymentRecords() {
        return new ArrayList<>(payments.values());
    }

    public boolean processPayment(Long paymentId, String transactionId) {
        PaymentRecord payment = payments.get(paymentId);
        if (payment == null || !"待支付".equals(payment.getStatus())) {
            return false;
        }

        payment.setTransactionId(transactionId);
        payment.setStatus("已支付");

        // 更新用户信息
        MembershipPlan plan = plans.get(payment.getPlanId());
        if (plan != null) {
            User user = userService.getUserByUsername(payment.getUsername());
            if (user != null) {
                // 添加积分
                user.addCredits(plan.getCredits());

                // 如果是会员套餐（有时长），更新VIP状态
                if (plan.getDurationMonths() > 0) {
                    user.setVipMonths(plan.getDurationMonths());
                }

                return true;
            }
        }

        return false;
    }

    public boolean cancelPayment(Long paymentId) {
        PaymentRecord payment = payments.get(paymentId);
        if (payment == null || !"待支付".equals(payment.getStatus())) {
            return false;
        }

        payment.setStatus("已取消");
        return true;
    }
}