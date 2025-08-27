package com.smhrd.dtect.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * classification 필드가
 *  - 객체 {label,count} 로 오거나
 *  - 배열 [{label,count}, ...] 로 오더라도
 * List<LabelCount> 로 파싱되도록 처리
 */
public class LabelCountListDeserializer extends JsonDeserializer<List<LabelCount>> {
    @Override
    public List<LabelCount> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken t = p.currentToken();
        ObjectCodec c = p.getCodec();

        List<LabelCount> out = new ArrayList<>();

        if (t == JsonToken.START_OBJECT) {
            LabelCount one = c.readValue(p, LabelCount.class);
            if (one != null) out.add(one);
            return out;
        }
        if (t == JsonToken.START_ARRAY) {
            JsonNode arr = c.readTree(p);
            for (JsonNode n : arr) {
                LabelCount lc = c.treeToValue(n, LabelCount.class);
                if (lc != null) out.add(lc);
            }
            return out;
        }
        // null 또는 알 수 없는 형식
        return out;
    }
}
