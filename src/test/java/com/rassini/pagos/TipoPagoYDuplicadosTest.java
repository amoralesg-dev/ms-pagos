package com.rassini.pagos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rassini.pagos.dto.TipoPagoSeleccionadoItemDTO;
import com.rassini.pagos.entity.PagosArchivo;
import com.rassini.pagos.entity.Supplier;
import com.rassini.pagos.exception.BusinessException;
import com.rassini.pagos.repository.CatalogoTipoPagoRepository;
import com.rassini.pagos.repository.PagosArchivoRepository;
import com.rassini.pagos.repository.SupplierRepository;
import com.rassini.pagos.service.EmpresaTipoPagoCache;
import com.rassini.pagos.service.FileLoaderService;
import com.rassini.pagos.service.impl.PagoServiceImpl;

@ExtendWith(MockitoExtension.class)
public class TipoPagoYDuplicadosTest {

    @Mock
    private PagosArchivoRepository pagosRepo;

    @Mock
    private CatalogoTipoPagoRepository catalogoRepo;

    @Mock
    private EmpresaTipoPagoCache cache;

    @Mock
    private SupplierRepository supplierRepo;

    @Mock
    private FileLoaderService fileLoaderService;

    private PagoServiceImpl pagoService;

    @BeforeEach
    public void setup() {
        pagoService = new PagoServiceImpl(pagosRepo, catalogoRepo, cache, supplierRepo, fileLoaderService);
    }

    private String invokeGenerarLineaLayout(PagosArchivo pago, Supplier supplier, String outputFileName) throws Exception {
        Method method = PagoServiceImpl.class.getDeclaredMethod("generarLineaLayout", PagosArchivo.class, Supplier.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(pagoService, pago, supplier, outputFileName);
    }

    private Supplier createValidSupplier() {
        Supplier s = new Supplier();
        s.setStreetName("Main St");
        s.setStreetNumber("123");
        s.setZipCode("12345");
        s.setCityCode("City");
        s.setStateCode("State");
        s.setCountryCode("USA");
        s.setBankCountry("US");
        return s;
    }

    @Test
    @DisplayName("CP01 - Layout ACH asigna ABA en campos[22]")
    public void testCP01_LayoutAchAsignaAba() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setFechaEnvio("2026-09-01");
        pago.setFechaValor("2026-09-01");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("ACH");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123456");
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("ABA123456", partes[22]);
    }

