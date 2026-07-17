package com.rassini.pagos.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.rassini.pagos.constants.ErrorCodes;
import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.exception.BusinessExceptionCode;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.service.FileLoaderService;
import com.rassini.pagos.util.EmpresaUtils;
import com.rassini.pagos.util.TxtParser;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class FileLoaderServiceImpl implements FileLoaderService {

    private final PagosArchivoRepository repository;
    private final SupplierRepository supplierRepository;

    @Value("${loader.path}")
    private String rutaCarpeta;

    
    public FileLoaderServiceImpl(PagosArchivoRepository repository, SupplierRepository supplierRepository) {
        this.repository = repository;
        this.supplierRepository = supplierRepository;
    }

    @Override
    public void cargarArchivos() {

        File folder = new File(rutaCarpeta);
                    log.info(folder);


        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("Ruta inválida configurada: " + rutaCarpeta);
        }

        File[] archivos = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (archivos == null || archivos.length == 0) {
            log.info("No hay archivos para procesar");
            return;
        }

        for (File archivo : archivos) {
            procesarArchivo(archivo);
        }
    }

    private boolean isBlank(String valor) {
        return valor == null || valor.trim().isEmpty();
    }

    private void agregarError(List<String> errores, String mensaje) {

    if (!errores.contains(mensaje)) {
        errores.add(mensaje);
    }
}

    private String error(String codigo, String mensaje) {
        log.info("Error generado: {} - {}", codigo, mensaje);
        return codigo;
    }

    private String error(String codigo, String formato, Object... args) {
        log.info("Error generado: {} - {}", codigo, String.format(formato, args));
        return codigo;
    }

    private void validarLongitud(
        String valor,
        int longitudMaxima,
        String nombreCampo,
        List<String> errores) {

        if (!isBlank(valor) && valor.trim().length() > longitudMaxima) {

            switch (nombreCampo) {

                case "Empresa":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR013,
                                    "Empresa excede longitud máxima de 10"));
                    break;

                case "Cuenta ordenante":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR014,
                                    "Cuenta ordenante excede longitud máxima de 35"));
                    break;

                case "Moneda ordenante":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR015,
                                    "Moneda ordenante excede longitud máxima de 3"));
                    break;

                case "Referencia":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR016,
                                    "Referencia excede longitud máxima de 255"));
                    break;

                case "Información adicional":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR017,
                                    "Información adicional excede longitud máxima de 2000"));
                    break;

                case "Fecha envío":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR018,
                                    "Fecha envío excede longitud máxima de 10"));
                    break;

                case "Fecha valor":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR019,
                                    "Fecha valor excede longitud máxima de 10"));
                    break;

                case "Moneda":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR020,
                                    "Moneda excede longitud máxima de 3"));
                    break;

                case "Código proveedor":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR021,
                                    "Código proveedor excede longitud máxima de 10"));
                    break;

                case "RFC beneficiario":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR023,
                                    "RFC beneficiario excede longitud máxima de 35"));
                    break;

                case "Cuenta beneficiario":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR024,
                                    "Cuenta beneficiario excede longitud máxima de 35"));
                    break;

                case "Moneda beneficiario":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR025,
                                    "Moneda beneficiario excede longitud máxima de 3"));
                    break;

                case "Nombre archivo":
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR026,
                                    "Nombre archivo excede longitud máxima de 35"));
                    break;

                default:
                    agregarError(
                            errores,
                            error(ErrorCodes.ERR999,
                                    "%s excede longitud máxima de %s",
                                    nombreCampo,
                                    longitudMaxima));
            }
        }
    }

    private void validarCamposSupplier(
        Supplier supplier,
        List<String> errores) {

        if (isBlank(supplier.getSupplierName())) {
            agregarError(
        errores,
        error(ErrorCodes.ERR032,
                "Supplier Name es obligatorio"));

        }

        if (isBlank(supplier.getStreetName())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR033,
                            "Street Name es obligatorio"));
        }

        if (isBlank(supplier.getStreetNumber())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR034,
                            "Street Number es obligatorio"));
        }

        if (isBlank(supplier.getZipCode())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR035,
                            "Zip Code es obligatorio"));
        }

        if (isBlank(supplier.getCityCode())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR036,
                            "City Code es obligatorio"));
        }

        if (isBlank(supplier.getStateCode())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR037,
                            "State Code es obligatorio"));
        }

        if (isBlank(supplier.getCountryCode())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR038,
                            "Country Code es obligatorio"));
        }

        if (isBlank(supplier.getBeneficiaryBankName())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR039,
                            "Beneficiary Bank Name es obligatorio"));
        }

        if (isBlank(supplier.getBankCountry())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR040,
                            "Bank Country es obligatorio"));
        }
        if (isBlank(supplier.getRoutingCodeAba())
            && isBlank(supplier.getRoutingCodeSwift())) {

            agregarError(
                    errores,
                    error(ErrorCodes.ERR041,
                            "Routing Code ABA o SWIFT es obligatorio"));
        }

        if (!isBlank(supplier.getIntermediaryAccount())) {

            if (isBlank(supplier.getIntermediaryRoutingCodeAba())
                    && isBlank(supplier.getIntermediaryRoutingCodeSwift())) {

                agregarError(
                        errores,
                        error(ErrorCodes.ERR042,
                                "Routing Code intermediario es obligatorio cuando existe cuenta intermediaria"));
            }

            if (isBlank(supplier.getIntermediaryAccountCountry())) {

                agregarError(
                        errores,
                        error(ErrorCodes.ERR043,
                                "País intermediario es obligatorio cuando existe cuenta intermediaria"));
            }
        }
    }

    private void validarCamposPagosArchivo(
        PagosArchivo pago,
        List<String> errores) {

        if (isBlank(pago.getEmpresa())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR001, "Empresa es obligatoria"));
        }

        if (isBlank(pago.getCuentaOrdenante())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR002, "Cuenta ordenante es obligatoria"));
        }

        if (isBlank(pago.getMonedaOrdenante())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR003, "Moneda ordenante es obligatoria"));
        }

        if (isBlank(pago.getReferencia())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR004, "Referencia es obligatoria"));
        }

        if (isBlank(pago.getFechaEnvio())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR005, "Fecha envío es obligatoria"));
        }

        if (isBlank(pago.getFechaValor())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR006, "Fecha valor es obligatoria"));
        }

        if (isBlank(pago.getMonto())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR007, "Monto es obligatorio"));
        }

        if (isBlank(pago.getMoneda())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR008, "Moneda es obligatoria"));
        }

        if (isBlank(pago.getNombreBeneficiario())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR009, "Nombre beneficiario es obligatorio"));
        }

        if (isBlank(pago.getCuentaBeneficiario())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR010, "Cuenta beneficiario es obligatoria"));
        }

        if (isBlank(pago.getMonedaBeneficiario())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR011, "Moneda beneficiario es obligatoria"));
        }

        if (isBlank(pago.getNombreArchivo())) {
            agregarError(
                errores,
                error(ErrorCodes.ERR012, "Nombre archivo es obligatorio"));
        }
        validarLongitud(
        pago.getEmpresa(),
        10,
        "Empresa",
        errores);

        validarLongitud(
                pago.getCuentaOrdenante(),
                35,
                "Cuenta ordenante",
                errores);

        validarLongitud(
                pago.getMonedaOrdenante(),
                3,
                "Moneda ordenante",
                errores);

        validarLongitud(
                pago.getReferencia(),
                255,
                "Referencia",
                errores);

        validarLongitud(
                pago.getInformacionAdicional(),
                2000,
                "Información adicional",
                errores);

        validarLongitud(
                pago.getFechaEnvio(),
                10,
                "Fecha envío",
                errores);

        validarLongitud(
                pago.getFechaValor(),
                10,
                "Fecha valor",
                errores);

        validarLongitud(
                pago.getMoneda(),
                3,
                "Moneda",
                errores);

        validarLongitud(
                pago.getCodigoProveedor(),
                10,
                "Código proveedor",
                errores);

        if (pago.getNombreBeneficiario() != null && pago.getNombreBeneficiario().length() > 35) {
            pago.setNombreBeneficiario(pago.getNombreBeneficiario().trim().substring(0, 35));
        }

        validarLongitud(
                pago.getRfcBeneficiario(),
                35,
                "RFC beneficiario",
                errores);

        validarLongitud(
                pago.getCuentaBeneficiario(),
                35,
                "Cuenta beneficiario",
                errores);

        validarLongitud(
                pago.getMonedaBeneficiario(),
                3,
                "Moneda beneficiario",
                errores);

        validarLongitud(
                pago.getNombreArchivo(),
                35,
                "Nombre archivo",
                errores);
    }

    private Supplier validarYObtenerSupplier(
        PagosArchivo pago,
        List<String> errores) {

        
    List<Supplier> suppliersList = supplierRepository.findByErpIdQad(pago.getCodigoProveedor());


        if(suppliersList.isEmpty()) {
            agregarError(
                errores,
                error(ErrorCodes.ERR045, "No existe supplier para el código proveedor %s", pago.getCodigoProveedor()));
            return null;
        }

        if (isBlank(pago.getEmpresa())) {

            
            agregarError(
                errores,
                error(ErrorCodes.ERR027, "No es posible validar supplier porque Empresa viene vacía"));
            return null;
        }

        if (isBlank(pago.getCuentaBeneficiario())) {
            agregarError(
                    errores,
                    error(ErrorCodes.ERR028, "No es posible validar supplier porque Cuenta Beneficiario viene vacía"));
            return null;
        }

        String cuentaBeneficiario = pago.getCuentaBeneficiario().trim();
        String ultimos8=obtenerUltimos8DigitosCuenta(cuentaBeneficiario);
        String empresaPadre="";

        try {
            empresaPadre = EmpresaUtils.obtenerEmpresaPadre(
                    pago.getEmpresa());
        } catch (BusinessException e) {

            agregarError(
                    errores,
                    error(
                            ErrorCodes.ERR046,
                            e.getMessage()));

            return null;
        }

        
        Supplier supplier;

        try {
            supplier = obtenerSupplierPadrePorCuenta(
                    pago.getCodigoProveedor(),
                    pago.getEmpresa(),
                    ultimos8);
        } catch (BusinessExceptionCode e) {

            agregarError(
                    errores,
                    error(
                            e.getCodigo(),
                            e.getMessage()));

            return null;
        }

        return supplier;
    }

    @Override
    public Supplier obtenerSupplierPadre(
        String codigoProveedor,
        String empresa) {

        String empresaPadre =
                EmpresaUtils.obtenerEmpresaPadre(empresa);

        Supplier supplier = (Supplier) supplierRepository
                .findByErpIdQadAndBusinessUnitCode(
                        codigoProveedor,
                        empresaPadre);

        if (supplier == null) {
            throw new BusinessExceptionCode(
                    ErrorCodes.ERR045,
                    String.format(
                            "No existe supplier para proveedor %s en empresa %s",
                            codigoProveedor,
                            empresaPadre));
        }

        return supplier;
    }

    @Override
    public Supplier obtenerSupplierPadrePorCuenta(
        String codigoProveedor,
        String empresa,
        String ultimos8) {

        String empresaPadre =
                EmpresaUtils.obtenerEmpresaPadre(empresa);

        List<Supplier> suppliers = supplierRepository
                .findByCodigoProveedorAndEmpresaAndAccountNumberEndsWith(
                        codigoProveedor,
                        empresaPadre,
                        ultimos8);

        if (suppliers.isEmpty()) {
            throw new BusinessExceptionCode(
                    ErrorCodes.ERR030,
                    "La cuenta beneficiaria del proveedor, no existe en Integrity.");
        }

        if (suppliers.size() > 1) {
            throw new BusinessExceptionCode(
                    ErrorCodes.ERR031,
                    "Existe más de un supplier para Empresa y Cuenta Beneficiario usando últimos 8 caracteres");
        }

        return suppliers.get(0);
    }    
    @Override
    public String obtenerUltimos8DigitosCuenta(String cuentaBeneficiario) {

        if (cuentaBeneficiario == null
                || cuentaBeneficiario.isBlank()) {

            throw new BusinessException(
                    "Cuenta beneficiario viene vacía");
        }

        String cuenta = cuentaBeneficiario.trim();

        if (cuenta.length() >= 8) {
            return cuenta.substring(cuenta.length() - 8);
        }

        return cuenta;
    }
    private void procesarArchivo(File archivo) {

        List<PagosArchivo> batch = new ArrayList<>();
        Set<String> registrosProcesados = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String line;


            while ((line = br.readLine()) != null) {

                try {
                    PagosArchivo pago = TxtParser.parseLine(line, archivo.getName());

                    List<String> errores = new ArrayList<>();

                    validarCamposPagosArchivo(pago, errores);

                Supplier supplier =
                        validarYObtenerSupplier(
                                pago,
                                errores);

                    if (supplier != null) {
                        validarCamposSupplier(
                                supplier,
                                errores);
                    }


                    // Validar si existe duplicado (en memoria para el archivo actual y luego en BD)
                    String uniqueKey = pago.getNombreArchivo() + "|" +
                                       pago.getMonto() + "|" +
                                       pago.getCodigoProveedor() + "|" +
                                       pago.getFechaEnvio();

                    boolean existeDuplicado = registrosProcesados.contains(uniqueKey);

                    if (!existeDuplicado) {
                        existeDuplicado = repository.existsByNombreArchivoAndMontoAndCodigoProveedorAndFechaEnvio(
                            pago.getNombreArchivo(),
                            pago.getMonto(),
                            pago.getCodigoProveedor(),
                            pago.getFechaEnvio()
                        );
                    }

                    if (existeDuplicado) {

                        agregarError(
                            errores,
                            error(
                                    ErrorCodes.ERR044,
                                    "Registro duplicado en archivo o base de datos"));

                    } else {

                        registrosProcesados.add(uniqueKey);
                    }

                    if (!errores.isEmpty()) {
                        pago.setMensaje(String.join(" | ", errores));
                        pago.setEstatus("ERROR");
                    } else {
                        pago.setMensaje(null);
                        pago.setEstatus("PENDIENTE");
                    }

                    batch.add(pago);

                    if (batch.size() == 500) {
                        repository.saveAll(batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    log.error("Error procesando línea: " + line, e);
                    log.info("Error en línea: " + line);
                }
            }

            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }

            log.info("Archivo procesado: " + archivo.getName());

        } catch (Exception e) {
            throw new RuntimeException("Error procesando archivo: " + archivo.getName(), e);
        }
    }

    
}