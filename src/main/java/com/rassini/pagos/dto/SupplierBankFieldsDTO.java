package com.rassini.pagos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Campos bancarios; accountNumber, intermediaryAccount y los ABA van enmascarados. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierBankFieldsDTO {
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
