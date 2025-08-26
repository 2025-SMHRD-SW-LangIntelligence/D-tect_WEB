package com.smhrd.dtect.service.model;

import com.smhrd.dtect.dto.ModelMessage;
import java.util.List;

public interface ModelGateway {
    List<ModelMessage> predict(byte[] imageBytes, String filename) throws Exception;
}
