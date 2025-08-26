package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.ChatSenderType;
import com.smhrd.dtect.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UploadRepository extends JpaRepository<Upload, Long> {

    // 매칭 기준으로 업로드 목록
    List<Upload> findByMatching_MatchingIdx(Long matchingId);

    // FileService.findUploadsByMatching 에서 사용
    @Query("select distinct u from Upload u left join fetch u.uploadFileList where u.matching.matchingIdx = :matchingId")
    List<Upload> findWithFiles(@Param("matchingId") Long matchingId);
    
    // 사용자 멤버 기준 업로드 전부 조회
    List<Upload> findAllByMatching_User_Member_MemIdx(Long memIdx);

    // 전문가 멤버 기준 업로드 전부 조회
    List<Upload> findAllByMatching_Expert_Member_MemIdx(Long memIdx);

    // (선택) 업로더 타입까지 좁히고 싶을 때
    List<Upload> findAllByMatching_User_Member_MemIdxAndUploaderType(Long memIdx, ChatSenderType type);
    List<Upload> findAllByMatching_Expert_Member_MemIdxAndUploaderType(Long memIdx, ChatSenderType type);
    
    // 필요 시 벌크 삭제
    @Modifying @Transactional
    @Query("delete from Upload u where u.matching.user.member.memIdx = :memIdx")
    void deleteByUserMemIdx(@Param("memIdx") Long memIdx);

    @Modifying @Transactional
    @Query("delete from Upload u where u.matching.expert.member.memIdx = :memIdx")
    void deleteByExpertMemIdx(@Param("memIdx") Long memIdx);
    

}
