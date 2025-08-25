package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.Field;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FieldRepository extends JpaRepository<Field, Long> {
    List<Field> findAllByExpert_ExpertIdx(Long expertIdx);

    // ✅ 전문가 한 명에 대한 분야 일괄 조회/삭제
    List<Field> findAllByExpert(Expert expert);
    void deleteByExpert(Expert expert);

    // 여러 전문가의 분야를 한번에 가져오기
    List<Field> findAllByExpert_ExpertIdxIn(Collection<Long> expertIdxList);

    void deleteByExpert_ExpertIdx(Long expertIdx);

    List<Field> findByExpert_ExpertIdxIn(List<Long> expertIds);

    List<Field> findAllByExpert_ExpertIdxIn(List<Long> expertIdxs);
}
