package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.Chat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ChatDto {
    Long id;
    String content;
    String createdAt;
    String senderType;
    String file;

    public static ChatDto from(Chat c) {
        return new ChatDto(
                c.getChatIdx(),
                c.getChatContent(),
                c.getChatedAt()!=null ? c.getChatedAt().toString() : null,
                c.getSenderType()!=null ? c.getSenderType().name() : "USER",
                c.getSendedFile()
        );
    }
}
