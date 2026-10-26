package com.vynk.cv.controllers;

import com.vynk.cv.services.CvPdfService;
import com.vynk.shared.api.ApiPaths;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.BASE + "/profiles")
public class CvController {
    private final CvPdfService cvPdfService;

    public CvController(CvPdfService cvPdfService) {
        this.cvPdfService = cvPdfService;
    }

    @GetMapping(value = "/{username}/cv", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> download(@PathVariable String username) {
        byte[] pdf = cvPdfService.render(username);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(username + "-cv.pdf").build().toString())
                .contentLength(pdf.length)
                .body(pdf);
    }
}
