package com.rassini.pagos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pagos_archivo")
@Getter
@Setter
public class PagosArchivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String empresa;
    private String referencia;

    @Column(name = "fecha_envio")
    private String fechaEnvio;

    @Column(name = "fecha_valor")
    private String fechaValor;

    @Column(name = "fecha_3")
    private String fecha3;

    private String contador;

    @Column(name = "codigo_proveedor")
    private String codigoProveedor;

    @Column(name = "rfc_beneficiario")
    private String rfcBeneficiario;

    @Column(name = "informacion_adicional")
    private String informacionAdicional;

    private String monto;
    private String moneda;

    @Column(name = "tipo_cambio")
    private String tipoCambio;

    @Column(name = "nombre_beneficiario")
    private String nombreBeneficiario;

    @Column(name = "cuenta_beneficiario")
    private String cuentaBeneficiario;

    @Column(name = "cuenta_bancaria")
    private String cuentaBancaria;

    @Column(name = "campo_vacio_1")
    private String campoVacio1;

    @Column(name = "campo_vacio_2")
    private String campoVacio2;

    @Column(name = "campo_vacio_3")
    private String campoVacio3;

    @Column(name = "campo_vacio_4")
    private String campoVacio4;

    @Column(name = "moneda_beneficiario")
    private String monedaBeneficiario;

    @Column(name = "moneda_ordenante")
    private String monedaOrdenante;

    @Column(name = "cuenta_ordenante")
    private String cuentaOrdenante;

    @Column(name = "campo_vacio_5")
    private String campoVacio5;

    @Column(name = "campo_vacio_6")
    private String campoVacio6;

    @Column(name = "nombre_archivo")
    private String nombreArchivo;

    // 🔗 FK por deal_type (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_type", referencedColumnName = "deal_type")
    private CatalogoTipoPago tipoPago;
}