package com.rassini.pagos.service.impl;

import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.service.FileLoaderService;
import com.rassini.pagos.util.TxtParser;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileLoaderServiceImpl implements FileLoaderService {

    private final PagosArchivoRepository repository;

    @Value("${loader.path}")
    private String rutaCarpeta;

    
    public FileLoaderServiceImpl(PagosArchivoRepository repository) {
        this.repository = repository;
    }

    @Override
    public void cargarArchivos() {

        File folder = new File(rutaCarpeta);

        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("Ruta inválida configurada: " + rutaCarpeta);
        }

        File[] archivos = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (archivos == null || archivos.length == 0) {
            System.out.println("No hay archivos para procesar");
            return;
        }

        for (File archivo : archivos) {
            procesarArchivo(archivo);
        }
    }

    private void procesarArchivo(File archivo) {

        List<PagosArchivo> batch = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String line;

            while ((line = br.readLine()) != null) {

                try {
                    PagosArchivo pago = TxtParser.parseLine(line, archivo.getName());
                    batch.add(pago);

                    if (batch.size() == 500) {
                        repository.saveAll(batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    System.out.println("Error en línea: " + line);
                }
            }

            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }

            System.out.println("Archivo procesado: " + archivo.getName());

        } catch (Exception e) {
            throw new RuntimeException("Error procesando archivo: " + archivo.getName(), e);
        }
    }
}