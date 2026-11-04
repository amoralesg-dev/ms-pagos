package com.rassini.pagos.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Snapshot (solo lectura) de un renglon de suppliers antes de un cambio bancario.
 * La tabla es creada y escrita por graphite_client_ms; ms-pagos solo la consulta.
 */
@Entity
@Immutable
@Table(name = "suppliers_ver")
@Getter
@Setter
public class SupplierVersion {

    @Id
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "erp_id_qad")
    private String erpIdQad;

    @Column(name = "business_unit_code")
    private String businessUnitCode;

    @Column(name = "supplier_code")
    private String supplierCode;

    @Column(name = "supplier_name")
    private String supplierName;

    @Column(name = "rfc")
    private String rfc;

    @Column(name = "beneficiary_bank_name")
    private String beneficiaryBankName;

    @Column(name = "beneficiary_account_name")
    private String beneficiaryAccountName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "bank_currency")
    private String bankCurrency;

    @Column(name = "bank_country")
    private String bankCountry;

    @Column(name = "routing_code_aba")
    private String routingCodeAba;

    @Column(name = "routing_code_swift")
    private String routingCodeSwift;

    @Column(name = "intermediary_bank_name")
    private String intermediaryBankName;

    @Column(name = "intermediary_account")
    private String intermediaryAccount;

    @Column(name = "intermediary_account_country")
    private String intermediaryAccountCountry;

    @Column(name = "intermediary_routing_code_aba")
    private String intermediaryRoutingCodeAba;

    @Column(name = "intermediary_routing_code_swift")
    private String intermediaryRoutingCodeSwift;

    @Column(name = "changed_fields", length = 1000)
    private String changedFields;

    @Column(name = "version_source", length = 30)
    private String versionSource;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
