package com.rassini.pagos.suppliers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import com.rassini.pagos.controller.SupplierHistoryController;
import com.rassini.pagos.dto.SupplierSummaryDTO;
import com.rassini.pagos.dto.SupplierVersionHistoryDTO;
import com.rassini.pagos.dto.SupplierVersionItemDTO;
import com.rassini.pagos.exception.ResourceNotFoundException;
import com.rassini.pagos.service.SupplierHistoryService;

/** MockMvc standalone (el contexto @WebMvcTest no arranca en este proyecto por la config JPA de la app). */
class SupplierHistoryControllerTest {

    private SupplierHistoryService service;
    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(SupplierHistoryService.class);
        mockMvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .standaloneSetup(new SupplierHistoryController(service))
                .setControllerAdvice(new com.rassini.pagos.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void listaProveedores_paginada() throws Exception {
        SupplierSummaryDTO s = SupplierSummaryDTO.builder().id(10L).supplierCode("1001").erpIdQad("ERP1001")
                .accountNumber("****1234").bankCurrency("USD").versionCount(2).build();
        when(service.buscarProveedores(eq("ERP1001"), eq("1001"), isNull(), isNull(), any()))
                .thenReturn(new PageImpl<>(List.of(s), PageRequest.of(1, 5), 6));

        mockMvc.perform(get("/suppliers").param("erpIdQad", "ERP1001").param("supplierCode", "1001").param("page", "1").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].accountNumber").value("****1234"))
                .andExpect(jsonPath("$.content[0].bankCurrency").value("USD"))
                .andExpect(jsonPath("$.content[0].versionCount").value(2))
                .andExpect(jsonPath("$.totalElements").value(6));

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(service).buscarProveedores(eq("ERP1001"), eq("1001"), isNull(), isNull(), cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals(1, cap.getValue().getPageNumber());
        org.junit.jupiter.api.Assertions.assertEquals(5, cap.getValue().getPageSize());
    }

    @Test
    void versiones_ordenDefaultDesc() throws Exception {
        SupplierVersionHistoryDTO h = SupplierVersionHistoryDTO.builder()
                .current(SupplierVersionItemDTO.builder().supplierId(10L).current(true).changedFields(List.of()).build())
                .versions(new PageImpl<>(List.of(SupplierVersionItemDTO.builder().supplierId(10L).versionNumber(2)
                        .changedFields(List.of("routing_code_swift")).build()), PageRequest.of(0, 20), 1))
                .build();
        when(service.obtenerVersiones(eq(10L), any())).thenReturn(h);

        mockMvc.perform(get("/suppliers/10/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current.current").value(true))
                .andExpect(jsonPath("$.versions.content[0].versionNumber").value(2))
                .andExpect(jsonPath("$.versions.content[0].changedFields[0]").value("routing_code_swift"));

        ArgumentCaptor<Pageable> cap = ArgumentCaptor.forClass(Pageable.class);
        verify(service).obtenerVersiones(eq(10L), cap.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Sort.Direction.DESC,
                cap.getValue().getSort().getOrderFor("versionNumber").getDirection());
    }

    @Test
    void proveedorNoExiste_404() throws Exception {
        when(service.obtenerVersiones(eq(99L), any())).thenThrow(new ResourceNotFoundException("Proveedor 99 no encontrado"));
        mockMvc.perform(get("/suppliers/99/versions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void versionNoExiste_404() throws Exception {
        when(service.obtenerDetalleVersion(10L, 7)).thenThrow(new ResourceNotFoundException("Version 7 no encontrada"));
        mockMvc.perform(get("/suppliers/10/versions/7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void validacionParametros_400() throws Exception {
        mockMvc.perform(get("/suppliers").param("page", "-1")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/suppliers").param("size", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/suppliers").param("size", "101")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/suppliers").param("sortField", "accountNumber")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/suppliers/10/versions").param("sortOrder", "x")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/suppliers/abc/versions")).andExpect(status().isBadRequest());
    }
}
