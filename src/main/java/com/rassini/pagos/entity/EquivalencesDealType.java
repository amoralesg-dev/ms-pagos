package com.rassini.pagos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "equivalences_deal_type")
@Getter
@Setter
public class EquivalencesDealType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bu", nullable = false, length = 50)
    private String bu;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "des_code", nullable = false, length = 255)
    private String desCode;

    @Column(name = "equivalences", nullable = false, length = 100)
    private String equivalences;
}
