package com.rassini.pagos.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.rassini.pagos.dto.SupplierDTO;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.service.SupplierService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public List<SupplierDTO> findAll() {
        return supplierRepository.findAll()
                .stream()
                .map(this::toDTO)
                .toList();
    }

    private SupplierDTO toDTO(Supplier supplier) {
        return SupplierDTO.builder()
                .id(supplier.getId())
                .erpIdQad(supplier.getErpIdQad())
                .businessUnitCode(supplier.getBusinessUnitCode())
                .supplierCode(supplier.getSupplierCode())
                .supplierName(supplier.getSupplierName())
                .supplierSearchName(supplier.getSupplierSearchName())
                .rfc(supplier.getRfc())
                .streetName(supplier.getStreetName())
                .streetName2(supplier.getStreetName2())
                .streetName3(supplier.getStreetName3())
                .streetNumber(supplier.getStreetNumber())
                .zipCode(supplier.getZipCode())
                .cityCode(supplier.getCityCode())
                .stateCode(supplier.getStateCode())
                .stateDescription(supplier.getStateDescription())
                .countryCode(supplier.getCountryCode())
                .contactEmail(supplier.getContactEmail())
                .supplierCodeDisIntegrity(supplier.getSupplierCodeDisIntegrity())
                .supplierCurrency(supplier.getSupplierCurrency())
                .purchaseTypeCode(supplier.getPurchaseTypeCode())
                .supplierTypeCode(supplier.getSupplierTypeCode())
                .contactName(supplier.getContactName())
                .beneficiaryBankName(supplier.getBeneficiaryBankName())
                .beneficiaryAccountName(supplier.getBeneficiaryAccountName())
                .accountNumber(supplier.getAccountNumber())
                .bankCurrency(supplier.getBankCurrency())
                .bankCountry(supplier.getBankCountry())
                .routingCodeAba(supplier.getRoutingCodeAba())
                .routingCodeSwift(supplier.getRoutingCodeSwift())
                .intermediaryBankName(supplier.getIntermediaryBankName())
                .intermediaryAccount(supplier.getIntermediaryAccount())
                .intermediaryAccountCountry(supplier.getIntermediaryAccountCountry())
                .intermediaryRoutingCodeAba(supplier.getIntermediaryRoutingCodeAba())
                .intermediaryRoutingCodeSwift(supplier.getIntermediaryRoutingCodeSwift())
                .build();
    }
}
