package com.rassini.pagos.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.mapper.PagoMapper;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.PagoService;


@Service
public class PagoServiceImpl implements PagoService {

    private final PagosArchivoRepository pagosRepo;
    private final CatalogoTipoPagoRepository catalogoRepo;
    private final EmpresaTipoPagoCache cache;
    private final SupplierRepository supplierRepo;

    public PagoServiceImpl(PagosArchivoRepository pagosRepo,
                           CatalogoTipoPagoRepository catalogoRepo,
                           EmpresaTipoPagoCache cache,
                           SupplierRepository supplierRepo) {
        this.pagosRepo = pagosRepo;
        this.catalogoRepo = catalogoRepo;
        this.cache = cache;
        this.supplierRepo = supplierRepo;
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
    public Page<PagoPendienteDTO> filtrarEnviadosPaginado(String bu, String codigoProveedor, String rfcBeneficiario, Pageable pageable) {
        return pagosRepo.filtrarEnviadosPaginado(bu, codigoProveedor, rfcBeneficiario, pageable)
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

    @Override
    public int enviarPagosPendientes(String bu) {
        List<PagosArchivo> pendientes = pagosRepo.findPendientesPorEnviar(bu);
        if (pendientes == null || pendientes.isEmpty()) {
            return 0;
        }

        // Group by NombreArchivo and TipoPago
        Map<String, Map<String, List<PagosArchivo>>> grouped = pendientes.stream()
            .collect(Collectors.groupingBy(
                p -> p.getNombreArchivo() == null ? "SinArchivo" : p.getNombreArchivo(),
                Collectors.groupingBy(
                    p -> (p.getTipoPago() != null && p.getTipoPago().getDealType() != null) ? p.getTipoPago().getDealType() : "SinTipo"
                )
            ));

        int filesGenerated = 0;
        String fechaActual = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMMyyyy"));

        for (Map.Entry<String, Map<String, List<PagosArchivo>>> entryArchivo : grouped.entrySet()) {
            String nombreArchivo = entryArchivo.getKey();
            for (Map.Entry<String, List<PagosArchivo>> entryTipo : entryArchivo.getValue().entrySet()) {
                String tipoPago = entryTipo.getKey();
                List<PagosArchivo> pagos = entryTipo.getValue();

                String cleanTipoPago = tipoPago.replaceAll("\\s+", "");
                String outputFileName = String.format("%s_%s_%s_%s.txt", cleanTipoPago, bu, fechaActual, nombreArchivo);

                List<String> lineas = new ArrayList<>();
                for (PagosArchivo pago : pagos) {
                    Supplier supplier = null;
                    if (pago.getCodigoProveedor() != null) {
                        supplier = supplierRepo.findFirstByCreditorCode(pago.getCodigoProveedor()).orElse(null);
                    }
                    lineas.add(generarLineaLayout(pago, supplier));

                    pago.setNombreArchivoEnvio(outputFileName);
                }

                try {
                    Path outputPath = Paths.get(outputFileName);
                    Files.write(outputPath, lineas);
                    filesGenerated++;
                } catch (IOException e) {
                    throw new BusinessException("Error al generar archivo txt: " + outputFileName);
                }
            }
        }

        pagosRepo.saveAll(pendientes);
        return filesGenerated;
    }

    private String generarLineaLayout(PagosArchivo pago, Supplier supplier) {
        String[] campos = new String[28];
        campos[0] = nvl(pago.getEmpresa());
        campos[1] = nvl(pago.getCuentaOrdenante());
        campos[2] = nvl(pago.getMonedaOrdenante());
        campos[3] = nvl(pago.getReferencia());
        campos[4] = nvl(pago.getInformacionAdicional());
        campos[5] = nvl(pago.getFechaEnvio());
        campos[6] = nvl(pago.getFechaValor());
        campos[7] = nvl(pago.getMonto());
        campos[8] = nvl(pago.getMoneda());
        campos[9] = nvl(pago.getTipoCambio());
        campos[10] = nvl(pago.getCodigoProveedor());
        campos[11] = nvl(pago.getNombreBeneficiario());
        campos[12] = nvl(pago.getRfcBeneficiario());

        if (supplier != null) {
            campos[13] = nvl(supplier.getAddressStreet1());
            campos[14] = nvl(supplier.getStreetNumber());
            campos[15] = nvl(supplier.getAddressZip());
            campos[16] = nvl(supplier.getCityCode());
            campos[17] = nvl(supplier.getStateCode());
            campos[18] = nvl(supplier.getCountryCode());
        } else {
            campos[13] = campos[14] = campos[15] = campos[16] = campos[17] = campos[18] = "";
        }

        campos[19] = nvl(pago.getCuentaBeneficiario());
        campos[20] = nvl(pago.getMonedaBeneficiario());

        if (supplier != null) {
            boolean isBeneficiaryBankValid =
                java.util.Objects.equals(pago.getCuentaBeneficiario(), supplier.getAccountNumber()) &&
                java.util.Objects.equals(pago.getEmpresa(), supplier.getBusinessUnitCode()) &&
                java.util.Objects.equals(pago.getMonedaBeneficiario(), supplier.getCurrency());

            if (isBeneficiaryBankValid) {
                campos[21] = nvl(supplier.getBeneficiaryBankName());
            } else {
                campos[21] = "";
            }

            String ruteo = supplier.getRoutingCodeBIC();
            if (ruteo == null || ruteo.isEmpty()) {
                ruteo = supplier.getRoutingCodeABA();
            }
            campos[22] = nvl(ruteo);

            campos[23] = nvl(supplier.getBankCountry());
            campos[24] = nvl(supplier.getIntermediaryAccount());

            String intRuteo = supplier.getIntermediaryRoutingCodeBIC();
            if (intRuteo == null || intRuteo.isEmpty()) {
                intRuteo = supplier.getIntermediaryRoutingCodeABA();
            }
            campos[25] = nvl(intRuteo);

            campos[26] = nvl(supplier.getIntermediaryAccountCountry());
        } else {
            campos[21] = campos[22] = campos[23] = campos[24] = campos[25] = campos[26] = "";
        }

        campos[27] = nvl(pago.getNombreArchivo());

        return String.join("|", campos);
    }

    private String nvl(String val) {
        return val == null ? "" : val;
    }
}