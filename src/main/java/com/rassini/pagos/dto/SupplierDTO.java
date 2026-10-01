package com.rassini.pagos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {

    private Long id;
    private String erpIdQad;
    private String businessUnitCode;
    private String supplierCode;
    private String supplierName;
    private String supplierSearchName;
    private String rfc;
    private String streetName;
    private String streetName2;
    private String streetName3;
    private String streetNumber;
    private String zipCode;
    private String cityCode;
    private String stateCode;
    private String stateDescription;
    private String countryCode;
    private String contactEmail;
    private String supplierCodeDisIntegrity;
    private String supplierCurrency;
    private String purchaseTypeCode;
    private String supplierTypeCode;
    private String contactName;
    private String beneficiaryBankName;
    private String beneficiaryAccountName;
    private String accountNumber;
    private String bankCurrency;
    private String bankCountry;
    private String routingCodeAba;
    private String routingCodeSwift;
    private String intermediaryBankName;
    private String intermediaryAccount;
    private String intermediaryAccountCountry;
    private String intermediaryRoutingCodeAba;
    private String intermediaryRoutingCodeSwift;
}
