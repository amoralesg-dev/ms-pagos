package com.rassini.pagos.controller;

import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.service.CatalogoTipoPagoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/catalogos/tipo-pago")
@RequiredArgsConstructor
public class CatalogoTipoPagoController {
    
    private final CatalogoTipoPagoService catalogoTipoPagoService;

    @GetMapping
    public List<CatalogoTipoPago> obtenerTodos() {
        return catalogoTipoPagoService.obtenerTodos();
    }

}