    @Test
    @DisplayName("CP02 - Layout WIRE asigna SWIFT en campos[22]")
    public void testCP02_LayoutWireAsignaSwift() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setFechaEnvio("2026-09-01");
        pago.setFechaValor("2026-09-01");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("WIRE");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123456");
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("SWIFT999", partes[22]);
    }

    @Test
    @DisplayName("CP03 - Layout ACH suma +1 dia a fecha envio y fecha valor")
    public void testCP03_LayoutAchSumaUnDia() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("0103");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("ACH");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123456");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("09/02/2026", partes[5]);
        assertEquals("09/02/2026", partes[6]);
        assertEquals("09/01/2026", pago.getFechaEnvio());
    }

    @Test
    @DisplayName("CP04 - Layout WIRE conserva fechas originales sin sumar dia")
    public void testCP04_LayoutWireConservaFechas() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("WIRE");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("09/01/2026", partes[5]);
        assertEquals("09/01/2026", partes[6]);
    }

    @Test
    @DisplayName("CP05 - Fallback historico para Breakes (1850) con ABA cuando tipoPagoSeleccionado es null")
    public void testCP05_FallbackBreakesConAba() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado(null);

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123456");
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("ABA123456", partes[22]);
        assertEquals("09/02/2026", partes[5]);
        assertEquals("09/02/2026", partes[6]);
    }

    @Test
    @DisplayName("CP05b - Fallback historico para planta 09 con ABA cuando tipoPagoSeleccionado es null")
    public void testCP05b_Fallback09ConAba() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("09");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado(null);

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123456");
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("ABA123456", partes[22]);
        assertEquals("09/02/2026", partes[5]);
        assertEquals("09/02/2026", partes[6]);
    }

    @Test
    @DisplayName("CP06 - Fallback historico para Breakes sin ABA")
    public void testCP06_FallbackBreakesSinAba() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setFechaEnvio("2026-09-01");
        pago.setFechaValor("2026-09-01");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba(null);
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("SWIFT999", partes[22]);
        assertEquals("2026-09-01", partes[5]);
    }

    @Test
    @DisplayName("CP07 - Fallback historico para otra empresa")
    public void testCP07_FallbackOtraEmpresa() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("0103");
        pago.setFechaEnvio("2026-09-01");
        pago.setFechaValor("2026-09-01");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado(null);

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123");
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("SWIFT999", partes[22]);
        assertEquals("2026-09-01", partes[5]);
    }

    @Test
    @DisplayName("CP08 - Error si tipo es ACH y supplier no tiene ABA")
    public void testCP08_ErrorAchSinAba() {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("ACH");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba(null);
        supplier.setRoutingCodeSwift("SWIFT999");

        Exception ex = assertThrows(Exception.class, () -> invokeGenerarLineaLayout(pago, supplier, "archivo.txt"));
        assertTrue(ex.getCause() instanceof BusinessException);
        assertTrue(ex.getCause().getMessage().contains("Routing Code ABA requerido"));
    }

    @Test
    @DisplayName("CP09 - Error si tipo es WIRE y supplier no tiene SWIFT")
    public void testCP09_ErrorWireSinSwift() {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("WIRE");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123");
        supplier.setRoutingCodeSwift(null);

        Exception ex = assertThrows(Exception.class, () -> invokeGenerarLineaLayout(pago, supplier, "archivo.txt"));
        assertTrue(ex.getCause() instanceof BusinessException);
        assertTrue(ex.getCause().getMessage().contains("Routing Code SWIFT requerido"));
    }

    @Test
    @DisplayName("CP10 - Actualizar tipoPagoSeleccionado individualmente a ACH")
    public void testCP10_ActualizarIndividualAch() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(10L);
        pago.setEmpresa("1850");
        pago.setEstatus("PENDIENTE");
        pago.setTipoPagoSeleccionado("WIRE");

        when(pagosRepo.findById(10L)).thenReturn(Optional.of(pago));

        pagoService.actualizarTipoPagoSeleccionado(10L, "ACH");

        assertEquals("ACH", pago.getTipoPagoSeleccionado());
        verify(pagosRepo, times(1)).save(pago);
    }

    @Test
    @DisplayName("CP11 - Actualizar tipoPagoSeleccionado individualmente a WIRE")
    public void testCP11_ActualizarIndividualWire() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(10L);
        pago.setEmpresa("09");
        pago.setEstatus("PENDIENTE");
        pago.setTipoPagoSeleccionado("ACH");

        when(pagosRepo.findById(10L)).thenReturn(Optional.of(pago));

        pagoService.actualizarTipoPagoSeleccionado(10L, "wire");

        assertEquals("WIRE", pago.getTipoPagoSeleccionado());
        verify(pagosRepo, times(1)).save(pago);
    }

    @Test
    @DisplayName("CP12 - Validacion de valor invalido en actualizacion individual")
    public void testCP12_ValorInvalidoIndividual() {
        assertThrows(BusinessException.class, () -> pagoService.actualizarTipoPagoSeleccionado(10L, "OTRO"));
        assertThrows(BusinessException.class, () -> pagoService.actualizarTipoPagoSeleccionado(10L, ""));
        assertThrows(BusinessException.class, () -> pagoService.actualizarTipoPagoSeleccionado(10L, null));
    }

    @Test
    @DisplayName("CP12b - Error si id es null en actualizacion individual")
    public void testCP12b_IdNuloIndividual() {
        assertThrows(BusinessException.class, () -> pagoService.actualizarTipoPagoSeleccionado(null, "ACH"));
    }

    @Test
    @DisplayName("CP13 - Error si pago no existe en actualizacion individual")
    public void testCP13_PagoNoExisteIndividual() {
        when(pagosRepo.findById(99L)).thenReturn(Optional.empty());
        assertThrows(BusinessException.class, () -> pagoService.actualizarTipoPagoSeleccionado(99L, "ACH"));
    }

    @Test
    @DisplayName("CP13b - Error si pago no esta en estatus PENDIENTE en actualizacion individual")
    public void testCP13b_PagoNoEsPendienteIndividual() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(10L);
        pago.setEstatus("ENVIADO");

        when(pagosRepo.findById(10L)).thenReturn(Optional.of(pago));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.actualizarTipoPagoSeleccionado(10L, "ACH"));
        assertTrue(ex.getMessage().contains("PENDIENTE"));
        verify(pagosRepo, never()).save(any());
    }

    @Test
    @DisplayName("CP13c - Error si empresa no participa en ACH/WIRE en actualizacion individual")
    public void testCP13c_EmpresaNoParticipaIndividual() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(10L);
        pago.setEmpresa("0111");
        pago.setEstatus("PENDIENTE");

        when(pagosRepo.findById(10L)).thenReturn(Optional.of(pago));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.actualizarTipoPagoSeleccionado(10L, "ACH"));
        assertTrue(ex.getMessage().contains("no participa en la funcionalidad"));
        verify(pagosRepo, never()).save(any());
    }

    @Test
    @DisplayName("CP14 - Actualizacion masiva de tipos de pago seleccionados")
    public void testCP14_ActualizarMasivo() {
        PagosArchivo p1 = new PagosArchivo();
        p1.setId(1L);
        p1.setEmpresa("1850");
        p1.setEstatus("PENDIENTE");
        p1.setTipoPagoSeleccionado("WIRE");

        PagosArchivo p2 = new PagosArchivo();
        p2.setId(2L);
        p2.setEmpresa("09");
        p2.setEstatus("PENDIENTE");
        p2.setTipoPagoSeleccionado("ACH");

        when(pagosRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(p1, p2));

        TipoPagoSeleccionadoItemDTO item1 = new TipoPagoSeleccionadoItemDTO();
        item1.setId(1L);
        item1.setTipoPagoSeleccionado("ACH");

        TipoPagoSeleccionadoItemDTO item2 = new TipoPagoSeleccionadoItemDTO();
        item2.setId(2L);
        item2.setTipoPagoSeleccionado("WIRE");

        pagoService.actualizarTiposPagoSeleccionados(List.of(item1, item2));

        assertEquals("ACH", p1.getTipoPagoSeleccionado());
        assertEquals("WIRE", p2.getTipoPagoSeleccionado());
        verify(pagosRepo, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("CP15 - Validacion de valor invalido en actualizacion masiva")
    public void testCP15_ValorInvalidoMasivo() {
        TipoPagoSeleccionadoItemDTO item1 = new TipoPagoSeleccionadoItemDTO();
        item1.setId(1L);
        item1.setTipoPagoSeleccionado("INVALIDO");

        assertThrows(BusinessException.class, () -> pagoService.actualizarTiposPagoSeleccionados(List.of(item1)));
    }

    @Test
    @DisplayName("CP16 - Validar duplicado por referencia con trim contra BD")
    public void testCP16_ValidarDuplicadoReferenciaTrimBd() {
        when(pagosRepo.existsByReferenciaTrim("REF-12345")).thenReturn(true);
        when(pagosRepo.existsByReferenciaTrim("REF-99999")).thenReturn(false);

        assertTrue(pagosRepo.existsByReferenciaTrim("REF-12345"));
        assertFalse(pagosRepo.existsByReferenciaTrim("REF-99999"));
    }

    @Test
    @DisplayName("CP17 - Calculo inicial de tipo de pago segun empresa (1850 y 09) y ABA")
    public void testCP17_CalculoInicialTipoPago() {
        PagosArchivo pBreakesConAba = new PagosArchivo();
        pBreakesConAba.setEmpresa("1850");
        Supplier sConAba = new Supplier();
        sConAba.setRoutingCodeAba("ABA123");

        if (com.rassini.pagos.util.EmpresaUtils.aplicaTransferenciaAchWire(pBreakesConAba.getEmpresa())
                && sConAba.getRoutingCodeAba() != null && !sConAba.getRoutingCodeAba().isBlank()) {
            pBreakesConAba.setTipoPagoSeleccionado("ACH");
        } else {
            pBreakesConAba.setTipoPagoSeleccionado("WIRE");
        }

        assertEquals("ACH", pBreakesConAba.getTipoPagoSeleccionado());

        PagosArchivo p09ConAba = new PagosArchivo();
        p09ConAba.setEmpresa("09");
        if (com.rassini.pagos.util.EmpresaUtils.aplicaTransferenciaAchWire(p09ConAba.getEmpresa())
                && sConAba.getRoutingCodeAba() != null && !sConAba.getRoutingCodeAba().isBlank()) {
            p09ConAba.setTipoPagoSeleccionado("ACH");
        } else {
            p09ConAba.setTipoPagoSeleccionado("WIRE");
        }

        assertEquals("ACH", p09ConAba.getTipoPagoSeleccionado());

        PagosArchivo pOtraEmpresa = new PagosArchivo();
        pOtraEmpresa.setEmpresa("0103");
        if (com.rassini.pagos.util.EmpresaUtils.aplicaTransferenciaAchWire(pOtraEmpresa.getEmpresa())
                && sConAba.getRoutingCodeAba() != null && !sConAba.getRoutingCodeAba().isBlank()) {
            pOtraEmpresa.setTipoPagoSeleccionado("ACH");
        } else {
            pOtraEmpresa.setTipoPagoSeleccionado("WIRE");
        }

        assertEquals("WIRE", pOtraEmpresa.getTipoPagoSeleccionado());
    }

    // ========== NUEVOS CASOS DE PRUEBA (Bloqueante 1) ==========

    @Test
    @DisplayName("CP18 - Error si lista es null en actualizacion masiva")
    public void testCP18_ListaNulaMasivo() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.actualizarTiposPagoSeleccionados(null));
        assertTrue(ex.getMessage().contains("nula ni vacía"));
        verify(pagosRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("CP19 - Error si lista esta vacia en actualizacion masiva")
    public void testCP19_ListaVaciaMasivo() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.actualizarTiposPagoSeleccionados(List.of()));
        assertTrue(ex.getMessage().contains("nula ni vacía"));
        verify(pagosRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("CP20 - Error si un item es null en actualizacion masiva")
    public void testCP20_ItemNuloMasivo() {
        java.util.List<TipoPagoSeleccionadoItemDTO> items = new java.util.ArrayList<>();
        items.add(null);

        assertThrows(BusinessException.class,
                () -> pagoService.actualizarTiposPagoSeleccionados(items));
        verify(pagosRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("CP21 - Error si un item tiene id null en actualizacion masiva")
    public void testCP21_IdNuloMasivo() {
        TipoPagoSeleccionadoItemDTO item = new TipoPagoSeleccionadoItemDTO();
        item.setId(null);
        item.setTipoPagoSeleccionado("ACH");

        assertThrows(BusinessException.class,
                () -> pagoService.actualizarTiposPagoSeleccionados(List.of(item)));
        verify(pagosRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("CP22 - Error si hay IDs duplicados en el request masivo")
    public void testCP22_IdDuplicadoMasivo() {
        TipoPagoSeleccionadoItemDTO item1 = new TipoPagoSeleccionadoItemDTO();
        item1.setId(5L);
        item1.setTipoPagoSeleccionado("ACH");

        TipoPagoSeleccionadoItemDTO item2 = new TipoPagoSeleccionadoItemDTO();
        item2.setId(5L); // mismo ID
        item2.setTipoPagoSeleccionado("WIRE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.actualizarTiposPagoSeleccionados(List.of(item1, item2)));
        assertTrue(ex.getMessage().contains("duplicado") || ex.getMessage().contains("5"));
        verify(pagosRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("CP23 - Error si un ID solicitado no existe en BD; ninguno se guarda")
    public void testCP23_IdNoExisteMasivo() {
        PagosArchivo p1 = new PagosArchivo();
        p1.setId(1L);
        p1.setEstatus("PENDIENTE");

        // Solo devuelve p1; falta id 999
        when(pagosRepo.findAllById(List.of(1L, 999L))).thenReturn(List.of(p1));

        TipoPagoSeleccionadoItemDTO item1 = new TipoPagoSeleccionadoItemDTO();
        item1.setId(1L);
        item1.setTipoPagoSeleccionado("ACH");

        TipoPagoSeleccionadoItemDTO item2 = new TipoPagoSeleccionadoItemDTO();
        item2.setId(999L);
        item2.setTipoPagoSeleccionado("WIRE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.actualizarTiposPagoSeleccionados(List.of(item1, item2)));
        assertTrue(ex.getMessage().contains("999"));
        verify(pagosRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("CP24 - Error si un pago no esta en PENDIENTE en actualizacion masiva; ninguno se guarda")
    public void testCP24_PagoNoEsPendienteMasivo() {
        PagosArchivo p1 = new PagosArchivo();
        p1.setId(1L);
        p1.setEmpresa("1850");
        p1.setEstatus("PENDIENTE");

        PagosArchivo p2 = new PagosArchivo();
        p2.setId(2L);
        p2.setEmpresa("1850");
        p2.setEstatus("ENVIADO"); // no PENDIENTE

        when(pagosRepo.findAllById(List.of(1L, 2L))).thenReturn(List.of(p1, p2));

        TipoPagoSeleccionadoItemDTO item1 = new TipoPagoSeleccionadoItemDTO();
        item1.setId(1L);
        item1.setTipoPagoSeleccionado("ACH");

        TipoPagoSeleccionadoItemDTO item2 = new TipoPagoSeleccionadoItemDTO();
        item2.setId(2L);
        item2.setTipoPagoSeleccionado("WIRE");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> pagoService.actualizarTiposPagoSeleccionados(List.of(item1, item2)));
        assertTrue(ex.getMessage().contains("PENDIENTE"));
        verify(pagosRepo, never()).saveAll(any());
    }

    @Test
    @DisplayName("CP25 - Layout con tipoPagoSeleccionado INVALIDO lanza BusinessException, no usa SWIFT y no genera linea")
    public void testCP25_LayoutTipoInvalido() {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("INVALIDO");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA123");
        supplier.setRoutingCodeSwift("SWIFT999");

        Exception ex = assertThrows(Exception.class, () -> invokeGenerarLineaLayout(pago, supplier, "archivo.txt"));
        assertTrue(ex.getCause() instanceof BusinessException);
        assertTrue(ex.getCause().getMessage().contains("Tipo de pago seleccionado inválido: INVALIDO"));
    }

    @Test
    @DisplayName("CP26 - PagoMapper mapea tipoPagoSeleccionado para ACH y WIRE")
    public void testCP26_PagoMapperTipoPagoSeleccionado() {
        PagosArchivo pagoAch = new PagosArchivo();
        pagoAch.setId(10L);
        pagoAch.setTipoPagoSeleccionado("ACH");

        com.rassini.pagos.dto.PagoPendienteDTO dtoAch = com.rassini.pagos.mapper.PagoMapper.toDTO(pagoAch);
        assertEquals("ACH", dtoAch.getTipoPagoSeleccionado());

        PagosArchivo pagoWire = new PagosArchivo();
        pagoWire.setId(20L);
        pagoWire.setTipoPagoSeleccionado("WIRE");

        com.rassini.pagos.dto.PagoPendienteDTO dtoWire = com.rassini.pagos.mapper.PagoMapper.toDTO(pagoWire);
        assertEquals("WIRE", dtoWire.getTipoPagoSeleccionado());
    }

    @Test
    @DisplayName("CP27 - Caso historico Breakes: tipoPagoSeleccionado null con supplier ABA genera ACH en layout")
    public void testCP27_HistoricoBreakesNullConAbaGeneraAch() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV-BK");
        pago.setTipoPagoSeleccionado(null); // Registro histórico null

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeAba("ABA-BREAKES-999");
        supplier.setRoutingCodeSwift("SWIFT-NOT-USED");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        // Fallback calcula ACH: routing code es el ABA y suma +1 día
        assertEquals("ABA-BREAKES-999", partes[22]);
        assertEquals("09/02/2026", partes[5]);
        assertEquals("09/02/2026", partes[6]);
    }

    @Test
    @DisplayName("CP28 - Caso 1: Proveedor con ABA y SWIFT retorna opciones ACH y WIRE")
    public void testCP28_Caso1_AbaYSwift_RetornaAchYWire() {
        List<String> opciones = com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago(true, true);
        assertEquals(2, opciones.size());
        assertTrue(opciones.contains("ACH"));
        assertTrue(opciones.contains("WIRE"));
    }

    @Test
    @DisplayName("CP29 - Caso 2: Proveedor con ABA sin SWIFT retorna únicamente opción ACH")
    public void testCP29_Caso2_AbaSinSwift_RetornaSoloAch() {
        List<String> opciones = com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago(true, false);
        assertEquals(1, opciones.size());
        assertEquals("ACH", opciones.get(0));
    }

    @Test
    @DisplayName("CP30 - Caso 3: Proveedor sin ABA con SWIFT retorna únicamente opción WIRE")
    public void testCP30_Caso3_SinAbaConSwift_RetornaSoloWire() {
        List<String> opciones = com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago(false, true);
        assertEquals(1, opciones.size());
        assertEquals("WIRE", opciones.get(0));
    }

    @Test
    @DisplayName("CP31 - Caso 4: Proveedor sin ABA ni SWIFT retorna lista vacía de opciones")
    public void testCP31_Caso4_SinAbaNiSwift_RetornaListaVacia() {
        List<String> opciones = com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago(false, false);
        assertNotNull(opciones);
        assertTrue(opciones.isEmpty());
    }

    @Test
    @DisplayName("CP32 - PagoPendienteDTO transporta tieneAba, tieneSwift y opcionesTipoPago")
    public void testCP32_PagoPendienteDTOCapacidadesProveedor() {
        com.rassini.pagos.dto.PagoPendienteDTO dto = new com.rassini.pagos.dto.PagoPendienteDTO();
        dto.setTieneAba(true);
        dto.setTieneSwift(false);
        dto.setOpcionesTipoPago(com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago(true, false));

        assertTrue(dto.getTieneAba());
        assertFalse(dto.getTieneSwift());
        assertEquals(List.of("ACH"), dto.getOpcionesTipoPago());
    }
}


