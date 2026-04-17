package com.rassini.pagos.util;

import com.rassini.pagos.entity.PagosArchivo;

public class TxtParser {

    private TxtParser() {
        // evitar instanciación
    }

    public static PagosArchivo parseLine(String line, String nombreArchivo) {

        String[] parts = line.split("\\|", -1); // IMPORTANTE: -1 mantiene vacíos

        PagosArchivo p = new PagosArchivo();

        p.setEmpresa(get(parts, 0));
        p.setReferencia(get(parts, 1));
        p.setFechaEnvio(get(parts, 2));
        p.setFechaValor(get(parts, 3));
        p.setFecha3(get(parts, 4));
        p.setContador(get(parts, 5));
        p.setCodigoProveedor(get(parts, 6));
        p.setRfcBeneficiario(get(parts, 7));

        p.setMonto(get(parts, 9));
        p.setMoneda(get(parts, 10));
        p.setTipoCambio(get(parts, 11));
        p.setNombreBeneficiario(get(parts, 12));
        p.setCuentaBeneficiario(get(parts, 13));

        p.setMonedaBeneficiario(get(parts, 19));
        p.setMonedaOrdenante(get(parts, 20));
        p.setCuentaOrdenante(get(parts, 21));
        p.setCampoVacio5(get(parts, 22));
        p.setCampoVacio6(get(parts, 23));

        p.setNombreArchivo(nombreArchivo);

        return p;
    }

    /**
     * Método seguro para evitar errores de índice
     */
    private static String get(String[] arr, int index) {
        if (index >= arr.length) {
            return null;
        }
        String value = arr[index];
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}