package com.smhrd.dtect.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Entity
@Data
@Table(name="tb_upload_file")
public class UploadFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_idx")
    private Long fileIdx;

    @Column(name = "file_name")
    private String fileName;

    @ManyToOne
    @JoinColumn(name = "upload_idx", nullable = false)
    private Upload upload;
}
