package com.smhrd.dtect.dto;

import com.smhrd.dtect.entity.ExpertStatus;
import com.smhrd.dtect.entity.FieldName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PendingExpertDetailDto {

    private Long expertIdx;
    private String name;
    private String officeName;
    private String officeAddress;
    private Timestamp joinedAt;
    private ExpertStatus status;
    private List<FieldName> fields;
    private String certificationFile;
    private boolean hasCertificate;
}
