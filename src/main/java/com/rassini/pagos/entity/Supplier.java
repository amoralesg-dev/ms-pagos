package com.rassini.pagos.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "CreditorCode", length = 10)
    private String creditorCode;

    @Column(name = "BusinessRelationName1", length = 140)
    private String businessRelationName1;

    @Column(name = "BusinessRelationSearchName", length = 20)
    private String businessRelationSearchName;

    @Column(name = "CreditorTaxIDFederal", length = 50)
    private String creditorTaxIDFederal;

    @Column(name = "AddressStreet1", length = 70)
    private String addressStreet1;

    @Column(name = "StreetNumber", length = 16)
    private String streetNumber;

    @Column(name = "AddressZip", length = 16)
    private String addressZip;

    @Column(name = "CityCode", length = 35)
    private String cityCode;

    @Column(name = "StateCode", length = 35)
    private String stateCode;

    @Column(name = "CountryCode", length = 2)
    private String countryCode;

    @Column(name = "ContactEmail", length = 50)
    private String contactEmail;

    @Column(name = "CptyAccountCode", length = 10)
    private String cptyAccountCode;

    @Column(name = "Currency", length = 3)
    private String currency;

    @Column(name = "BeneficiaryName", length = 100)
    private String beneficiaryName;

    @Column(name = "AccountNumber", length = 35)
    private String accountNumber;

    @Column(name = "BeneficiaryBankName", length = 140)
    private String beneficiaryBankName;

    @Column(name = "BankCountry", length = 2)
    private String bankCountry;

    @Column(name = "RoutingCodeABA", length = 11)
    private String routingCodeABA;

    @Column(name = "RoutingCodeBIC", length = 9)
    private String routingCodeBIC;

    @Column(name = "IntermediaryAccount", length = 140)
    private String intermediaryAccount;

    @Column(name = "IntermediaryRoutingCodeABA", length = 11)
    private String intermediaryRoutingCodeABA;

    @Column(name = "IntermediaryRoutingCodeBIC", length = 9)
    private String intermediaryRoutingCodeBIC;

    @Column(name = "IntermediaryAccountCountry", length = 2)
    private String intermediaryAccountCountry;
}
