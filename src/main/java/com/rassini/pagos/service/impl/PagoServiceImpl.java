package com.rassini.pagos.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.mapper.PagoMapper;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.PagoService;


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
    public List<PagoPendienteDTO> obtenerSinClasificar(String bu) {

        return pagosRepo.findByEmpresaAndNombreArchivoEnvioIsNull(bu)
                .stream()
                .map(PagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long obtenerTotalPendientes(String bu) {
        return pagosRepo.countByEmpresaAndTipoPagoIsNull(bu);
    }

    /**
     *  Clasificar pago con validación en cache (sin DB extra)
     */
    @Override
    public void clasificarPagos(List<ClasificarPagoItem> items, String bu) {

        List<PagosArchivo> pagosActualizar = new ArrayList<>();

        for (ClasificarPagoItem item : items) {

            PagosArchivo pago = pagosRepo.findById(item.getId())
                    .orElseThrow(() -> new BusinessException("Pago no encontrado: " + item.getId()));

            if (!bu.equals(pago.getEmpresa())) {
                 throw new BusinessException("El pago no pertenece a la unidad de negocio especificada");
            }

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
    public List<PagoPendienteDTO> filtrarPendientes(String bu, String codigoProveedor, String rfcBeneficiario) {

        return pagosRepo.filtrar(bu, codigoProveedor, rfcBeneficiario)
            .stream()
            .map(PagoMapper::toDTO)
            .toList();
    }

    @Override
    public Page<PagoPendienteDTO> filtrarPendientesPaginado(String bu, String codigoProveedor, String rfcBeneficiario, Pageable pageable) {
        return pagosRepo.filtrarPaginado(bu, codigoProveedor, rfcBeneficiario, pageable)
                .map(PagoMapper::toDTO);
    }

    @Override
    public int validarPagosPendientes(String bu) {
        List<PagosArchivo> pendientes = pagosRepo.findPendientesParaValidar(bu);
        if (pendientes == null || pendientes.isEmpty()) {
            return 0;
        }

        String primerNombreArchivo = pendientes.get(0).getNombreArchivo();

        for (PagosArchivo pago : pendientes) {
            String nombreArchivo = pago.getNombreArchivo();
            if (primerNombreArchivo == null) {
                if (nombreArchivo != null) return 0;
            } else if (!primerNombreArchivo.equals(nombreArchivo)) {
                return 0;
            }

            String nombreArchivoEnvio = pago.getNombreArchivoEnvio();
            if (nombreArchivoEnvio != null && !nombreArchivoEnvio.trim().isEmpty()) {
                return 0;
            }

            if (pago.getTipoPago() == null ||
                pago.getTipoPago().getDealType() == null ||
                pago.getTipoPago().getDealType().trim().isEmpty()) {
                return 0;
            }
        }

        return 1;
    }
}