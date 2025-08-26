package com.smhrd.dtect.service.model;

import com.smhrd.dtect.dto.ModelResultDto;
import java.util.List;

public interface ModelGateway {
    List<ModelResultDto> predict(byte[] imageBytes, String filename) throws Exception;
}
