package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.Chat;
import com.smhrd.dtect.entity.ChatSenderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatDto {
    Long id;
    String content;
    String createdAt;
    String senderType;
    String senderName;
    String file;

    public static ChatDto from(Chat c) {
        String name = "사용자";
        if (c.getSenderType() == ChatSenderType.EXPERT) {
            name = (c.getMatching()!=null &&
                    c.getMatching().getExpert()!=null &&
                    c.getMatching().getExpert().getMember()!=null &&
                    c.getMatching().getExpert().getMember().getName()!=null)
                    ? c.getMatching().getExpert().getMember().getName()
                    : "법률 전문가";
        } else {
            name = (c.getMatching()!=null &&
                    c.getMatching().getUser()!=null &&
                    c.getMatching().getUser().getMember()!=null &&
                    c.getMatching().getUser().getMember().getName()!=null)
                    ? c.getMatching().getUser().getMember().getName()
                    : "사용자";
        }

        return ChatDto.builder()
                .id(c.getChatIdx())
                .content(c.getChatContent())
                .createdAt(c.getChatedAt()!=null ? c.getChatedAt().toString() : null)
                .senderType(c.getSenderType()!=null ? c.getSenderType().name() : "USER")
                .file(c.getSendedFile())
                .senderName(name)
                .build();
    }
}
