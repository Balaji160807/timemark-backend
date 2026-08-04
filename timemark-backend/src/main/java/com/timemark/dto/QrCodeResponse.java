package com.timemark.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QrCodeResponse {
    private String token;
    private String imageBase64; // data:image/png;base64,... — ready to drop into an <img src>
}
