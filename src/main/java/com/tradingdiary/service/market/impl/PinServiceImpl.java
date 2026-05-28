package com.tradingdiary.service.market.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.tradingdiary.entity.Concept;
import com.tradingdiary.entity.Industry;
import com.tradingdiary.mapper.ConceptMapper;
import com.tradingdiary.mapper.IndustryMapper;
import com.tradingdiary.service.market.PinService;
import com.tradingdiary.util.BatchSqlRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 板块置顶服务实现，管理行业和概念板块的置顶状态和排序
 */
@Service
public class PinServiceImpl implements PinService {

    private final IndustryMapper industryMapper;
    private final ConceptMapper conceptMapper;
    private final BatchSqlRunner batchSqlRunner;

    public PinServiceImpl(IndustryMapper industryMapper, ConceptMapper conceptMapper, BatchSqlRunner batchSqlRunner) {
        this.industryMapper = industryMapper;
        this.conceptMapper = conceptMapper;
        this.batchSqlRunner = batchSqlRunner;
    }

    @Override
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

    @Override
    @Transactional
    public Map<String, Object> reorderPinned(String type, List<String> codes) {
        if ("industry".equals(type)) {
            List<Industry> existing = industryMapper.selectList(
                    new LambdaQueryWrapper<Industry>()
                            .in(Industry::getCode, codes)
                            .eq(Industry::getPinned, true)
                            .eq(Industry::getIsDeleted, false));

            Map<String, Industry> byCode = existing.stream()
                    .collect(Collectors.toMap(Industry::getCode, i -> i, (a, b) -> a));

            List<Industry> toUpdate = new ArrayList<>();
            for (int i = 0; i < codes.size(); i++) {
                Industry item = byCode.get(codes.get(i));
                if (item != null) {
                    item.setPinOrder(i + 1);
                    toUpdate.add(item);
                }
            }

            if (!toUpdate.isEmpty()) {
                batchSqlRunner.batchUpdate(toUpdate);
            }
        } else {
            List<Concept> existing = conceptMapper.selectList(
                    new LambdaQueryWrapper<Concept>()
                            .in(Concept::getCode, codes)
                            .eq(Concept::getPinned, true)
                            .eq(Concept::getIsDeleted, false));

            Map<String, Concept> byCode = existing.stream()
                    .collect(Collectors.toMap(Concept::getCode, c -> c, (a, b) -> a));

            List<Concept> toUpdate = new ArrayList<>();
            for (int i = 0; i < codes.size(); i++) {
                Concept item = byCode.get(codes.get(i));
                if (item != null) {
                    item.setPinOrder(i + 1);
                    toUpdate.add(item);
                }
            }

            if (!toUpdate.isEmpty()) {
                batchSqlRunner.batchUpdate(toUpdate);
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