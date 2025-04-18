package com.example.doccompare.service;

import com.example.doccompare.model.Regulation;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class RegulationService {
    private Map<Long, Regulation> regulations = new HashMap<>();
    private AtomicLong idGenerator = new AtomicLong(1);

    @PostConstruct
    public void init() {
        // 初始化一些规章制度
        addRegulation(
                "采购合同标准",
                "<h3>采购合同规范要求</h3><p>采购合同应包含以下内容：</p><ol><li>合同双方的基本信息</li><li>采购产品或服务的详细描述</li><li>价格和支付条款</li><li>交付时间和地点</li><li>质量要求和验收标准</li><li>违约责任</li><li>争议解决方式</li></ol>",
                "采购合同",
                "templates/admin"
        );

        addRegulation(
                "招标文件标准",
                "<h3>招标文件规范要求</h3><p>招标文件应包含以下内容：</p><ol><li>招标项目概述</li><li>投标人资格要求</li><li>招标技术规格和要求</li><li>评标方法和标准</li><li>投标文件格式和要求</li><li>合同条款</li><li>投标截止时间和地点</li></ol>",
                "招标文件",
                "templates/admin"
        );

        addRegulation(
                "投标文件规范",
                "<h3>投标文件规范要求</h3><p>投标文件应包含以下内容：</p><ol><li>投标函</li><li>投标人资格证明文件</li><li>技术方案</li><li>商务方案</li><li>投标报价</li><li>服务承诺</li><li>其他相关材料</li></ol><p>投标文件应在<strong>30天内</strong>完成准备和提交。</p>",
                "投标文件",
                "templates/admin"
        );
    }

    public List<Regulation> getAllRegulations() {
        return new ArrayList<>(regulations.values());
    }

    public List<Regulation> getEnabledRegulations() {
        return regulations.values().stream()
                .filter(Regulation::isEnabled)
                .collect(Collectors.toList());
    }

    public Regulation getRegulationById(Long id) {
        return regulations.get(id);
    }

    public Regulation addRegulation(String title, String content, String category, String createdBy) {
        Regulation regulation = new Regulation(idGenerator.getAndIncrement(), title, content, category, createdBy);
        regulations.put(regulation.getId(), regulation);
        return regulation;
    }

    public boolean updateRegulation(Long id, String title, String content, String category, String updatedBy) {
        Regulation regulation = regulations.get(id);
        if (regulation != null) {
            regulation.setTitle(title);
            regulation.setContent(content);
            regulation.setCategory(category);
            regulation.setUpdatedBy(updatedBy);
            regulation.setUpdatedAt(new Date());
            return true;
        }
        return false;
    }

    public boolean toggleRegulationStatus(Long id) {
        Regulation regulation = regulations.get(id);
        if (regulation != null) {
            regulation.setEnabled(!regulation.isEnabled());
            regulation.setUpdatedAt(new Date());
            return true;
        }
        return false;
    }

    public boolean deleteRegulation(Long id) {
        if (regulations.containsKey(id)) {
            regulations.remove(id);
            return true;
        }
        return false;
    }

    public List<Regulation> getRegulationsByCategory(String category) {
        return regulations.values().stream()
                .filter(r -> r.isEnabled() && r.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    public List<Regulation> searchRegulations(String keyword) {
        String searchTerm = keyword.toLowerCase();
        return regulations.values().stream()
                .filter(r -> r.isEnabled() &&
                        (r.getTitle().toLowerCase().contains(searchTerm) ||
                                r.getContent().toLowerCase().contains(searchTerm) ||
                                r.getCategory().toLowerCase().contains(searchTerm)))
                .collect(Collectors.toList());
    }

    // 获取规章制度的数量
    public int getRegulationCount() {
        return regulations.size();
    }

    // 检查规章制度是否存在
    public boolean regulationExists(Long id) {
        return regulations.containsKey(id);
    }

    // 批量启用/禁用规章制度
    public int batchToggleStatus(List<Long> ids, boolean enabled) {
        int count = 0;
        for (Long id : ids) {
            Regulation regulation = regulations.get(id);
            if (regulation != null) {
                regulation.setEnabled(enabled);
                regulation.setUpdatedAt(new Date());
                count++;
            }
        }
        return count;
    }
}