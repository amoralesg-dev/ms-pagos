package com.rassini.pagos.controller;

import com.rassini.pagos.dto.UploadResponse;
import com.rassini.pagos.service.FileLoaderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/file")
public class LoaderController {

    private final FileLoaderService service;

    @Value("${loader.path}")
    private String rutaCarpeta;

    public LoaderController(FileLoaderService service) {
        this.service = service;
    }

    // Ejecutar carga de archivos (batch)
    @PostMapping("/loader")
    public String cargar() {
        service.cargarArchivos();
        return "Carga completada";
    }

    // Upload de uno o varios archivos
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> upload(@RequestParam("files") MultipartFile[] files) {

        if (files == null || files.length == 0) {
            return ResponseEntity.badRequest()
                    .body(new UploadResponse(List.of(), List.of("No se enviaron archivos")));
        }

        File carpeta = new File(rutaCarpeta);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        List<String> exitosos = new ArrayList<>();
        List<String> errores = new ArrayList<>();

        for (MultipartFile file : files) {

            if (file.isEmpty()) {
                errores.add("Archivo vacío");
                continue;
            }

            try {
                // 🔒 Sanitizar nombre
                String nombreArchivo = new File(file.getOriginalFilename()).getName();

                // 🔒 Validar extensión
                if (!nombreArchivo.endsWith(".txt")) {
                    errores.add(nombreArchivo + " -> extensión no permitida");
                    continue;
                }

                File destino = new File(rutaCarpeta, nombreArchivo);

                // 🔒 Evitar sobrescribir
                if (destino.exists()) {
                    errores.add(nombreArchivo + " -> ya existe");
                    continue;
                }

                file.transferTo(destino);
                exitosos.add(nombreArchivo);

            } catch (IOException e) {
                errores.add(file.getOriginalFilename() + " -> error al guardar");
            }
        }

        return ResponseEntity.ok(new UploadResponse(exitosos, errores));
    }

    // Health check
    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }
}