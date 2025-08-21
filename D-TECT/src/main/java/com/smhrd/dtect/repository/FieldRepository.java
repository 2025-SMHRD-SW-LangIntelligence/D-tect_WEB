package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {
    List<Field> findAllByExpert_ExpertIdx(Long expertIdx);

    // 여러 전문가의 분야를 한번에 가져오기
    List<Field> findAllByExpert_ExpertIdxIn(Collection<Long> expertIdxList);

    // 가입 거절시 전문 분야 행 삭제
    void deleteByExpert_ExpertIdx(Long expertIdx);

    // 한 번에 전문가 여러 명의 전문분야 끌어오기
    List<Field> findByExpert_ExpertIdxIn(List<Long> expertIds);
}
