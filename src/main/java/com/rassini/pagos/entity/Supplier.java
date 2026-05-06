package com.rassini.pagos.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "suppliers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_suppliers_creditor_erp", columnNames = {"creditor_code", "business_unit_code"})
    }
)
@Getter
@Setter
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "address_street_1")
    private String addressStreet1;

    @Column(name = "address_zip")
    private String addressZip;

    @Column(name = "bank_country")
    private String bankCountry;

    @Column(name = "beneficiary_bank_name")
    private String beneficiaryBankName;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "business_relation_name_1")
    private String businessRelationName1;

    @Column(name = "business_relation_search_name")
    private String businessRelationSearchName;

    @Column(name = "business_unit_code")
    private String businessUnitCode;

    @Column(name = "city_code")
    private String cityCode;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "country_code")
    private String countryCode;

    @Column(name = "cpty_account_code")
    private String cptyAccountCode;

    @Column(name = "creditor_code")
    private String creditorCode;

    @Column(name = "creditor_tax_id_federal")
    private String creditorTaxIDFederal;

    @Column(name = "currency")
    private String currency;

    @Column(name = "intermediary_account")
    private String intermediaryAccount;

    @Column(name = "intermediary_account_country")
    private String intermediaryAccountCountry;

    @Column(name = "intermediary_routing_code_aba")
    private String intermediaryRoutingCodeABA;

    @Column(name = "intermediary_routing_code_bic")
    private String intermediaryRoutingCodeBIC;

    @Column(name = "routing_code_aba")
    private String routingCodeABA;

    @Column(name = "routing_code_bic")
    private String routingCodeBIC;

    @Column(name = "state_code")
    private String stateCode;

    @Column(name = "street_number")
    private String streetNumber;

    @Column(name = "contact_name")
    private String contactName;

    @Enumerated(EnumType.STRING)
    @Column(name = "xml_status", nullable = false)
    private XmlStatus xmlStatus;

    @Column(name = "address_street_2")
    private String addressStreet2;

    @Column(name = "address_street_3")
    private String addressStreet3;

    @Column(name = "erp_id_qad")
    private String erpIdQad;
}
