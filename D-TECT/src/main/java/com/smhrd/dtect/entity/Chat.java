package com.smhrd.dtect.entity;

import java.sql.Timestamp;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tb_chat")
public class Chat {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_idx")
	private Long chatIdx;
	
	@Column(name = "chat_content")
	private String chatContent;
	
	@Column(name = "chated_at")
	private Timestamp chatedAt;
	
	@Column(name = "sended_file")
	private String sendedFile;
	
	@ManyToOne
    @JoinColumn(name = "matching_idx", nullable = false)
    private Matching matching;

	@Enumerated(EnumType.STRING)
	@Column(name = "sender_type", nullable = false, length = 20)
	private ChatSenderType senderType;
	
	// 날짜 자동 기입 함수
	@PrePersist
	protected void onCreate() {
	    this.chatedAt = new Timestamp(System.currentTimeMillis());
	}

}

