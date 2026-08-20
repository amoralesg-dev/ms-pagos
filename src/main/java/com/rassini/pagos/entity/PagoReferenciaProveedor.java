package com.rassini.pagos.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
    name = "pago_referencia_proveedor",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_pago_ref_proveedor",
            columnNames = {"bu", "proveedor"}
        )
    }
)
public class PagoReferenciaProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String bu;

    @Column(nullable = false, length = 50)
    private String proveedor;

    @Column(name = "nombre_proveedor", nullable = false)
    private String nombreProveedor;

    @Column(nullable = false, length = 20)
    private String seleccion;

    @Column(nullable = false)
    private String referencia;

    @Column(nullable = false, length = 50)
    private String entidad;

    // getters y setters
}