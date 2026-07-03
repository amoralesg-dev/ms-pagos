package com.rassini.pagos.scheduler;

import com.rassini.pagos.service.FileLoaderService;

import lombok.extern.log4j.Log4j2;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Log4j2
@Component
public class FileLoaderScheduler {

    private final FileLoaderService fileLoaderService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public FileLoaderScheduler(FileLoaderService fileLoaderService) {
        this.fileLoaderService = fileLoaderService;
    }

    @Scheduled(cron = "${loader.schedule.cron}", zone = "${loader.schedule.zone}")
    public void ejecutarCargaProgramada() {
        if (!running.compareAndSet(false, true)) {
            log.warn("La carga programada ya está en ejecución. Se omite esta ejecución.");
            return;
        }

        try {
            log.info("Inicia carga programada de archivos");
            fileLoaderService.cargarArchivos();
            log.info("Finaliza carga programada de archivos");
        } catch (Exception e) {
            log.error("Error durante la carga programada de archivos", e);
        } finally {
            running.set(false);
        }
    }
}