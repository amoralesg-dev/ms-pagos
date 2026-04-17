package com.rassini.pagos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class UploadResponse {

    private List<String> exitosos;
    private List<String> errores;
}