package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.LawyerCardDto;
import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Field;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.FieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpertCatalogService {

    private final ExpertRepository expertRepository;
    private final FieldRepository fieldRepository;

    public List<LawyerCardDto> list(String q, String skill, String sortKey) {
        FieldName skillEnum = parseSkill(skill);
        Sort sort = toSort(sortKey);

        List<Expert> experts = expertRepository.searchApproved(q, skillEnum, sort);
        if (experts.isEmpty()) return List.of();

        // 분야
        List<Long> ids = experts.stream().map(Expert::getExpertIdx).toList();
        List<Field> all = fieldRepository.findAllByExpert_ExpertIdxIn(ids);
        Map<Long, List<Field>> map = all.stream()
                .collect(Collectors.groupingBy(f -> f.getExpert().getExpertIdx()));

        // DTO
        List<LawyerCardDto> out = new ArrayList<>(experts.size());
        for (Expert e : experts) {
            out.add(LawyerCardDto.from(e, map.getOrDefault(e.getExpertIdx(), Collections.emptyList())));
        }
        return out;
    }

    private FieldName parseSkill(String code) {
        if (code == null || code.isBlank()) return null;
        try { return FieldName.valueOf(code); } catch (Exception ignore) { return null; }
    }

    private Sort toSort(String key) {
        if ("name".equalsIgnoreCase(key)) {
            return Sort.by(Sort.Order.asc("member.name").ignoreCase());
        }
        return Sort.by(Sort.Order.desc("expertIdx")); // 최신 등록순
    }
}
