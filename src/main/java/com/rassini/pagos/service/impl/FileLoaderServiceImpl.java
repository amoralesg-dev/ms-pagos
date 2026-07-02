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

import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.service.FileLoaderService;
import com.rassini.pagos.util.TxtParser;

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
                    System.out.println(folder);


        if (!folder.exists() || !folder.isDirectory()) {
            throw new RuntimeException("Ruta inválida configurada: " + rutaCarpeta);
        }

        File[] archivos = folder.listFiles((dir, name) -> name.endsWith(".txt"));

        if (archivos == null || archivos.length == 0) {
            System.out.println("No hay archivos para procesar");
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
        errores.add(mensaje);
    }

    private void validarLongitud(
        String valor,
        int longitudMaxima,
        String nombreCampo,
        List<String> errores) {

        if (!isBlank(valor) && valor.trim().length() > longitudMaxima) {
            agregarError(
                    errores,
                    nombreCampo + " excede longitud máxima de " + longitudMaxima
            );
        }
    }

    private void validarCamposSupplier(
        Supplier supplier,
        List<String> errores) {

        if (isBlank(supplier.getSupplierName())) {
            agregarError(errores,
                    "Supplier Name es obligatorio");
        }

        if (isBlank(supplier.getStreetName())) {
            agregarError(errores,
                    "Street Name es obligatorio");
        }

        if (isBlank(supplier.getStreetNumber())) {
            agregarError(errores,
                    "Street Number es obligatorio");
        }

        if (isBlank(supplier.getZipCode())) {
            agregarError(errores,
                    "Zip Code es obligatorio");
        }

        if (isBlank(supplier.getCityCode())) {
            agregarError(errores,
                    "City Code es obligatorio");
        }

        if (isBlank(supplier.getStateCode())) {
            agregarError(errores,
                    "State Code es obligatorio");
        }

        if (isBlank(supplier.getCountryCode())) {
            agregarError(errores,
                    "Country Code es obligatorio");
        }

        if (isBlank(supplier.getBeneficiaryBankName())) {
            agregarError(errores,
                    "Beneficiary Bank Name es obligatorio");
        }

        if (isBlank(supplier.getBankCountry())) {
            agregarError(errores,
                    "Bank Country es obligatorio");
        }
        if (isBlank(supplier.getRoutingCodeAba())
            && isBlank(supplier.getRoutingCodeSwift())) {

            agregarError(
                    errores,
                    "Routing Code ABA o SWIFT es obligatorio");
        }

        if (!isBlank(supplier.getIntermediaryAccount())) {

            if (isBlank(supplier.getIntermediaryRoutingCodeAba())
                    && isBlank(supplier.getIntermediaryRoutingCodeSwift())) {

                agregarError(
                        errores,
                        "Routing Code intermediario es obligatorio cuando existe cuenta intermediaria");
            }

            if (isBlank(supplier.getIntermediaryAccountCountry())) {

                agregarError(
                        errores,
                        "País intermediario es obligatorio cuando existe cuenta intermediaria");
            }
        }
    }

    private void validarCamposPagosArchivo(
        PagosArchivo pago,
        List<String> errores) {

        if (isBlank(pago.getEmpresa())) {
            agregarError(errores, "Empresa es obligatoria");
        }

        if (isBlank(pago.getCuentaOrdenante())) {
            agregarError(errores, "Cuenta ordenante es obligatoria");
        }

        if (isBlank(pago.getMonedaOrdenante())) {
            agregarError(errores, "Moneda ordenante es obligatoria");
        }

        if (isBlank(pago.getReferencia())) {
            agregarError(errores, "Referencia es obligatoria");
        }

        if (isBlank(pago.getFechaEnvio())) {
            agregarError(errores, "Fecha envío es obligatoria");
        }

        if (isBlank(pago.getFechaValor())) {
            agregarError(errores, "Fecha valor es obligatoria");
        }

        if (isBlank(pago.getMonto())) {
            agregarError(errores, "Monto es obligatorio");
        }

        if (isBlank(pago.getMoneda())) {
            agregarError(errores, "Moneda es obligatoria");
        }

        if (isBlank(pago.getNombreBeneficiario())) {
            agregarError(errores, "Nombre beneficiario es obligatorio");
        }

        if (isBlank(pago.getCuentaBeneficiario())) {
            agregarError(errores, "Cuenta beneficiario es obligatoria");
        }

        if (isBlank(pago.getMonedaBeneficiario())) {
            agregarError(errores, "Moneda beneficiario es obligatoria");
        }

        if (isBlank(pago.getNombreArchivo())) {
            agregarError(errores, "Nombre archivo es obligatorio");
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

        validarLongitud(
                pago.getNombreBeneficiario(),
                35,
                "Nombre beneficiario",
                errores);

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
        List<String> errores,
        Map<String, List<Supplier>> indiceSuppliers) {

        if (isBlank(pago.getEmpresa())) {

            
            agregarError(
                errores,
                "No es posible validar supplier porque Empresa viene vacía");
            return null;
        }

        if (isBlank(pago.getCuentaBeneficiario())) {
            agregarError(
                    errores,
                    "No es posible validar supplier porque Cuenta Beneficiario viene vacía");
            return null;
        }

        String cuentaBeneficiario = pago.getCuentaBeneficiario().trim();

        if (cuentaBeneficiario.length() < 8) {
            agregarError(
                    errores,
                    "Cuenta Beneficiario debe tener al menos 8 caracteres");
            return null;
        }

        String ultimos8 = cuentaBeneficiario.substring(
                cuentaBeneficiario.length() - 8);

        List<Supplier> suppliers = indiceSuppliers.get(ultimos8);

        if (suppliers == null || suppliers.isEmpty()) {

            agregarError(
                    errores,
                    "No existe supplier para Empresa "
                            + pago.getEmpresa()
                            + " y Cuenta Beneficiario "
                            + pago.getCuentaBeneficiario());

            return null;
        }

        if (suppliers.size() > 1) {

            agregarError(
                    errores,
                    "Existe más de un supplier para Empresa "
                            + pago.getEmpresa()
                            + " y Cuenta Beneficiario "
                            + pago.getCuentaBeneficiario()
                            + " usando últimos 8 caracteres: "
                            + ultimos8);

            return null;
        }

        return suppliers.get(0);
    }

    private Map<String, List<Supplier>> obtenerIndiceSuppliersPorEmpresa(
        String empresa,
        Map<String, Map<String, List<Supplier>>> indicesPorEmpresa) {

        if (isBlank(empresa)) {
            return new HashMap<>();
        }

        String empresaNormalizada = empresa.trim();

        if (indicesPorEmpresa.containsKey(empresaNormalizada)) {
            return indicesPorEmpresa.get(empresaNormalizada);
        }

        List<Supplier> suppliers =
                supplierRepository.findByBusinessUnitCodeAndAccountNumberIsNotNull(
                        empresaNormalizada
                );

        Map<String, List<Supplier>> indice =
                construirIndiceSuppliersPorUltimos8(suppliers);

        indicesPorEmpresa.put(empresaNormalizada, indice);

        return indice;
    }

    private void procesarArchivo(File archivo) {

        List<PagosArchivo> batch = new ArrayList<>();
        Set<String> registrosProcesados = new HashSet<>();

        try (BufferedReader br = new BufferedReader(new FileReader(archivo))) {

            String line;

            Map<String, Map<String, List<Supplier>>> indicesPorEmpresa = new HashMap<>();

            while ((line = br.readLine()) != null) {

                try {
                    PagosArchivo pago = TxtParser.parseLine(line, archivo.getName());

                    List<String> errores = new ArrayList<>();

                    validarCamposPagosArchivo(pago, errores);

                    Map<String, List<Supplier>> indiceSuppliers =
            obtenerIndiceSuppliersPorEmpresa(
                pago.getEmpresa(),
                indicesPorEmpresa);

                Supplier supplier =
                        validarYObtenerSupplier(
                                pago,
                                errores,
                                indiceSuppliers);

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

                        //pago.setDuplicado("S");

                        agregarError(
                                errores,
                                "Registro duplicado en archivo o base de datos");

                    } else {

                        //pago.setDuplicado("N");

                        registrosProcesados.add(uniqueKey);
                    }

                    if (!errores.isEmpty()) {
                        pago.setMensaje(String.join(" | ", errores));
                    } else {
                        pago.setMensaje(null);
                    }

                    batch.add(pago);

                    if (batch.size() == 500) {
                        repository.saveAll(batch);
                        batch.clear();
                    }

                } catch (Exception e) {
                    System.out.println("Error en línea: " + line);
                }
            }

            if (!batch.isEmpty()) {
                repository.saveAll(batch);
            }

            System.out.println("Archivo procesado: " + archivo.getName());

        } catch (Exception e) {
            throw new RuntimeException("Error procesando archivo: " + archivo.getName(), e);
        }
    }

    private Map<String, List<Supplier>> construirIndiceSuppliersPorUltimos8(
        List<Supplier> suppliers) {

        Map<String, List<Supplier>> index = new HashMap<>();

        for (Supplier supplier : suppliers) {

            if (isBlank(supplier.getAccountNumber())) {
                continue;
            }

            String accountNumber = supplier.getAccountNumber().trim();

            if (accountNumber.length() < 8) {
                continue;
            }

            String ultimos8 = accountNumber.substring(accountNumber.length() - 8);

            index.computeIfAbsent(ultimos8, key -> new ArrayList<>())
                    .add(supplier);
        }

        return index;
    }


}