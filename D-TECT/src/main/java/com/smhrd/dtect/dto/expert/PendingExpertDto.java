package com.smhrd.dtect.dto.expert;

import com.smhrd.dtect.entity.expert.Expert;
import com.smhrd.dtect.entity.expert.ExpertStatus;
import lombok.Getter;

import java.sql.Timestamp;

@Getter
public class PendingExpertDto {
    private Long expertIdx;
    private String name;          // Member.name
    private String officeName;
    private String officeAddress;
    private Timestamp joinedAt;   // Member.joinedAt
    private ExpertStatus status;

    public PendingExpertDto(Long expertIdx, String name, String officeName, String officeAddress,
                            Timestamp joinedAt, ExpertStatus status) {
        this.expertIdx = expertIdx;
        this.name = name;
        this.officeName = officeName;
        this.officeAddress = officeAddress;
        this.joinedAt = joinedAt;
        this.status = status;
    }

    public static PendingExpertDto from(Expert e) {
        return new PendingExpertDto(
                e.getExpertIdx(),
                e.getMember() != null ? e.getMember().getName() : null,
                e.getOfficeName(),
                e.getOfficeAddress(),
                e.getMember() != null ? e.getMember().getJoinedAt() : null,
                e.getExpertStatus()
        );
    }
}