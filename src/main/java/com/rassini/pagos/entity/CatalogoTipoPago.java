package com.rassini.pagos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "catalogo_tipo_pago")
@Getter
@Setter
public class CatalogoTipoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "deal_type", nullable = false, unique = true)
    private String dealType;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "corpo")
    private Boolean corpo;

    @Column(name = "bu")
    private String bu;
}