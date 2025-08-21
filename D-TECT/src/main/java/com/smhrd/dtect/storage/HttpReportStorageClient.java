package com.smhrd.dtect.storage;

import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
public class HttpReportStorageClient implements ReportStorageClient {

    private final RestTemplate rt = new RestTemplate();

    @Override
    public byte[] loadBytes(String reportPath) {
        if (reportPath == null) {
            return null;
        }

        if (reportPath.startsWith("http://") || reportPath.startsWith("https://")) {
            try {
                RequestEntity<Void> req = new RequestEntity<>(HttpMethod.GET, URI.create(reportPath));
                ResponseEntity<byte[]> res = rt.exchange(req, byte[].class);
                if (res.getStatusCode().is2xxSuccessful()) {
                    return res.getBody();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
