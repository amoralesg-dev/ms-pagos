package com.rassini.pagos.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.xml.XMLConstants;

import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import com.rassini.pagos.dto.AnaliticaPendientesArchivoDTO;
import com.rassini.pagos.dto.AnaliticaPendientesEmpresaDTO;
import com.rassini.pagos.dto.AnaliticaPendientesMonedaDTO;
import com.rassini.pagos.dto.AnaliticaPendientesTipoPagoDTO;
import com.rassini.pagos.dto.ClasificarPagoItem;
import com.rassini.pagos.dto.PagoPendienteDTO;
import com.rassini.pagos.dto.ReferenciaManualItemDTO;
import com.rassini.pagos.dto.ValidacionEnvioDTO;
import com.rassini.pagos.entity.CatalogoTipoPago;
import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.mapper.PagoMapper;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.FileLoaderService;
import com.rassini.pagos.service.PagoService;
import com.rassini.pagos.util.AnaliticaPendientesUtils;
import com.rassini.pagos.util.BuUtils;
import com.rassini.pagos.util.ConstantsSuppliers;
import com.rassini.pagos.util.EmpresaUtils;

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
    private final FileLoaderService fileLoaderService;

    @Value("${loader.output.path}")
    private String outputPathBase;
    

    public PagoServiceImpl(PagosArchivoRepository pagosRepo,
            CatalogoTipoPagoRepository catalogoRepo,
            EmpresaTipoPagoCache cache,
            SupplierRepository supplierRepo,
            FileLoaderService fileLoaderService) {
        this.pagosRepo = pagosRepo;
        this.catalogoRepo = catalogoRepo;
        this.cache = cache;
        this.supplierRepo = supplierRepo;
        this.fileLoaderService=fileLoaderService;
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
            && !EmpresaUtils.obtenerEmpresasBusqueda(bu).contains(pago.getEmpresa())) {

                throw new BusinessException(
                    "El pago no pertenece a la unidad de negocio especificada");
            }

            CatalogoTipoPago tipo = catalogoRepo.findByDealType(item.getDealType())
                    .orElseThrow(() -> new BusinessException("Tipo inválido: " + item.getDealType()));

            
            String empresaPadre =
                    EmpresaUtils.obtenerEmpresaPadre(
                            pago.getEmpresa());


            if (!cache.esValido(empresaPadre, item.getDealType())) {
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
                String search,
                String fechaInicio,
                String fechaFin,
                Pageable pageable) {

        Page<PagosArchivo> page;

        if (BuUtils.isAll(bu)) {

            page = pagosRepo.filtrarEnviadosPaginadoAll(
                        search,
                        fechaInicio,
                        fechaFin,
                        pageable);

        } else {

            page = pagosRepo.filtrarEnviadosPaginadoMultiBu(
                        EmpresaUtils.obtenerEmpresasBusqueda(bu),
                        search,
                        fechaInicio,
                        fechaFin,
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
                    EmpresaUtils.obtenerEmpresasBusqueda(bu),
                    search,
                    pageable);

        }

        return page.map(PagoMapper::toDTO);
    }

    @Override
    public ValidacionEnvioDTO validarPagosPendientes(String bu) {

        List<PagosArchivo> pendientes;

        if (BuUtils.isAll(bu)) {

            pendientes = pagosRepo.findPendientesValidacionAll();

        } else {

            pendientes = pagosRepo.findPendientesValidacionMultiBu(
                    EmpresaUtils.obtenerEmpresasBusqueda(bu));
        }

        if (pendientes == null || pendientes.isEmpty()) {

            return new ValidacionEnvioDTO(
                    true,
                    Collections.emptyList());
        }
       

        Map<String, List<PagosArchivo>> pagosPorArchivo =
                pendientes.stream()
                        .collect(Collectors.groupingBy(
                                p -> p.getNombreArchivo() == null
                                        ? "SinArchivo"
                                        : p.getNombreArchivo()));

        List<String> errores = new ArrayList<>();

        for (Map.Entry<String, List<PagosArchivo>> entry
                : pagosPorArchivo.entrySet()) {

            String nombreArchivo = entry.getKey();

            long sinClasificar =
                    entry.getValue()
                            .stream()
                            .filter(p -> p.getTipoPago() == null)
                            .count();

            if (sinClasificar > 0) {

                errores.add(
                        String.format(
                                "El archivo %s tiene %d pago(s) sin clasificar",
                                nombreArchivo,
                                sinClasificar));
            }
        }

        return new ValidacionEnvioDTO(
                errores.isEmpty(),
                errores);
    }
    
    @Override
    public int enviarPagosPendientes(String bu) {

            List<PagosArchivo> pendientes;

            if (BuUtils.isAll(bu)) {

                    pendientes = pagosRepo.findPendientesPorEnviarAll();

            } else {

                    pendientes = pagosRepo.findPendientesPorEnviarMultiBu(
                                    EmpresaUtils.obtenerEmpresasBusqueda(bu));
            }

            if (pendientes == null || pendientes.isEmpty()) {
                    return 0;
            }

            ValidacionEnvioDTO validacion = validarPagosPendientes(bu);

            if (!validacion.isPermitido()) {

                    throw new BusinessException(
                                    String.join("\n", validacion.getErrores()));
            }

            Map<String, Map<String, List<PagosArchivo>>> grouped = pendientes.stream()
                            .collect(Collectors.groupingBy(
                                            p -> p.getNombreArchivo() == null
                                                            ? "SinArchivo"
                                                            : p.getNombreArchivo(),
                                            Collectors.groupingBy(
                                                            p -> (p.getTipoPago() != null
                                                                            && p.getTipoPago().getDealType() != null)
                                                                                            ? p.getTipoPago()
                                                                                                            .getDealType()
                                                                                            : "SinTipo")));

            int filesGenerated = 0;

            for (Map.Entry<String, Map<String, List<PagosArchivo>>> entryArchivo : grouped.entrySet()) {

                    String nombreArchivo = entryArchivo.getKey();

                    for (Map.Entry<String, List<PagosArchivo>> entryTipo : entryArchivo.getValue().entrySet()) {

                            String tipoPago = entryTipo.getKey();
                            List<PagosArchivo> pagos = entryTipo.getValue();

                            String cleanTipoPago = tipoPago.replaceAll("\\s+", "");

                            String baseNombreArchivo = nombreArchivo;

                            if (baseNombreArchivo != null
                                            && baseNombreArchivo.toLowerCase().endsWith(".txt")) {

                                    baseNombreArchivo = baseNombreArchivo.substring(
                                                    0,
                                                    baseNombreArchivo.length() - 4);
                            }

                            String nombreArchivoOutput = cleanTipoPago + baseNombreArchivo;

                            if (nombreArchivoOutput.length() > 31) {
                                    nombreArchivoOutput = nombreArchivoOutput.substring(0, 31);
                            }

                            String outputFileName = String.format("%s.txt", nombreArchivoOutput);

                            Map<String, List<PagosArchivo>> pagosPorEmpresa = pagos.stream()
                                            .collect(Collectors.groupingBy(
                                                            p -> EmpresaUtils.obtenerEmpresaPadre(
                                                                            p.getEmpresa())));

                            for (Map.Entry<String, List<PagosArchivo>> entryEmpresa : pagosPorEmpresa.entrySet()) {

                                    String empresaPadre = entryEmpresa.getKey();

                                    List<PagosArchivo> pagosEmpresa = entryEmpresa.getValue();

                                    List<String> lineas = new ArrayList<>();

                                    for (PagosArchivo pago : pagosEmpresa) {

                                            Supplier supplier = null;

                                            if (pago.getCodigoProveedor() != null) {

                                                    String ultimos8 = fileLoaderService.obtenerUltimos8DigitosCuenta(
                                                                    pago.getCuentaBeneficiario());

                                                    supplier = fileLoaderService.obtenerSupplierPadrePorCuenta(
                                                                    pago.getCodigoProveedor(),
                                                                    pago.getEmpresa(),
                                                                    ultimos8);
                                            }

                                            lineas.add(
                                                            generarLineaLayout(
                                                                            pago,
                                                                            supplier,
                                                                            outputFileName));

                                            pago.setNombreArchivoEnvio(outputFileName);
                                            pago.setEstatus("ENVIADO");
                                    }

                                    try {

                                            Path outputDir = Paths.get(
                                                            outputPathBase,
                                                            empresaPadre);

                                            if (!Files.exists(outputDir)) {
                                                    Files.createDirectories(outputDir);
                                            }

                                            Path outputPath = outputDir.resolve(outputFileName);

                                            Files.write(outputPath, lineas);

                                            log.info(
                                                            "Archivo generado en ruta: {}",
                                                            outputPath.toAbsolutePath());

                                            filesGenerated++;

                                    } catch (IOException e) {

                                            throw new BusinessException(
                                                            "Error al generar archivo txt: "
                                                                            + outputFileName);
                                    }
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

            campos[0] = nvl(pago.getEmpresa());//
            campos[1] = nvl(pago.getCuentaOrdenante());
            campos[2] = nvl(pago.getMonedaOrdenante());
            campos[3] = nvl(pago.getReferencia());
            if (ConstantsSuppliers.PN.equals(EmpresaUtils.obtenerEmpresaPadre(pago.getEmpresa()))) {
                    campos[4] = nvl(pago.getReferencia());
            } else {
                    campos[4] = nvl(pago.getInformacionAdicional());
            }

            if(pago.getReferenciaManual()!=null && !pago.getReferenciaManual().isBlank()){
                campos[4]=nvl(pago.getReferenciaManual());
            }

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

            // al parecer ya no se usara
            boolean isBeneficiaryBankValid = true;

            campos[21] = isBeneficiaryBankValid
                            ? nvl(supplier.getBeneficiaryBankName())
                            : "";


            if(ConstantsSuppliers.Breakes.equalsIgnoreCase(pago.getEmpresa())){
                String ruteo = supplier.getRoutingCodeAba();

                if (ruteo == null || ruteo.isBlank()) {
                        ruteo = supplier.getRoutingCodeSwift();
                }else{
                        log.info("ABA DETECTADO. Fecha envío original: {}", pago.getFechaEnvio());
                        log.info("ABA DETECTADO. Fecha valor original: {}", pago.getFechaValor());

                        campos[5] = sumarUnDia(pago.getFechaEnvio());
                        campos[6] = sumarUnDia(pago.getFechaValor());

                        log.info("ABA DETECTADO. Fecha envío +1: {}", campos[5]);
                        log.info("ABA DETECTADO. Fecha valor +1: {}", campos[6]);
                }
                campos[22] = nvl(ruteo);
            }else{
                String ruteo = supplier.getRoutingCodeSwift();
                campos[22] = nvl(ruteo);
            }

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

    private String sumarUnDia(String fecha) {
            if (fecha == null || fecha.isBlank()) {
                    return fecha;
            }

            try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

                    LocalDate date = LocalDate.parse(fecha, formatter);

                    return date
                                    .plusDays(1)
                                    .format(formatter);

            } catch (Exception e) {
                    log.error("Error procesando fecha [{}]", fecha, e);
                    return fecha;
            }
    }
    private String nvl(String val) {
        return val == null ? "" : val;
    }

    @Override
    @Transactional
    public void rechazarPago(Long id) {

        PagosArchivo pago = pagosRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Pago no encontrado: " + id));

        pago.setEstatus(ConstantsSuppliers.RECHAZADO);

        pagosRepo.save(pago);

    }

    @Override
    @Transactional
    public void rechazarPagos(List<Long> ids) {

        List<PagosArchivo> pagos = pagosRepo.findAllById(ids);

        pagos.forEach(p -> p.setEstatus(ConstantsSuppliers.RECHAZADO));

        pagosRepo.saveAll(pagos);

    }

    @Override
    @Transactional
    public void actualizarReferenciaManual(Long id, String referenciaManual) {

            PagosArchivo pago = pagosRepo.findById(id)
                            .orElseThrow(() -> new BusinessException(
                                            "No se encontró el pago con id: " + id));

            pago.setReferenciaManual(referenciaManual);

            pagosRepo.save(pago);
    }


    @Override
    @Transactional
    public void actualizarReferenciasManuales(
                    List<ReferenciaManualItemDTO> items) {

            List<Long> ids = items.stream()
                            .map(ReferenciaManualItemDTO::getId)
                            .toList();

            List<PagosArchivo> pagos = pagosRepo.findAllById(ids);

            Map<Long, String> referenciasPorId = items.stream()
                            .collect(Collectors.toMap(
                                            ReferenciaManualItemDTO::getId,
                                            ReferenciaManualItemDTO::getReferenciaManual));

            pagos.forEach(pago -> pago.setReferenciaManual(
                            referenciasPorId.get(pago.getId())));

            pagosRepo.saveAll(pagos);
    }

    @Override
    public List<AnaliticaPendientesArchivoDTO> obtenerAnaliticaPendientes(
                    String bu) {

            List<PagosArchivo> pendientes;

            if (BuUtils.isAll(bu)) {

                    pendientes = pagosRepo.findPendientesValidacionAll();

            } else {

                    pendientes = pagosRepo.findPendientesValidacionMultiBu(
                                    EmpresaUtils.obtenerEmpresasBusqueda(bu));
            }

            if (pendientes == null || pendientes.isEmpty()) {
                    return Collections.emptyList();
            }

            Map<String, List<PagosArchivo>> pagosPorArchivo = pendientes.stream()
                            .collect(Collectors.groupingBy(
                                            AnaliticaPendientesUtils::obtenerNombreArchivo));

            return pagosPorArchivo.entrySet()
                            .stream()
                            .map(entryArchivo -> {

                                    AnaliticaPendientesArchivoDTO archivoDTO = new AnaliticaPendientesArchivoDTO();

                                    archivoDTO.setNombreArchivo(
                                                    entryArchivo.getKey());

                                    archivoDTO.setCantidadPagos(
                                                    (long) entryArchivo.getValue().size());

                                    archivoDTO.setMontoTotal(
                                                    AnaliticaPendientesUtils.calcularMontoTotal(
                                                                    entryArchivo.getValue()));

                                    Map<String, List<PagosArchivo>> pagosPorEmpresa = entryArchivo.getValue()
                                                    .stream()
                                                    .collect(Collectors.groupingBy(
                                                                    AnaliticaPendientesUtils::obtenerEmpresa));

                                    List<AnaliticaPendientesEmpresaDTO> empresas = pagosPorEmpresa.entrySet()
                                                    .stream()
                                                    .map(entryEmpresa -> {

                                                            AnaliticaPendientesEmpresaDTO empresaDTO = new AnaliticaPendientesEmpresaDTO();

                                                            empresaDTO.setEmpresa(
                                                                            entryEmpresa.getKey());

                                                            empresaDTO.setCantidadPagos(
                                                                            (long) entryEmpresa.getValue().size());

                                                            empresaDTO.setMontoTotal(
                                                                            AnaliticaPendientesUtils.calcularMontoTotal(
                                                                                            entryEmpresa.getValue()));

                                                            Map<String, List<PagosArchivo>> pagosPorTipo = entryEmpresa
                                                                            .getValue()
                                                                            .stream()
                                                                            .collect(Collectors.groupingBy(
                                                                                            AnaliticaPendientesUtils::obtenerTipoPago));

                                                            List<AnaliticaPendientesTipoPagoDTO> tiposPago = pagosPorTipo
                                                                            .entrySet()
                                                                            .stream()
                                                                            .map(entryTipo -> {

                                                                                    AnaliticaPendientesTipoPagoDTO tipoDTO = new AnaliticaPendientesTipoPagoDTO();

                                                                                    tipoDTO.setTipoPago(
                                                                                                    entryTipo.getKey());

                                                                                    tipoDTO.setCantidadPagos(
                                                                                                    (long) entryTipo.getValue()
                                                                                                                    .size());

                                                                                    tipoDTO.setMontoTotal(
                                                                                                    AnaliticaPendientesUtils
                                                                                                                    .calcularMontoTotal(
                                                                                                                                    entryTipo.getValue()));

                                                                                    Map<String, List<PagosArchivo>> pagosPorMoneda = entryTipo
                                                                                                    .getValue()
                                                                                                    .stream()
                                                                                                    .collect(Collectors
                                                                                                                    .groupingBy(
                                                                                                                                    AnaliticaPendientesUtils::obtenerMoneda));

                                                                                    List<AnaliticaPendientesMonedaDTO> monedas = pagosPorMoneda
                                                                                                    .entrySet()
                                                                                                    .stream()
                                                                                                    .map(entryMoneda -> {

                                                                                                            AnaliticaPendientesMonedaDTO monedaDTO = new AnaliticaPendientesMonedaDTO();

                                                                                                            monedaDTO.setMoneda(
                                                                                                                            entryMoneda.getKey());

                                                                                                            monedaDTO.setCantidadPagos(
                                                                                                                            (long) entryMoneda
                                                                                                                                            .getValue()
                                                                                                                                            .size());

                                                                                                            monedaDTO.setMontoTotal(
                                                                                                                            AnaliticaPendientesUtils
                                                                                                                                            .calcularMontoTotal(
                                                                                                                                                            entryMoneda.getValue()));

                                                                                                            return monedaDTO;
                                                                                                    })
                                                                                                    .toList();

                                                                                    tipoDTO.setMonedas(monedas);

                                                                                    return tipoDTO;
                                                                            })
                                                                            .toList();

                                                            empresaDTO.setTiposPago(tiposPago);

                                                            return empresaDTO;
                                                    })
                                                    .toList();

                                    archivoDTO.setEmpresas(empresas);

                                    return archivoDTO;
                            })
                            .toList();
    }

}