package com.rassini.pagos.service.impl;

import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.service.CatalogoTipoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor

public class CatalogoTipoPagoServiceImpl implements CatalogoTipoPagoService {
 
    private final CatalogoTipoPagoRepository catalogoTipoPagoRepository;

    @Override
    public List<CatalogoTipoPago> obtenerTodos() {
        return catalogoTipoPagoRepository.findAll();
    }
}
