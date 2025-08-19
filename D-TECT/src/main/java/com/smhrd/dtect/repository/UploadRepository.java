package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadRepository extends JpaRepository<Upload, Long> {

    // 매칭 기준으로 업로드 목록
    List<Upload> findByMatching_MatchingIdx(Long matchingId);

    @Query("""
        select distinct u
        from Upload u
        left join fetch u.uploadFileList f
        where u.matching.matchingIdx = :matchingId
        order by u.uploadIdx asc
    """)
    List<Upload> findWithFiles(@Param("matchingId") Long matchingId);
}
