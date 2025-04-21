package com.example.doccompare.service;

import com.example.doccompare.model.MembershipPlan;
import com.example.doccompare.model.PaymentRecord;
import com.example.doccompare.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MembershipService {

    private final Map<Integer, MembershipPlan> plans = new ConcurrentHashMap<>();
    private final Map<String, PaymentRecord> payments = new ConcurrentHashMap<>();
    private int nextPaymentId = 1;
    private int nextPlanId = 1;

    @Autowired
    private UserService userService;

    @PostConstruct
    public void init() {
        // 初始化会员套餐
        addPlan(new MembershipPlan(nextPlanId++, "月度会员", 29.9, 1, 30, "基础会员套餐"));
        addPlan(new MembershipPlan(nextPlanId++, "季度会员", 79.9, 3, 100, "标准会员套餐"));
        addPlan(new MembershipPlan(nextPlanId++, "年度会员", 299.9, 12, 500, "高级会员套餐"));

        // 初始化点数充值套餐
        addPlan(new MembershipPlan(nextPlanId++, "基础积分包", 49.9, 0, 100, "100积分充值"));
        addPlan(new MembershipPlan(nextPlanId++, "标准积分包", 129.9, 0, 300, "300积分充值"));
        addPlan(new MembershipPlan(nextPlanId++, "豪华积分包", 399.9, 0, 1000, "1000积分充值"));

        System.out.println("已初始化 " + plans.size() + " 个会员套餐");
    }

    // 添加会员套餐
    public void addPlan(MembershipPlan plan) {
        plans.put(plan.getId(), plan);
    }

    // 获取所有会员套餐
    public List<MembershipPlan> getAllPlans() {
        return new ArrayList<>(plans.values());
    }

    // 根据ID获取套餐
    public MembershipPlan getPlanById(int id) {
        return plans.get(id);
    }

    // 创建支付记录
    public PaymentRecord createPayment(String username, int planId) {
        MembershipPlan plan = getPlanById(planId);
        if (plan == null) {
            throw new IllegalArgumentException("套餐不存在");
        }

        PaymentRecord payment = new PaymentRecord();
        payment.setId(nextPaymentId++);
        payment.setUsername(username);
        payment.setAmount(plan.getPrice());
        payment.setPaymentTime(new Date());
        payment.setStatus("PENDING");

        if (plan.getDurationMonths() > 0) {
            payment.setPaymentType("MEMBERSHIP");
            payment.setDescription(plan.getName() + " - " + plan.getDurationMonths() + "个月");
        } else {
            payment.setPaymentType("CREDIT");
            payment.setDescription("积分充值 - " + plan.getCredits() + "积分");
        }

        payments.put(String.valueOf(payment.getId()), payment);
        return payment;
    }
    // 获取所有支付记录
    public List<PaymentRecord> getAllPayments() {
        return new ArrayList<>(payments.values());
    }

    // 处理支付结果
    public boolean processPayment(int paymentId, boolean success) {
        PaymentRecord payment = payments.get(String.valueOf(paymentId));
        if (payment == null) {
            return false;
        }

        if (success) {
            payment.setStatus("SUCCESS");
            payment.setTransactionId("TX" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

            // 更新用户会员状态和积分
            User user = userService.findByUsername(payment.getUsername());
            if (user != null) {
                MembershipPlan plan = null;
                for (MembershipPlan p : getAllPlans()) {
                    if (payment.getDescription().contains(p.getName())) {
                        plan = p;
                        break;
                    }
                }

                if (plan != null) {
                    // 更新用户积分
                    user.addCredits(plan.getCredits());

                    // 如果是会员套餐，更新会员状态
                    if (plan.getDurationMonths() > 0) {
                        Date now = new Date();
                        Date expiry = user.getVipExpiry();
                        if (expiry == null || expiry.before(now)) {
                            // 新会员或已过期，从当前时间计算
                            expiry = now;
                        }

                        // 计算新的到期时间
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(expiry);
                        calendar.add(Calendar.MONTH, plan.getDurationMonths());
                        user.setVipExpiry(calendar.getTime());
                        user.setVip(true);
                    }

                    userService.updateUser(user);
                }
            }
            return true;
        } else {
            payment.setStatus("FAILED");
            return false;
        }
    }

    // 获取用户的支付记录
    public List<PaymentRecord> getUserPayments(String username) {
        return payments.values().stream()
                .filter(p -> p.getUsername().equals(username))
                .collect(Collectors.toList());
    }
}