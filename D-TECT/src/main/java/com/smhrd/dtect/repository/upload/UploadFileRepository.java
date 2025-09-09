package com.smhrd.dtect.repository.upload;

import com.smhrd.dtect.entity.file.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {
    // 업로드 묶음(Upload) 하나에 속한 모든 파일
    List<UploadFile> findAllByUpload_UploadIdx(Long uploadIdx);

    // 여러 업로드 묶음의 파일을 한 번에
    List<UploadFile> findAllByUpload_UploadIdxIn(List<Long> uploadIdxList);

    List<UploadFile> findByUpload_Matching_MatchingIdx(Long matchingId);
    
 // 조회형 (필요시)
    List<UploadFile> findAllByUpload_Matching_User_Member_MemIdx(Long memIdx);
    List<UploadFile> findAllByUpload_Matching_Expert_Member_MemIdx(Long memIdx);

    // 벌크 삭제형 (DB가 자식 정리를 맡을 수 있거나, 먼저 자식 지우고 싶을 때)
    @Modifying @Transactional
    @Query("delete from UploadFile f where f.upload.matching.user.member.memIdx = :memIdx")
    void deleteByUserMemIdx(@Param("memIdx") Long memIdx);

    @Modifying @Transactional
    @Query("delete from UploadFile f where f.upload.matching.expert.member.memIdx = :memIdx")
    void deleteByExpertMemIdx(@Param("memIdx") Long memIdx);
}