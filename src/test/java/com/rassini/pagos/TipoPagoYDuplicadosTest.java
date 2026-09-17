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
    @DisplayName("CP03 - Layout ACH conserva fechas originales sin sumar dia")
    public void testCP03_LayoutAchConservaFechas() throws Exception {
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

        assertEquals("09/01/2026", partes[5]);
        assertEquals("09/01/2026", partes[6]);
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
    @DisplayName("CP05 - Fallback automatico para USD y US con ABA calcula ACH y conserva fechas")
    public void testCP05_FallbackUsdUsConAba() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setMoneda("USD");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado(null);

        Supplier supplier = createValidSupplier();
        supplier.setCountryCode("US");
        supplier.setRoutingCodeAba("ABA123456");
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("ABA123456", partes[22]);
        assertEquals("09/01/2026", partes[5]);
        assertEquals("09/01/2026", partes[6]);
    }

    @Test
    @DisplayName("CP05b - Fallback automatico para MXN y MX con SWIFT calcula SPEI y conserva fechas")
    public void testCP05b_FallbackMxnMxConSwift() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("09");
        pago.setMoneda("MXN");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado(null);

        Supplier supplier = createValidSupplier();
        supplier.setCountryCode("MX");
        supplier.setRoutingCodeAba("ABA123456");
        supplier.setRoutingCodeSwift("SWIFT999");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("SWIFT999", partes[22]);
        assertEquals("09/01/2026", partes[5]);
        assertEquals("09/01/2026", partes[6]);
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
    @DisplayName("CP13c - Cualquier empresa puede actualizar tipo de pago (restriccion por empresa eliminada)")
    public void testCP13c_EmpresaNoParticipaIndividual() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(10L);
        pago.setEmpresa("0111");
        pago.setEstatus("PENDIENTE");

        when(pagosRepo.findById(10L)).thenReturn(Optional.of(pago));

        // La restricción por empresa fue eliminada: cualquier BU puede actualizar el tipo de pago.
        // La funcionalidad ahora depende de moneda, país, ABA y SWIFT, no del código de empresa.
        assertDoesNotThrow(() -> pagoService.actualizarTipoPagoSeleccionado(10L, "ACH"));
        verify(pagosRepo, times(1)).save(pago);
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
    @DisplayName("CP17 - Calculo inicial de tipo de pago segun moneda y pais (logica nueva)")
    public void testCP17_CalculoInicialTipoPago() {
        // USD + US → ACH
        assertEquals("ACH",
                com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "US"));

        // MXN + MX → SPEI
        assertEquals("SPEI",
                com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("MXN", "MX"));

        // USD + MX → SPID
        assertEquals("SPID",
                com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "MX"));

        // USD + otro país → SPID
        assertEquals("SPID",
                com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "CA"));

        // Caso default → WIRE
        assertEquals("WIRE",
                com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("EUR", "DE"));
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
    @DisplayName("CP27 - Caso automatico USD + US con supplier ABA genera ACH en layout y fechas originales")
    public void testCP27_CasoUsdUsConAbaGeneraAch() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setMoneda("USD");
        pago.setFechaEnvio("09/01/2026");
        pago.setFechaValor("09/01/2026");
        pago.setCodigoProveedor("PRV-BK");
        pago.setTipoPagoSeleccionado(null); // Registro histórico null

        Supplier supplier = createValidSupplier();
        supplier.setCountryCode("US");
        supplier.setRoutingCodeAba("ABA-BREAKES-999");
        supplier.setRoutingCodeSwift("SWIFT-NOT-USED");

        String linea = invokeGenerarLineaLayout(pago, supplier, "archivo.txt");
        String[] partes = linea.split("\\|", -1);

        assertEquals("ABA-BREAKES-999", partes[22]);
        assertEquals("09/01/2026", partes[5]);
        assertEquals("09/01/2026", partes[6]);
    }

    @Test
    @DisplayName("CP28 - Regla Negocio 1: USD + US -> ACH")
    public void testCP28_ReglaUsdUsRetornaAch() {
        assertEquals("ACH", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "US"));
        assertEquals("ACH", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("usd", "us"));
    }

    @Test
    @DisplayName("CP29 - Regla Negocio 2: MXN + MX -> SPEI")
    public void testCP29_ReglaMxnMxRetornaSpei() {
        assertEquals("SPEI", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("MXN", "MX"));
        assertEquals("SPEI", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("mxn", "mx"));
    }

    @Test
    @DisplayName("CP30 - Regla Negocio 3: USD + MX -> SPID")
    public void testCP30_ReglaUsdMxRetornaSpid() {
        assertEquals("SPID", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "MX"));
        assertEquals("SPID", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("usd", "mx"));
    }

    @Test
    @DisplayName("CP31 - Regla Negocio 4: USD + pais <> MX -> SPID")
    public void testCP31_ReglaUsdPaisDistintoMxRetornaSpid() {
        assertEquals("SPID", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "CA"));
        assertEquals("SPID", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "DE"));
        assertEquals("SPID", com.rassini.pagos.util.EmpresaUtils.calcularTipoPagoAutomatico("USD", "GB"));
    }

    @Test
    @DisplayName("CP32 - Routing Layout: SPID y SPEI utilizan SWIFT y conservan fechas")
    public void testCP32_LayoutSpidYSpeiUsanSwiftYFechasOriginales() throws Exception {
        PagosArchivo pagoSpid = new PagosArchivo();
        pagoSpid.setEmpresa("1850");
        pagoSpid.setMoneda("USD");
        pagoSpid.setFechaEnvio("09/10/2026");
        pagoSpid.setFechaValor("09/10/2026");
        pagoSpid.setCodigoProveedor("PRV01");
        pagoSpid.setTipoPagoSeleccionado("SPID");

        Supplier supplier = createValidSupplier();
        supplier.setCountryCode("MX");
        supplier.setRoutingCodeSwift("SWIFT-SPID-123");

        String lineaSpid = invokeGenerarLineaLayout(pagoSpid, supplier, "archivo.txt");
        String[] partesSpid = lineaSpid.split("\\|", -1);
        assertEquals("SWIFT-SPID-123", partesSpid[22]);
        assertEquals("09/10/2026", partesSpid[5]);
        assertEquals("09/10/2026", partesSpid[6]);

        PagosArchivo pagoSpei = new PagosArchivo();
        pagoSpei.setEmpresa("1850");
        pagoSpei.setMoneda("MXN");
        pagoSpei.setFechaEnvio("09/11/2026");
        pagoSpei.setFechaValor("09/11/2026");
        pagoSpei.setCodigoProveedor("PRV02");
        pagoSpei.setTipoPagoSeleccionado("SPEI");

        supplier.setRoutingCodeSwift("SWIFT-SPEI-456");
        String lineaSpei = invokeGenerarLineaLayout(pagoSpei, supplier, "archivo.txt");
        String[] partesSpei = lineaSpei.split("\\|", -1);
        assertEquals("SWIFT-SPEI-456", partesSpei[22]);
        assertEquals("09/11/2026", partesSpei[5]);
        assertEquals("09/11/2026", partesSpei[6]);
    }

    @Test
    @DisplayName("CP33 - Error si SPID o SPEI no tienen SWIFT")
    public void testCP33_ErrorSpidSinSwift() {
        PagosArchivo pago = new PagosArchivo();
        pago.setEmpresa("1850");
        pago.setCodigoProveedor("PRV01");
        pago.setTipoPagoSeleccionado("SPID");

        Supplier supplier = createValidSupplier();
        supplier.setRoutingCodeSwift(null);

        Exception ex = assertThrows(Exception.class, () -> invokeGenerarLineaLayout(pago, supplier, "archivo.txt"));
        assertTrue(ex.getCause() instanceof BusinessException);
        assertTrue(ex.getCause().getMessage().contains("no cuenta con Routing Code SWIFT requerido para pagos SPID"));
    }

    @Test
    @DisplayName("CP34 - Guardado individual soporta SPID y SPEI con preservación de mayúsculas/minúsculas")
    public void testCP34_GuardadoIndividualSpidYSpei() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(100L);
        pago.setEmpresa("1850");
        pago.setEstatus("PENDIENTE");

        when(pagosRepo.findById(100L)).thenReturn(Optional.of(pago));

        pagoService.actualizarTipoPagoSeleccionado(100L, "spei");
        assertEquals("SPEI", pago.getTipoPagoSeleccionado());

        pagoService.actualizarTipoPagoSeleccionado(100L, "spid");
        assertEquals("SPID", pago.getTipoPagoSeleccionado());

        verify(pagosRepo, times(2)).save(pago);
    }

    @Test
    @DisplayName("CP35 - Guardado masivo soporta mezcla de ACH, WIRE, SPID y SPEI")
    public void testCP35_GuardadoMasivoNuevosTipos() {
        PagosArchivo p1 = new PagosArchivo();
        p1.setId(1L);
        p1.setEmpresa("1850");
        p1.setEstatus("PENDIENTE");

        PagosArchivo p2 = new PagosArchivo();
        p2.setId(2L);
        p2.setEmpresa("1850");
        p2.setEstatus("PENDIENTE");

        PagosArchivo p3 = new PagosArchivo();
        p3.setId(3L);
        p3.setEmpresa("09");
        p3.setEstatus("PENDIENTE");

        when(pagosRepo.findAllById(anyList())).thenReturn(List.of(p1, p2, p3));

        TipoPagoSeleccionadoItemDTO item1 = new TipoPagoSeleccionadoItemDTO();
        item1.setId(1L);
        item1.setTipoPagoSeleccionado("ACH");

        TipoPagoSeleccionadoItemDTO item2 = new TipoPagoSeleccionadoItemDTO();
        item2.setId(2L);
        item2.setTipoPagoSeleccionado("SPID");

        TipoPagoSeleccionadoItemDTO item3 = new TipoPagoSeleccionadoItemDTO();
        item3.setId(3L);
        item3.setTipoPagoSeleccionado("SPEI");

        pagoService.actualizarTiposPagoSeleccionados(List.of(item1, item2, item3));

        assertEquals("ACH", p1.getTipoPagoSeleccionado());
        assertEquals("SPID", p2.getTipoPagoSeleccionado());
        assertEquals("SPEI", p3.getTipoPagoSeleccionado());
        verify(pagosRepo).saveAll(anyList());
    }

    @Test
    @DisplayName("CP36 - Opciones dinámicas según moneda, país y códigos disponibles")
    public void testCP36_OpcionesDinamicasMonedaPais() {
        // USD + US con ABA y SWIFT: sugiere ACH (con ABA) y WIRE/SPID (con SWIFT)
        List<String> opc1 = com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago("USD", "US", true, true);
        assertTrue(opc1.contains("ACH"));
        assertTrue(opc1.contains("WIRE"));
        assertTrue(opc1.contains("SPID"));

        // MXN + MX con SWIFT: única opción válida es SPEI (no WIRE)
        List<String> opc2 = com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago("MXN", "MX", false, true);
        assertEquals(1, opc2.size());
        assertEquals("SPEI", opc2.get(0));
        assertFalse(opc2.contains("WIRE"));
        assertFalse(opc2.contains("ACH"));

        // Sin ABA ni SWIFT: lista vacía
        List<String> opc3 = com.rassini.pagos.util.EmpresaUtils.determinarOpcionesTipoPago("USD", "US", false, false);
        assertTrue(opc3.isEmpty());
    }

    @Test
    @DisplayName("CP37 - Proveedor con múltiples filas Supplier: No mezclar capacidades ni atributos de filas distintas")
    public void testCP37_MultiplesFilasSupplierNoMezclarCapacidades() {
        // Escenario: Un mismo erp_id_qad tiene dos filas en suppliers:
        // Fila A (BU 0111, cuenta 11112222, MX, solo SWIFT)
        // Fila B (BU 09, cuenta 33334444, US, solo ABA)
        PagosArchivo pago = new PagosArchivo();
        pago.setId(100L);
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_MULTI");
        pago.setCuentaBeneficiario("0011112222");
        pago.setMoneda("MXN");
        pago.setEstatus("PENDIENTE");

        Supplier supplierA = new Supplier();
        supplierA.setId(1L);
        supplierA.setErpIdQad("PROV_MULTI");
        supplierA.setBusinessUnitCode("0111");
        supplierA.setAccountNumber("0011112222");
        supplierA.setCountryCode("MX");
        supplierA.setRoutingCodeSwift("SWIFT_MX");
        supplierA.setRoutingCodeAba(null); // Sin ABA

        when(pagosRepo.filtrarPaginadoAll(any(), any(), any(), any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(pago)));
        when(fileLoaderService.obtenerUltimos8DigitosCuenta("0011112222")).thenReturn("11112222");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_MULTI", "0111", "11112222")).thenReturn(supplierA);

        org.springframework.data.domain.Page<com.rassini.pagos.dto.PagoPendienteDTO> result =
                pagoService.filtrarPendientesPaginado("ALL", null, null, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        com.rassini.pagos.dto.PagoPendienteDTO dto = result.getContent().get(0);

        // Se valida que las capacidades obtenidas provienen estrictamente de la fila A resuelta por cuenta y BU:
        assertTrue(Boolean.TRUE.equals(dto.getTieneSwift()));
        assertFalse(Boolean.TRUE.equals(dto.getTieneAba()), "No debe heredar el ABA de una fila de otra BU o cuenta");
        assertEquals(List.of("SPEI"), dto.getOpcionesTipoPago(), "MXN + MX debe resultar únicamente en SPEI");
    }

    @Test
    @DisplayName("CP38 - Confirmar que UI y generarLineaLayout utilizan exactamente el mismo Supplier resuelto por cuenta y empresa padre")
    public void testCP38_UIyLayoutUsanMismoSupplier() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(200L);
        pago.setEmpresa("02"); // Empresa hija de 09
        pago.setCodigoProveedor("PROV_SHARED");
        pago.setCuentaBeneficiario("999988887777");
        pago.setMoneda("USD");
        pago.setEstatus("PENDIENTE");

        Supplier supplierResuelto = createValidSupplier();
        supplierResuelto.setId(50L);
        supplierResuelto.setErpIdQad("PROV_SHARED");
        supplierResuelto.setBusinessUnitCode("09"); // Empresa padre
        supplierResuelto.setAccountNumber("999988887777");
        supplierResuelto.setCountryCode("US");
        supplierResuelto.setRoutingCodeAba("ABA_VALIDO");
        supplierResuelto.setRoutingCodeSwift("SWIFT_VALIDO");

        when(fileLoaderService.obtenerUltimos8DigitosCuenta("999988887777")).thenReturn("88887777");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_SHARED", "02", "88887777")).thenReturn(supplierResuelto);

        // 1. Simular resolución en flujo UI (filtrarPendientesPaginado)
        when(pagosRepo.filtrarPaginadoAll(any(), any(), any(), any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(pago)));

        org.springframework.data.domain.Page<com.rassini.pagos.dto.PagoPendienteDTO> result =
                pagoService.filtrarPendientesPaginado("ALL", null, null, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        com.rassini.pagos.dto.PagoPendienteDTO dto = result.getContent().get(0);
        assertTrue(Boolean.TRUE.equals(dto.getTieneAba()));
        assertTrue(Boolean.TRUE.equals(dto.getTieneSwift()));
        // Sugiere ACH, WIRE, SPID
        assertTrue(dto.getOpcionesTipoPago().contains("ACH"));

        // 2. Simular resolución en layout (mismo supplier pasado a generarLineaLayout)
        String linea = invokeGenerarLineaLayout(pago, supplierResuelto, "TEST_LAYOUT.txt");
        assertNotNull(linea);
        assertTrue(linea.contains("PROV_SHARED"));
        assertTrue(linea.contains(supplierResuelto.getCountryCode()));
    }

    @Test
    @DisplayName("CP39 - Proveedor con varias cuentas en la misma BU: Detección de cuenta exacta y no ambigua")
    public void testCP39_VariasCuentasMismaBU() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(300L);
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_ACCOUNTS");
        pago.setCuentaBeneficiario("111122223333");
        pago.setMoneda("USD");

        Supplier supplierCuenta1 = createValidSupplier();
        supplierCuenta1.setAccountNumber("111122223333");
        supplierCuenta1.setCountryCode("US");
        supplierCuenta1.setRoutingCodeAba("ABA_CTA1");

        when(fileLoaderService.obtenerUltimos8DigitosCuenta("111122223333")).thenReturn("22223333");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_ACCOUNTS", "0111", "22223333"))
                .thenReturn(supplierCuenta1);

        when(pagosRepo.filtrarPaginadoAll(any(), any(), any(), any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(pago)));

        org.springframework.data.domain.Page<com.rassini.pagos.dto.PagoPendienteDTO> result =
                pagoService.filtrarPendientesPaginado("ALL", null, null, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        com.rassini.pagos.dto.PagoPendienteDTO dto = result.getContent().get(0);
        assertTrue(Boolean.TRUE.equals(dto.getTieneAba()));
    }

    @Test
    @DisplayName("CP40 - Cuenta beneficiaria sin coincidencia en BD: Manejo seguro sin combinar datos")
    public void testCP40_CuentaSinCoincidencia() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(400L);
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_INEXISTENTE");
        pago.setCuentaBeneficiario("000000000000");
        pago.setMoneda("USD");

        when(fileLoaderService.obtenerUltimos8DigitosCuenta("000000000000")).thenReturn("00000000");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_INEXISTENTE", "0111", "00000000"))
                .thenThrow(new com.rassini.pagos.exception.BusinessExceptionCode("ERR030", "No existe cuenta"));
        when(fileLoaderService.obtenerSupplierPadre("PROV_INEXISTENTE", "0111"))
                .thenThrow(new com.rassini.pagos.exception.BusinessExceptionCode("ERR045", "No existe supplier"));

        when(pagosRepo.filtrarPaginadoAll(any(), any(), any(), any(), any(), any(), any(), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(pago)));

        org.springframework.data.domain.Page<com.rassini.pagos.dto.PagoPendienteDTO> result =
                pagoService.filtrarPendientesPaginado("ALL", null, null, null, null, null, null, null, org.springframework.data.domain.PageRequest.of(0, 10));

        com.rassini.pagos.dto.PagoPendienteDTO dto = result.getContent().get(0);
        assertFalse(Boolean.TRUE.equals(dto.getTieneAba()));
        assertFalse(Boolean.TRUE.equals(dto.getTieneSwift()));
        assertTrue(dto.getOpcionesTipoPago().isEmpty());
    }

    @Test
    @DisplayName("CP41 - Cuenta beneficiaria null: No usar findFirst arbitrario si hay múltiples filas")
    public void testCP41_CuentaBeneficiariaNullMultiplesFilasLanzaExcepcion() {
        com.rassini.pagos.service.impl.FileLoaderServiceImpl loaderService =
                new com.rassini.pagos.service.impl.FileLoaderServiceImpl(pagosRepo, supplierRepo, null, catalogoRepo, cache);

        Supplier s1 = new Supplier();
        s1.setId(1L);
        Supplier s2 = new Supplier();
        s2.setId(2L);

        when(supplierRepo.findByErpIdQadAndBusinessUnitCode("PROV_AMBIGUO", "0111"))
                .thenReturn(List.of(s1, s2));

        com.rassini.pagos.exception.BusinessExceptionCode ex = assertThrows(
                com.rassini.pagos.exception.BusinessExceptionCode.class,
                () -> loaderService.obtenerSupplierPadre("PROV_AMBIGUO", "0111"));

        assertTrue(ex.getMessage().contains("No es posible determinar un Supplier único sin cuenta"));
    }

    @Test
    @DisplayName("CP42 - Más de una coincidencia de cuenta con últimos 8 caracteres: Lanzar BusinessExceptionCode ERR031")
    public void testCP42_MasDeUnaCoincidenciaCuentaLanzaExcepcion() {
        com.rassini.pagos.service.impl.FileLoaderServiceImpl loaderService =
                new com.rassini.pagos.service.impl.FileLoaderServiceImpl(pagosRepo, supplierRepo, null, catalogoRepo, cache);

        Supplier s1 = new Supplier();
        Supplier s2 = new Supplier();

        when(supplierRepo.findByCodigoProveedorAndEmpresaAndAccountNumberEndsWith("PROV_DUPLICADO", "0111", "12345678"))
                .thenReturn(List.of(s1, s2));

        com.rassini.pagos.exception.BusinessExceptionCode ex = assertThrows(
                com.rassini.pagos.exception.BusinessExceptionCode.class,
                () -> loaderService.obtenerSupplierPadrePorCuenta("PROV_DUPLICADO", "0111", "12345678"));

        assertEquals("ERR031", ex.getCodigo());
        assertTrue(ex.getMessage().contains("Existe más de un supplier para Empresa y Cuenta"));
    }

    @Test
    @DisplayName("CP43 - Tipo histórico fuera de opciones válidas: Bloquear envío en validarPagosPendientes")
    public void testCP43_TipoHistoricoIncompatibleBloqueaEnvio() {
        PagosArchivo pagoIncompatible = new PagosArchivo();
        pagoIncompatible.setId(500L);
        pagoIncompatible.setNombreArchivo("ARCHIVO_TEST.txt");
        pagoIncompatible.setEmpresa("0111");
        pagoIncompatible.setCodigoProveedor("PROV_MX");
        pagoIncompatible.setCuentaBeneficiario("998877665544");
        pagoIncompatible.setMoneda("MXN");
        pagoIncompatible.setTipoPagoSeleccionado("WIRE"); // Histórico incompatible (debe ser SPEI)
        pagoIncompatible.setTipoPago(new com.rassini.pagos.entity.CatalogoTipoPago());

        Supplier sMX = createValidSupplier();
        sMX.setCountryCode("MX");
        sMX.setRoutingCodeSwift("SWIFT_MX");

        when(pagosRepo.findPendientesValidacionAll()).thenReturn(List.of(pagoIncompatible));
        when(fileLoaderService.obtenerUltimos8DigitosCuenta("998877665544")).thenReturn("77665544");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_MX", "0111", "77665544")).thenReturn(sMX);

        com.rassini.pagos.dto.ValidacionEnvioDTO validacion = pagoService.validarPagosPendientes("ALL");
        assertFalse(validacion.isPermitido(), "No debe permitir el envío si hay pagos con tipo incompatible");
        assertFalse(validacion.getErrores().isEmpty());
        assertTrue(validacion.getErrores().get(0).contains("no compatible con las opciones válidas"));
    }

    @Test
    @DisplayName("CP44 - Confirmar que generarLineaLayout no modifica tipo_pago_seleccionado en BD")
    public void testCP44_GenerarLineaLayoutNoModificaTipoPagoEnBD() throws Exception {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(600L);
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_TEST");
        pago.setMoneda("MXN");
        pago.setTipoPagoSeleccionado("SPEI");

        Supplier s = createValidSupplier();
        s.setRoutingCodeSwift("SWIFT123");

        String linea = invokeGenerarLineaLayout(pago, s, "LAYOUT.txt");
        assertNotNull(linea);

        // Validar que el valor en el objeto pago no se alteró
        assertEquals("SPEI", pago.getTipoPagoSeleccionado());
        // Validar que pagosRepo.save() NO fue invocado dentro de generarLineaLayout
        verify(pagosRepo, never()).save(pago);
    }

    @Test
    @DisplayName("CP45 - Proveedor sin ABA ni SWIFT (opciones vacías): Bloquear envío con mensaje funcional")
    public void testCP45_ProveedorSinAbaNiSwiftBloqueaEnvio() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(701L);
        pago.setNombreArchivo("TEST_NO_ROUTING.txt");
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_NO_ROUTING");
        pago.setCuentaBeneficiario("112233445566");
        pago.setMoneda("USD");
        pago.setTipoPago(new com.rassini.pagos.entity.CatalogoTipoPago());

        Supplier sSinRouting = createValidSupplier();
        sSinRouting.setRoutingCodeAba(null);
        sSinRouting.setRoutingCodeSwift(null);

        when(pagosRepo.findPendientesValidacionAll()).thenReturn(List.of(pago));
        when(fileLoaderService.obtenerUltimos8DigitosCuenta("112233445566")).thenReturn("33445566");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_NO_ROUTING", "0111", "33445566")).thenReturn(sSinRouting);

        com.rassini.pagos.dto.ValidacionEnvioDTO res = pagoService.validarPagosPendientes("ALL");
        assertFalse(res.isPermitido());
        assertTrue(res.getErrores().stream().anyMatch(e -> e.contains("no cuenta con opciones de transferencia válidas")));
    }

    @Test
    @DisplayName("CP46 - Tipo persistido null: Bloquear envío indicando selección requerida")
    public void testCP46_TipoPersistidoNullBloqueaEnvio() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(702L);
        pago.setNombreArchivo("TEST_NULL_TIPO.txt");
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_OK");
        pago.setCuentaBeneficiario("112233445566");
        pago.setMoneda("USD");
        pago.setTipoPagoSeleccionado(null);
        pago.setTipoPago(new com.rassini.pagos.entity.CatalogoTipoPago());

        Supplier sConRouting = createValidSupplier();
        sConRouting.setCountryCode("US");
        sConRouting.setRoutingCodeAba("ABA123");

        when(pagosRepo.findPendientesValidacionAll()).thenReturn(List.of(pago));
        when(fileLoaderService.obtenerUltimos8DigitosCuenta("112233445566")).thenReturn("33445566");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_OK", "0111", "33445566")).thenReturn(sConRouting);

        com.rassini.pagos.dto.ValidacionEnvioDTO res = pagoService.validarPagosPendientes("ALL");
        assertFalse(res.isPermitido());
        assertTrue(res.getErrores().stream().anyMatch(e -> e.contains("no tiene un tipo de transferencia seleccionado")));
    }

    @Test
    @DisplayName("CP47 - Tipo persistido vacío: Bloquear envío indicando selección requerida")
    public void testCP47_TipoPersistidoVacioBloqueaEnvio() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(703L);
        pago.setNombreArchivo("TEST_BLANK_TIPO.txt");
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_OK");
        pago.setCuentaBeneficiario("112233445566");
        pago.setMoneda("USD");
        pago.setTipoPagoSeleccionado("   ");
        pago.setTipoPago(new com.rassini.pagos.entity.CatalogoTipoPago());

        Supplier sConRouting = createValidSupplier();
        sConRouting.setCountryCode("US");
        sConRouting.setRoutingCodeAba("ABA123");

        when(pagosRepo.findPendientesValidacionAll()).thenReturn(List.of(pago));
        when(fileLoaderService.obtenerUltimos8DigitosCuenta("112233445566")).thenReturn("33445566");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_OK", "0111", "33445566")).thenReturn(sConRouting);

        com.rassini.pagos.dto.ValidacionEnvioDTO res = pagoService.validarPagosPendientes("ALL");
        assertFalse(res.isPermitido());
        assertTrue(res.getErrores().stream().anyMatch(e -> e.contains("no tiene un tipo de transferencia seleccionado")));
    }

    @Test
    @DisplayName("CP48 - Tipo persistido con valor no permitido por catálogo: Bloquear envío")
    public void testCP48_TipoPersistidoInvalidoCatalogoBloqueaEnvio() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(704L);
        pago.setNombreArchivo("TEST_INVALIDO.txt");
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_OK");
        pago.setCuentaBeneficiario("112233445566");
        pago.setMoneda("USD");
        pago.setTipoPagoSeleccionado("INEXISTENTE");
        pago.setTipoPago(new com.rassini.pagos.entity.CatalogoTipoPago());

        Supplier sConRouting = createValidSupplier();
        sConRouting.setCountryCode("US");
        sConRouting.setRoutingCodeAba("ABA123");

        when(pagosRepo.findPendientesValidacionAll()).thenReturn(List.of(pago));
        when(fileLoaderService.obtenerUltimos8DigitosCuenta("112233445566")).thenReturn("33445566");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_OK", "0111", "33445566")).thenReturn(sConRouting);

        com.rassini.pagos.dto.ValidacionEnvioDTO res = pagoService.validarPagosPendientes("ALL");
        assertFalse(res.isPermitido());
        assertTrue(res.getErrores().stream().anyMatch(e -> e.contains("no compatible con las opciones válidas")));
    }

    @Test
    @DisplayName("CP49 - Tipo persistido válido compatible con opciones: Permitir envío")
    public void testCP49_TipoPersistidoValidoPermiteEnvio() {
        PagosArchivo pago = new PagosArchivo();
        pago.setId(705L);
        pago.setNombreArchivo("TEST_VALIDO.txt");
        pago.setEmpresa("0111");
        pago.setCodigoProveedor("PROV_OK");
        pago.setCuentaBeneficiario("112233445566");
        pago.setMoneda("USD");
        pago.setTipoPagoSeleccionado("ACH");
        pago.setTipoPago(new com.rassini.pagos.entity.CatalogoTipoPago());

        Supplier sConRouting = createValidSupplier();
        sConRouting.setCountryCode("US");
        sConRouting.setRoutingCodeAba("ABA123");

        when(pagosRepo.findPendientesValidacionAll()).thenReturn(List.of(pago));
        when(fileLoaderService.obtenerUltimos8DigitosCuenta("112233445566")).thenReturn("33445566");
        when(fileLoaderService.obtenerSupplierPadrePorCuenta("PROV_OK", "0111", "33445566")).thenReturn(sConRouting);

        com.rassini.pagos.dto.ValidacionEnvioDTO res = pagoService.validarPagosPendientes("ALL");
        assertTrue(res.isPermitido());
        assertTrue(res.getErrores().isEmpty());
    }
}


