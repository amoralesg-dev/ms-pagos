package com.rassini.pagos.service.impl;

import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.PagoService;
import org.springframework.stereotype.Service;
import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.exception.*;
import com.rassini.pagos.mapper.PagoMapper;

import java.util.ArrayList;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PagoServiceImpl implements PagoService {

    private final PagosArchivoRepository pagosRepo;
    private final CatalogoTipoPagoRepository catalogoRepo;
    private final EmpresaTipoPagoCache cache;

    public PagoServiceImpl(PagosArchivoRepository pagosRepo,
                           CatalogoTipoPagoRepository catalogoRepo,
                           EmpresaTipoPagoCache cache) {
        this.pagosRepo = pagosRepo;
        this.catalogoRepo = catalogoRepo;
        this.cache = cache;
    }

    /**
     *  Obtener pagos sin clasificar
     */
    @Override
    public List<PagoPendienteDTO> obtenerSinClasificar() {

        return pagosRepo.findByTipoPagoIsNull()
                .stream()
                .map(PagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     *  Clasificar pago con validación en cache (sin DB extra)
     */
    @Override
    public void clasificarPagos(List<ClasificarPagoItem> items) {

        List<PagosArchivo> pagosActualizar = new ArrayList<>();

        for (ClasificarPagoItem item : items) {

            PagosArchivo pago = pagosRepo.findById(item.getId())
                    .orElseThrow(() -> new BusinessException("Pago no encontrado: " + item.getId()));

            CatalogoTipoPago tipo = catalogoRepo.findByDealType(item.getDealType())
                    .orElseThrow(() -> new BusinessException("Tipo inválido: " + item.getDealType()));

            if (!cache.esValido(pago.getEmpresa(), item.getDealType())) {
                throw new BusinessException(
                        "Tipo '" + item.getDealType() +
                        "' no permitido para empresa " + pago.getEmpresa() +
                        " (pago id: " + item.getId() + ")"
                );
            }

            pago.setTipoPago(tipo);
            pagosActualizar.add(pago);
        }

        pagosRepo.saveAll(pagosActualizar);
    }

    @Override
    public List<PagoPendienteDTO> filtrarPendientes(String codigoProveedor, String rfcBeneficiario) {

        System.out.println("codigoProveedor"+codigoProveedor);
        System.out.println("rfcBeneficiario"+rfcBeneficiario);
        return pagosRepo.filtrar(codigoProveedor, rfcBeneficiario)
            .stream()
            .map(PagoMapper::toDTO)
            .toList();
    }
}