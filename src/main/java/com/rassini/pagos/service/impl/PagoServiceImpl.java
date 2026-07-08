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
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
import com.rassini.pagos.util.BuUtils;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class PagoServiceImpl implements PagoService {

    private final PagosArchivoRepository pagosRepo;
    private final CatalogoTipoPagoRepository catalogoRepo;
    private final EmpresaTipoPagoCache cache;
    private final SupplierRepository supplierRepo;

    @Value("${loader.output.path}")
    private String outputPathBase;

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
     * Obtener pagos sin clasificar
     */
    @Override
    public List<PagoPendienteDTO> obtenerSinClasificar(String bu) {

        return pagosRepo.findByEmpresaAndEstatus(bu, "PENDIENTE")
                .stream()
                .map(PagoMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public long obtenerTotalPendientes(String bu) {
        return pagosRepo.countByEmpresaAndTipoPagoIsNull(bu);
    }

    /**
     * Clasificar pago con validación en cache (sin DB extra)
     */
    @Override
    public void clasificarPagos(List<ClasificarPagoItem> items, String bu) {

        List<PagosArchivo> pagosActualizar = new ArrayList<>();

        for (ClasificarPagoItem item : items) {

            PagosArchivo pago = pagosRepo.findById(item.getId())
                    .orElseThrow(() -> new BusinessException("Pago no encontrado: " + item.getId()));

            if (!BuUtils.isAll(bu)
            && !BuUtils.splitBus(bu).contains(pago.getEmpresa())) {

                throw new BusinessException(
                    "El pago no pertenece a la unidad de negocio especificada");
            }

            CatalogoTipoPago tipo = catalogoRepo.findByDealType(item.getDealType())
                    .orElseThrow(() -> new BusinessException("Tipo inválido: " + item.getDealType()));

            if (!cache.esValido(pago.getEmpresa(), item.getDealType())) {
                throw new BusinessException(
                        "Tipo '" + item.getDealType() +
                                "' no permitido para empresa " + pago.getEmpresa() +
                                " (pago id: " + item.getId() + ")");
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
    public Page<PagoPendienteDTO> filtrarPendientesPaginado(
            String bu,
            String codigoProveedor,
            String rfcBeneficiario,
            String tipoPago,
            String estatus,
            Pageable pageable) {

        Page<PagosArchivo> page;

        if (BuUtils.isAll(bu)) {

            page = pagosRepo.filtrarPaginadoAll(
                    codigoProveedor,
                    rfcBeneficiario,
                    tipoPago,
                    estatus,
                    pageable);

        } else {

            page = pagosRepo.filtrarPaginadoMultiBu(
                    BuUtils.splitBus(bu),
                    codigoProveedor,
                    rfcBeneficiario,
                    tipoPago,
                    estatus,
                    pageable);

        }

        return page.map(PagoMapper::toDTO);
    }

    @Override
    public Page<PagoPendienteDTO> filtrarEnviadosPaginado(
            String bu,
            String codigoProveedor,
            String rfcBeneficiario,
            Pageable pageable) {

        Page<PagosArchivo> page;

        if (BuUtils.isAll(bu)) {

            page = pagosRepo.filtrarEnviadosPaginadoAll(
                    codigoProveedor,
                    rfcBeneficiario,
                    pageable);

        } else {

            page = pagosRepo.filtrarEnviadosPaginadoMultiBu(
                    BuUtils.splitBus(bu),
                    codigoProveedor,
                    rfcBeneficiario,
                    pageable);

        }

        return page.map(PagoMapper::toDTO);
    }

    

    @Override
    public Page<PagoPendienteDTO> filtrarErroresPaginado(
            String bu,
            String search,
            Pageable pageable) {

        Page<PagosArchivo> page;

        if (BuUtils.isAll(bu)) {

            page = pagosRepo.filtrarErroresPaginadoAll(
                    search,
                    pageable);

        } else {

            page = pagosRepo.filtrarErroresPaginadoMultiBu(
                    BuUtils.splitBus(bu),
                    search,
                    pageable);

        }

        return page.map(PagoMapper::toDTO);
    }

    @Override
    public int validarPagosPendientes(String bu) {

        List<PagosArchivo> pendientes;

        if (BuUtils.isAll(bu)) {

            pendientes = pagosRepo.findPendientesParaValidarAll();

        } else {

            pendientes = pagosRepo.findPendientesParaValidarMultiBu(
                    BuUtils.splitBus(bu));

        }

        if (pendientes == null || pendientes.isEmpty()) {
            return 0;
        }

        String primerNombreArchivo =
                pendientes.get(0).getNombreArchivo();

        for (PagosArchivo pago : pendientes) {

            String nombreArchivo =
                    pago.getNombreArchivo();

            if (primerNombreArchivo == null) {

                if (nombreArchivo != null) {
                    return 0;
                }

            } else if (!primerNombreArchivo.equals(nombreArchivo)) {

                return 0;

            }

            if (pago.getNombreArchivoEnvio() == null) {
                return 1;
            }

            String nombreArchivoEnvio =
                    pago.getNombreArchivoEnvio();

            if (nombreArchivoEnvio != null &&
                !nombreArchivoEnvio.trim().isEmpty()) {

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

        List<PagosArchivo> pendientes;

        if (BuUtils.isAll(bu)) {

            pendientes = pagosRepo.findPendientesPorEnviarAll();

        } else {

            pendientes = pagosRepo.findPendientesPorEnviarMultiBu(
                    BuUtils.splitBus(bu));

        }

        if (pendientes == null || pendientes.isEmpty()) {
            return 0;
        }

        Map<String, Map<String, List<PagosArchivo>>> grouped = pendientes.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getNombreArchivo() == null ? "SinArchivo" : p.getNombreArchivo(),
                        Collectors.groupingBy(
                                p -> (p.getTipoPago() != null && p.getTipoPago().getDealType() != null)
                                        ? p.getTipoPago().getDealType()
                                        : "SinTipo")));

        int filesGenerated = 0;

        for (Map.Entry<String, Map<String, List<PagosArchivo>>> entryArchivo : grouped.entrySet()) {
            String nombreArchivo = entryArchivo.getKey();

            for (Map.Entry<String, List<PagosArchivo>> entryTipo : entryArchivo.getValue().entrySet()) {
                String tipoPago = entryTipo.getKey();
                List<PagosArchivo> pagos = entryTipo.getValue();

                String cleanTipoPago = tipoPago.replaceAll("\\s+", "");
                String baseNombreArchivo = nombreArchivo;

                if (baseNombreArchivo != null && baseNombreArchivo.toLowerCase().endsWith(".txt")) {
                    baseNombreArchivo = baseNombreArchivo.substring(0, baseNombreArchivo.length() - 4);
                }

                String nombreArchivoOutput = cleanTipoPago+baseNombreArchivo;
                if(nombreArchivoOutput.length() > 31) {
                    nombreArchivoOutput = nombreArchivoOutput.substring(0, 31);
                }
                String outputFileName = String.format("%s.txt", nombreArchivoOutput);

                List<String> lineas = new ArrayList<>();

                for (PagosArchivo pago : pagos) {
                    Supplier supplier = null;

                    if (pago.getCodigoProveedor() != null) {
                        supplier = supplierRepo
                                .findFirstByErpIdQadAndBusinessUnitCode(pago.getCodigoProveedor(), pago.getEmpresa())
                                .orElse(null);
                    }

                    if (pago.getDuplicado() == null || pago.getDuplicado().isEmpty()
                            || pago.getDuplicado().equals("A")) {
                        lineas.add(generarLineaLayout(pago, supplier, outputFileName));
                        pago.setNombreArchivoEnvio(outputFileName);
                        pago.setEstatus("ENVIADO");
                    } else {
                        pago.setNombreArchivoEnvio(outputFileName);
                        pago.setEstatus("ENVIADO");
                    }
                }

                try {
                    Path outputDir = Paths.get(outputPathBase);

                    if (!Files.exists(outputDir)) {
                        Files.createDirectories(outputDir);
                    }

                    Path outputPath = outputDir.resolve(outputFileName);

                    Files.write(outputPath, lineas);

                    log.info("Archivo generado en ruta: {}", outputPath.toAbsolutePath());

                    filesGenerated++;
                } catch (IOException e) {
                    throw new BusinessException("Error al generar archivo txt: " + outputFileName);
                }
            }
        }

        pagosRepo.saveAll(pendientes);
        return filesGenerated;
    }

    private String generarLineaLayout(PagosArchivo pago,
            Supplier supplier,
            String outputFileName) {

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

        if (supplier == null ||
                supplier.getStreetName() == null || supplier.getStreetName().trim().isEmpty() ||
                supplier.getStreetNumber() == null || supplier.getStreetNumber().trim().isEmpty() ||
                supplier.getZipCode() == null || supplier.getZipCode().trim().isEmpty() ||
                supplier.getCityCode() == null || supplier.getCityCode().trim().isEmpty() ||
                supplier.getStateCode() == null || supplier.getStateCode().trim().isEmpty() ||
                supplier.getCountryCode() == null || supplier.getCountryCode().trim().isEmpty()) {

            throw new BusinessException(
                    "El proveedor " + pago.getCodigoProveedor() +
                            " no cuenta con la información completa para generar el layout");
        }

        campos[13] = nvl(supplier.getStreetName());
        campos[14] = nvl(supplier.getStreetNumber());
        campos[15] = nvl(supplier.getZipCode());
        campos[16] = nvl(supplier.getCityCode());
        campos[17] = nvl(supplier.getStateCode());
        campos[18] = nvl(supplier.getCountryCode());

        campos[19] = nvl(pago.getCuentaBeneficiario());
        campos[20] = nvl(pago.getMonedaBeneficiario());

        boolean isBeneficiaryBankValid = Objects.equals(pago.getCuentaBeneficiario(), supplier.getAccountNumber()) &&
                Objects.equals(pago.getEmpresa(), supplier.getBusinessUnitCode()) &&
                Objects.equals(pago.getMonedaBeneficiario(), supplier.getSupplierCurrency());

        campos[21] = isBeneficiaryBankValid
                ? nvl(supplier.getBeneficiaryBankName())
                : "";

        String ruteo = supplier.getRoutingCodeSwift();

        if (ruteo == null || ruteo.isBlank()) {
            ruteo = supplier.getRoutingCodeAba();
        }

        campos[22] = nvl(ruteo);

        campos[23] = nvl(supplier.getBankCountry());
        campos[24] = nvl(supplier.getIntermediaryAccount());

        String intRuteo = supplier.getIntermediaryRoutingCodeSwift();

        if (intRuteo == null || intRuteo.isBlank()) {
            intRuteo = supplier.getIntermediaryRoutingCodeAba();
        }

        campos[25] = nvl(intRuteo);

        campos[26] = nvl(supplier.getIntermediaryAccountCountry());

        campos[27] = nvl(outputFileName);

        return String.join("|", campos);
    }

    private String nvl(String val) {
        return val == null ? "" : val;
    }

    @Override
    @Transactional
    public void rechazarPago(Long id) {

        PagosArchivo pago = pagosRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Pago no encontrado: " + id));

        pago.setEstatus("RECHAZADO");

        pagosRepo.save(pago);

    }

    @Override
    @Transactional
    public void rechazarPagos(List<Long> ids) {

        List<PagosArchivo> pagos = pagosRepo.findAllById(ids);

        pagos.forEach(p -> p.setEstatus("RECHAZADO"));

        pagosRepo.saveAll(pagos);

    }

}