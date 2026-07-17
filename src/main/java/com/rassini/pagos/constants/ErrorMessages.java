package com.rassini.pagos.constants;

import java.util.Map;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final Map<String, String> ES = Map.ofEntries(

        Map.entry(ErrorCodes.ERR001, "Empresa es obligatoria"),
        Map.entry(ErrorCodes.ERR002, "Cuenta ordenante es obligatoria"),
        Map.entry(ErrorCodes.ERR003, "Moneda ordenante es obligatoria"),
        Map.entry(ErrorCodes.ERR004, "Referencia es obligatoria"),
        Map.entry(ErrorCodes.ERR005, "Fecha envío es obligatoria"),
        Map.entry(ErrorCodes.ERR006, "Fecha valor es obligatoria"),
        Map.entry(ErrorCodes.ERR007, "Monto es obligatorio"),
        Map.entry(ErrorCodes.ERR008, "Moneda es obligatoria"),
        Map.entry(ErrorCodes.ERR009, "Nombre beneficiario es obligatorio"),
        Map.entry(ErrorCodes.ERR010, "Cuenta beneficiario es obligatoria"),
        Map.entry(ErrorCodes.ERR011, "Moneda beneficiario es obligatoria"),
        Map.entry(ErrorCodes.ERR012, "Nombre archivo es obligatorio"),

        Map.entry(ErrorCodes.ERR013, "Empresa excede longitud máxima de 10"),
        Map.entry(ErrorCodes.ERR014, "Cuenta ordenante excede longitud máxima de 35"),
        Map.entry(ErrorCodes.ERR015, "Moneda ordenante excede longitud máxima de 3"),
        Map.entry(ErrorCodes.ERR016, "Referencia excede longitud máxima de 255"),
        Map.entry(ErrorCodes.ERR017, "Información adicional excede longitud máxima de 2000"),
        Map.entry(ErrorCodes.ERR018, "Fecha envío excede longitud máxima de 10"),
        Map.entry(ErrorCodes.ERR019, "Fecha valor excede longitud máxima de 10"),
        Map.entry(ErrorCodes.ERR020, "Moneda excede longitud máxima de 3"),
        Map.entry(ErrorCodes.ERR021, "Código proveedor excede longitud máxima de 10"),
        Map.entry(ErrorCodes.ERR022, "Nombre beneficiario excede longitud máxima de 35"),
        Map.entry(ErrorCodes.ERR023, "RFC beneficiario excede longitud máxima de 35"),
        Map.entry(ErrorCodes.ERR024, "Cuenta beneficiario excede longitud máxima de 35"),
        Map.entry(ErrorCodes.ERR025, "Moneda beneficiario excede longitud máxima de 3"),
        Map.entry(ErrorCodes.ERR026, "Nombre archivo excede longitud máxima de 35"),

        Map.entry(ErrorCodes.ERR027, "No es posible validar supplier porque Empresa viene vacía"),
        Map.entry(ErrorCodes.ERR028, "No es posible validar supplier porque Cuenta Beneficiario viene vacía"),
        Map.entry(ErrorCodes.ERR029, "Cuenta Beneficiario debe tener al menos 8 caracteres"),
        Map.entry(ErrorCodes.ERR030, "La cuenta beneficiaria del proveedor, no existe en Integrity."),
        Map.entry(ErrorCodes.ERR031, "Existe más de un supplier para Empresa y Cuenta Beneficiario usando últimos 8 caracteres"),

        Map.entry(ErrorCodes.ERR032, "Supplier Name es obligatorio"),
        Map.entry(ErrorCodes.ERR033, "Street Name es obligatorio"),
        Map.entry(ErrorCodes.ERR034, "Street Number es obligatorio"),
        Map.entry(ErrorCodes.ERR035, "Zip Code es obligatorio"),
        Map.entry(ErrorCodes.ERR036, "City Code es obligatorio"),
        Map.entry(ErrorCodes.ERR037, "State Code es obligatorio"),
        Map.entry(ErrorCodes.ERR038, "Country Code es obligatorio"),
        Map.entry(ErrorCodes.ERR039, "Beneficiary Bank Name es obligatorio"),
        Map.entry(ErrorCodes.ERR040, "Bank Country es obligatorio"),
        Map.entry(ErrorCodes.ERR041, "Routing Code ABA o SWIFT es obligatorio"),
        Map.entry(ErrorCodes.ERR042, "Routing Code intermediario es obligatorio cuando existe cuenta intermediaria"),
        Map.entry(ErrorCodes.ERR043, "País intermediario es obligatorio cuando existe cuenta intermediaria"),

        Map.entry(ErrorCodes.ERR044, "Registro duplicado en archivo o base de datos"),
        Map.entry(ErrorCodes.ERR045, "No existe supplier para el código proveedor"),
        Map.entry(ErrorCodes.ERR046, "No existe supplier para el código proveedor en mapeo de bussines unit")

    );
}