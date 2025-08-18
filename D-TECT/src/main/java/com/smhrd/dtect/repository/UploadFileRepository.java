package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.UploadFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UploadFileRepository extends JpaRepository<UploadFile, Long> {
    // 업로드 묶음(Upload) 하나에 속한 모든 파일
    List<UploadFile> findAllByUpload_UploadIdx(Long uploadIdx);

    // 여러 업로드 묶음의 파일을 한 번에
    List<UploadFile> findAllByUpload_UploadIdxIn(List<Long> uploadIdxList);
}