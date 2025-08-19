package com.smhrd.dtect.entity;

import java.io.File;
import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_chat")
public class Board {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_idx")
	private Long chatIdx;
	
	@Column(name = "chat_content")
	private String chatContent;
	
	@Column(name = "chated_at")
	private Timestamp chatedAt;
	
	@Column(name = "sended_file")
	private File sendedFile;
	
	@ManyToOne
    @JoinColumn(name = "user_idx", nullable = false)
    private User user;
	
	@ManyToOne
    @JoinColumn(name = "expert_idx", nullable = false)
    private Expert expert;
	
	// 날짜 자동 기입 함수
	@PrePersist
	protected void onCreate() {
	    this.chatedAt = new Timestamp(System.currentTimeMillis());
	}
}
