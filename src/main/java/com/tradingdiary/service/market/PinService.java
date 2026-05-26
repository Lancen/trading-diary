package com.tradingdiary.service.market;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.IndustryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class PinService {

    private final IndustryMapper industryMapper;
    private final ConceptMapper conceptMapper;

    public PinService(IndustryMapper industryMapper, ConceptMapper conceptMapper) {
        this.industryMapper = industryMapper;
        this.conceptMapper = conceptMapper;
    }

    @Transactional
    public Map<String, Object> togglePin(String type, String code, boolean pinned) {
        if ("industry".equals(type)) {
            Industry item = industryMapper.selectOne(
                    new LambdaQueryWrapper<Industry>().eq(Industry::getCode, code));
            if (item == null) {
                return Map.of("status", "failed", "error", "行业不存在: " + code);
            }
            if (pinned) {
                Integer maxOrder = getMaxPinOrder(type);
                industryMapper.update(null, new LambdaUpdateWrapper<Industry>()
                        .eq(Industry::getCode, code)
                        .set(Industry::getPinned, true)
                        .set(Industry::getPinOrder, maxOrder != null ? maxOrder + 1 : 1));
            } else {
                industryMapper.update(null, new LambdaUpdateWrapper<Industry>()
                        .eq(Industry::getCode, code)
                        .set(Industry::getPinned, false)
                        .set(Industry::getPinOrder, null));
            }
        } else {
            Concept item = conceptMapper.selectOne(
                    new LambdaQueryWrapper<Concept>().eq(Concept::getCode, code));
            if (item == null) {
                return Map.of("status", "failed", "error", "概念不存在: " + code);
            }
            if (pinned) {
                Integer maxOrder = getMaxPinOrder(type);
                conceptMapper.update(null, new LambdaUpdateWrapper<Concept>()
                        .eq(Concept::getCode, code)
                        .set(Concept::getPinned, true)
                        .set(Concept::getPinOrder, maxOrder != null ? maxOrder + 1 : 1));
            } else {
                conceptMapper.update(null, new LambdaUpdateWrapper<Concept>()
                        .eq(Concept::getCode, code)
                        .set(Concept::getPinned, false)
                        .set(Concept::getPinOrder, null));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("type", type);
        result.put("code", code);
        result.put("pinned", pinned);
        return result;
    }

    @Transactional
    public Map<String, Object> reorderPinned(String type, List<String> codes) {
        if ("industry".equals(type)) {
            for (int i = 0; i < codes.size(); i++) {
                industryMapper.update(null, new LambdaUpdateWrapper<Industry>()
                        .eq(Industry::getCode, codes.get(i))
                        .eq(Industry::getPinned, true)
                        .set(Industry::getPinOrder, i + 1));
            }
        } else {
            for (int i = 0; i < codes.size(); i++) {
                conceptMapper.update(null, new LambdaUpdateWrapper<Concept>()
                        .eq(Concept::getCode, codes.get(i))
                        .eq(Concept::getPinned, true)
                        .set(Concept::getPinOrder, i + 1));
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "success");
        result.put("type", type);
        result.put("reordered", codes.size());
        return result;
    }

    private Integer getMaxPinOrder(String type) {
        if ("industry".equals(type)) {
            List<Industry> pinned = industryMapper.selectList(
                    new LambdaQueryWrapper<Industry>()
                            .eq(Industry::getPinned, true)
                            .orderByDesc(Industry::getPinOrder)
                            .last("LIMIT 1"));
            return pinned.isEmpty() ? null : pinned.get(0).getPinOrder();
        } else {
            List<Concept> pinned = conceptMapper.selectList(
                    new LambdaQueryWrapper<Concept>()
                            .eq(Concept::getPinned, true)
                            .orderByDesc(Concept::getPinOrder)
                            .last("LIMIT 1"));
            return pinned.isEmpty() ? null : pinned.get(0).getPinOrder();
        }
    }
}