package com.rassini.pagos.service.impl;

import com.rassini.pagos.entity.EmpresaTipoPago;
import com.rassini.pagos.repository.EmpresaTipoPagoRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmpresaTipoPagoCacheImpl implements EmpresaTipoPagoCache {

    private final EmpresaTipoPagoRepository repository;

    private Map<String, Set<String>> cache = new HashMap<>();

    public EmpresaTipoPagoCacheImpl(EmpresaTipoPagoRepository repository) {
        this.repository = repository;
    }

    /**
     * Se ejecuta automáticamente al levantar la app
     */
    @PostConstruct
    public void init() {
        recargar();
    }

    /**
     * Carga todas las reglas en memoria
     */
    @Override
    public void recargar() {
        List<EmpresaTipoPago> lista = repository.findAll();

        cache = lista.stream()
                .collect(Collectors.groupingBy(
                        EmpresaTipoPago::getEmpresa,
                        Collectors.mapping(
                                e -> e.getTipoPago().getDealType(),
                                Collectors.toSet()
                        )
                ));

        System.out.println("Cache cargado: " + cache.size() + " empresas");
    }

    /**
     * Valida si una empresa puede usar un tipo de pago
     */
    @Override
    public boolean esValido(String empresa, String dealType) {
        return cache.getOrDefault(empresa, Collections.emptySet())
                .contains(dealType);
    }
}