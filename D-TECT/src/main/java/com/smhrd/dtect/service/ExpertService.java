package com.smhrd.dtect.service;

import com.smhrd.dtect.dto.ExpertProfileDto;
import com.smhrd.dtect.dto.OptionDto;
import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Field;
import com.smhrd.dtect.entity.FieldName;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.FieldRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpertService {

    private final ExpertRepository expertRepository;
    private final FieldRepository fieldRepository;

    @Transactional(readOnly = true)
    public ExpertProfileDto getProfile(Long expertId) {
        Expert e = expertRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("전문가 없음: " + expertId));

        String name  = (e.getMember() != null) ? e.getMember().getName()  : null;
        String email = (e.getMember() != null) ? e.getMember().getEmail() : null;

        List<Field> fields = fieldRepository.findAllByExpert_ExpertIdx(expertId);
        List<String> codes = fields.stream()
                .map(f -> f.getFieldName().name())
                .toList();

        return new ExpertProfileDto(
                e.getExpertIdx(),
                name,
                email,
                e.getOfficeName(),
                e.getOfficeAddress(),
                codes
        );
    }

    @Transactional(readOnly = true)
    public List<OptionDto> getAllSpecialtyOptions() {
        return Arrays.stream(FieldName.values())
                .map(fn -> new OptionDto(fn.name(), toKo(fn)))
                .toList();
    }

    private static String toKo(FieldName n) {
        return switch (n) {
            case VIOLENCE   -> "폭력";
            case DEFAMATION -> "명예훼손";
            case STALKING   -> "스토킹";
            case SEXUAL     -> "성희롱·성폭력";
            case LEAK       -> "정보유출";
            case BULLYING   -> "따돌림/집단괴롭힘";
            case CHANTAGE   -> "협박";
            case EXTORTION  -> "갈취";
        };
    }

    public Long findExpertIdByMemIdx(Long memIdx) {
        return expertRepository.findByMember_MemIdx(memIdx)
                .map(e -> e.getExpertIdx())
                .orElseThrow(() -> new IllegalStateException("전문가 레코드를 찾을 수 없습니다."));
    }
}
