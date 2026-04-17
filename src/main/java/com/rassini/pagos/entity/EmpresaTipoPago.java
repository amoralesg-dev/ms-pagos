package com.rassini.pagos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empresa_tipo_pago",
       uniqueConstraints = @UniqueConstraint(columnNames = {"empresa", "deal_type"}))
@Getter
@Setter
public class EmpresaTipoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "empresa", nullable = false)
    private String empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deal_type", referencedColumnName = "deal_type")
    private CatalogoTipoPago tipoPago;
}