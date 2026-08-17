package com.rassini.pagos.service.impl;

import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.entity.EmpresaTipoPago;
import com.rassini.pagos.entity.EquivalencesDealType;
import com.rassini.pagos.entity.PagoReferenciaProveedor;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.repository.EmpresaTipoPagoRepository;
import com.rassini.pagos.repository.EquivalencesDealTypeRepository;
import com.rassini.pagos.repository.PagoReferenciaProveedorRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EmpresaTipoPagoCacheImpl implements EmpresaTipoPagoCache {

    private final EmpresaTipoPagoRepository repository;
    private final EquivalencesDealTypeRepository equivalencesDealTypeRepository;
    private final CatalogoTipoPagoRepository catalogoTipoPagoRepository;
    private final PagoReferenciaProveedorRepository pagoReferenciaProveedorRepository;

    private Map<String, Set<String>> cache = new HashMap<>();

    private Map<String, EquivalencesDealType> equivalencesDealTypeMap = new HashMap<>();

    private Map<String, CatalogoTipoPago> catalogoTipoPagoMap = new HashMap<>();

    private Map<String, PagoReferenciaProveedor> referenciaProveedorMap = new HashMap<>();

    public EmpresaTipoPagoCacheImpl(
            EmpresaTipoPagoRepository repository,
            EquivalencesDealTypeRepository equivalencesDealTypeRepository,
            CatalogoTipoPagoRepository catalogoTipoPagoRepository,
            PagoReferenciaProveedorRepository pagoReferenciaProveedorRepository) {

        this.repository = repository;
        this.equivalencesDealTypeRepository = equivalencesDealTypeRepository;
        this.catalogoTipoPagoRepository = catalogoTipoPagoRepository;
        this.pagoReferenciaProveedorRepository = pagoReferenciaProveedorRepository;
    }

    @PostConstruct
    public void init() {
        recargar();
    }

    @Override
    public void recargar() {

        cargarEmpresasTipoPago();
        cargarEquivalenciasDealType();
        cargarCatalogoTipoPago();
        cargarReferenciaProveedor();

        log.info("Cache cargado correctamente");
    }

    private void cargarEmpresasTipoPago() {

        List<EmpresaTipoPago> lista = repository.findAll();

        cache = lista.stream()
                .collect(Collectors.groupingBy(
                        EmpresaTipoPago::getEmpresa,
                        Collectors.mapping(
                                e -> e.getTipoPago().getDealType(),
                                Collectors.toSet()
                        )
                ));

        log.info("Cache EmpresaTipoPago: " + cache.size());
    }

    private void cargarEquivalenciasDealType() {

        equivalencesDealTypeMap = equivalencesDealTypeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        e -> e.getBu() + "-" + e.getCode(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        log.info("Cache EquivalencesDealType: " + equivalencesDealTypeMap.size());
    }

    private void cargarCatalogoTipoPago() {

        catalogoTipoPagoMap = catalogoTipoPagoRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        CatalogoTipoPago::getDealType,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        log.info("Cache CatalogoTipoPago: " + catalogoTipoPagoMap.size());
    }

    private void cargarReferenciaProveedor() {

        referenciaProveedorMap = pagoReferenciaProveedorRepository.findAll()
                .stream()
                .collect(Collectors.toMap(
                        r -> r.getBu() + "-" + r.getProveedor(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        log.info("Cache PagoReferenciaProveedor: " + referenciaProveedorMap.size());
    }

    @Override
    public boolean esValido(String empresa, String dealType) {

        return cache.getOrDefault(
                empresa,
                Collections.emptySet())
                .contains(dealType);
    }

    public Map<String, EquivalencesDealType> getEquivalencesDealTypeMap() {
        return equivalencesDealTypeMap;
    }

    public Map<String, CatalogoTipoPago> getCatalogoTipoPagoMap() {
        return catalogoTipoPagoMap;
    }

    public Map<String, PagoReferenciaProveedor> getReferenciaProveedorMap() {
        return referenciaProveedorMap;
    }
}