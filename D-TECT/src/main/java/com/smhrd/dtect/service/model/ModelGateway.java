package com.smhrd.dtect.service.model;

import com.smhrd.dtect.dto.model.ModelMessage;
import java.util.List;

public interface ModelGateway {

    List<ModelMessage> predict(byte[] imageBytes, String filename, Long analId) throws Exception;
}
