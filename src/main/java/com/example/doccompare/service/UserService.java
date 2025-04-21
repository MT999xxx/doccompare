package com.example.doccompare.service;

import com.example.doccompare.model.Role;
import com.example.doccompare.model.User;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class UserService {
    private Map<String, User> users = new HashMap<>();
    private AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {

        // 添加调试日志
        System.out.println("初始化用户服务...");

        // 确保管理员账户信息正确
        User admin = new User(idGenerator.getAndIncrement(), "admin", "admin123", "admin@example.com", "13800000001", "Company HQ", Role.ADMIN);
        users.put(admin.getUsername(), admin);
        System.out.println("添加管理员: " + admin.getUsername() + ", 密码: " + admin.getPassword());
        // 初始化一些用户
        addUser("superadmin", "admin123", "super@example.com", "13800000000", "Company HQ", Role.SUPER_ADMIN);
        addUser("templates/admin", "admin123", "admin@example.com", "13800000001", "Company HQ", Role.ADMIN);
        addUser("employee", "employee123", "employee@company.com", "13800000002", "Company Branch", Role.EMPLOYEE);

        User vipUser = addUser("vip", "vip123", "vip@example.com", "13800000003", "Client Corp", Role.VIP);
        if (vipUser != null) {
            vipUser.setVipMonths(3); // 设置3个月VIP期限
        }

        addUser("user", "user123", "user@example.com", "13800000004", "Client Corp", Role.REGULAR);
    }

    public User authenticate(String username, String password) {
        User user = users.get(username);

        // 添加详细日志以便调试
        System.out.println("尝试登录用户: " + username);
        System.out.println("用户是否存在: " + (user != null));
        if (user != null) {
            System.out.println("存储的密码: " + user.getPassword());
            System.out.println("输入的密码: " + password);
            System.out.println("密码是否匹配: " + user.getPassword().equals(password));
            System.out.println("用户是否激活: " + user.isActive());
        }

        if (user != null && user.getPassword().equals(password) && user.isActive()) {
            user.setLastLoginTime(LocalDateTime.now());
            return user;
        }
        return null;
    }

    public User register(String username, String password, String email, String phoneNumber, String company) {
        if (users.containsKey(username) || getUserByEmail(email) != null) {
            return null; // 用户名或邮箱已存在
        }

        // 检查是否是公司员工
        Role role = (company != null && !company.trim().isEmpty() && company.trim().equals("Company HQ")) ? Role.EMPLOYEE : Role.REGULAR;
        int initialCredits = (role == Role.EMPLOYEE) ? 0 : 5; // 员工用户送5积分

        User newUser = new User(idGenerator.getAndIncrement(), username, password, email, phoneNumber, company, role);
        newUser.setRegistrationTime(LocalDateTime.now());
        users.put(username, newUser);

        return newUser;
    }

    public User getUserByUsername(String username) {
        return users.get(username);
    }

    public User getUserByEmail(String email) {
        for (User user : users.values()) {
            if (user.getEmail() != null && user.getEmail().equals(email)) {
                return user;
            }
        }
        return null;
    }
    // 根据用户名查找用户 - 添加这个方法
    public User findByUsername(String username) {
        return users.get(username);
    }

    // 更新用户信息 - 添加这个方法
    public boolean updateUser(User user) {
        if (users.containsKey(user.getUsername())) {
            users.put(user.getUsername(), user);
            return true;
        }
        return false;
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(users.values());
    }

    public User addUser(String username, String password, String email, String phoneNumber, String company, Role role) {
        if (users.containsKey(username)) {
            return null;
        }

        User newUser = new User(idGenerator.getAndIncrement(), username, password, email, phoneNumber, company, role);
        newUser.setRegistrationTime(LocalDateTime.now());
        users.put(username, newUser);
        return newUser;
    }

    public boolean deductCredits(String username, int amount) {
        User user = users.get(username);
        if (user != null) {
            return user.deductCredits(amount);
        }
        return false;
    }

    public void addCredits(String username, int amount) {
        User user = users.get(username);
        if (user != null) {
            user.addCredits(amount);
        }
    }

    public boolean hasEnoughCredits(String username, int amount) {
        User user = users.get(username);
        return user != null && user.getCredits() >= amount;
    }

    public boolean setUserRole(String username, Role role, String adminUsername) {
        User user = users.get(username);
        User admin = users.get(adminUsername);

        if (user != null && admin != null && admin.isAdmin()) {
            // 只有超级管理员可以设置管理员角色
            if ((role == Role.ADMIN || role == Role.SUPER_ADMIN) && !admin.isSuperAdmin()) {
                return false;
            }

            // 不允许降级超级管理员
            if (user.isSuperAdmin() && role != Role.SUPER_ADMIN) {
                return false;
            }

            user.setRole(role);
            return true;
        }
        return false;
    }

    public boolean setVipStatus(String username, int months) {
        User user = users.get(username);
        if (user != null) {
            user.setVipMonths(months);
            return true;
        }
        return false;
    }

    public boolean toggleUserStatus(String username, String adminUsername) {
        User user = users.get(username);
        User admin = users.get(adminUsername);

        if (user != null && admin != null && admin.isAdmin()) {
            // 不允许禁用超级管理员
            if (user.isSuperAdmin()) {
                return false;
            }

            user.setActive(!user.isActive());
            return true;
        }
        return false;
    }

    // 检查用户是否存在
    public boolean userExists(String username) {
        return users.containsKey(username);
    }

    // 清除非活动用户（仅测试用）
    public void clearInactiveUsers() {
        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, User> entry : users.entrySet()) {
            if (!entry.getValue().isActive()) {
                toRemove.add(entry.getKey());
            }
        }
        for (String key : toRemove) {
            users.remove(key);
        }
    }

}