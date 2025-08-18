package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadRepository extends JpaRepository<Upload, Long> {

    // 매칭된 사용자/전문가의 업로드 묶음 목록
    List<Upload> findByUserUserIdxAndExpertExpertIdx(Long userIdx, Long expertIdx);

    // 업로드와 그에 속한 파일들을 한 번에 로딩
    @Query("""
           select distinct u
           from Upload u
           left join fetch u.uploadFileList f
           where u.user.userIdx = :userId
             and u.expert.expertIdx = :expertId
           order by u.createdAt desc
           """)
    List<Upload> findWithFiles(Long userId, Long expertId);
}
