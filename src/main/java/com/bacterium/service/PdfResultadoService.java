package com.bacterium.service;

import com.bacterium.model.Medico;
import com.bacterium.model.Orden;
import com.bacterium.model.Paciente;
import com.bacterium.model.Resultado;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PdfResultadoService {

    private static final DeviceRgb AZUL_CORP  = new DeviceRgb(26,  79, 163);
    private static final DeviceRgb AZUL_CLARO = new DeviceRgb(232, 240, 251);
    private static final DeviceRgb ROJO       = new DeviceRgb(185,  28,  28);
    private static final DeviceRgb AZUL_L     = new DeviceRgb(29,  78, 216);
    private static final DeviceRgb GRIS_PIE   = new DeviceRgb(107, 106, 101);

    private static final DateTimeFormatter FMT_LARGO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter FMT_CORTO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ConfiguracionService configuracionService;

    @Transactional(readOnly = true)
    public byte[] generarPdf(Orden orden) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
             Document doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(30, 28, 28, 28);
            PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);
            agregarEncabezado(doc, orden, bold, regular, italic);
            agregarDatosPaciente(doc, orden, bold, regular);
            agregarResultados(doc, orden, bold, regular);
            agregarObservaciones(doc, orden, regular, italic);
            if (bool(configuracionService.obtenerConfiguracionLaboratorio(), "pdf.mostrarFirmas")) {
                agregarFirmas(doc, bold, regular);
            }
            agregarPie(doc, regular, italic);
        }
        return baos.toByteArray();
    }

    private void agregarEncabezado(Document doc, Orden orden, PdfFont bold, PdfFont regular, PdfFont italic) {
        Map<String, String> cfg = configuracionService.obtenerConfiguracionLaboratorio();
        Table header = new Table(UnitValue.createPercentArray(new float[]{18, 47, 35}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(6);

        Cell logoCell = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        Image logo = bool(cfg, "pdf.mostrarLogo") ? crearLogo(cfg.get(ConfiguracionService.LOGO_CLAVE)) : null;
        if (logo != null) {
            logo.setAutoScale(true);
            logo.setMaxWidth(76);
            logo.setMaxHeight(54);
            logoCell.add(logo);
        }
        header.addCell(logoCell);

        Cell izq = new Cell().setBorder(Border.NO_BORDER).setPadding(4);
        izq.add(new Paragraph(valor(cfg, "lab.nombre")).setFont(bold).setFontSize(15).setFontColor(AZUL_CORP).setMarginBottom(2));
        izq.add(new Paragraph("Analisis Clinicos  *  Microbiologia  *  Urologia  *  Hematologia")
                .setFont(italic).setFontSize(8.5f).setFontColor(GRIS_PIE).setMarginBottom(3));
        if (bool(cfg, "pdf.mostrarContacto")) {
            izq.add(new Paragraph("Dir.: " + valor(cfg, "lab.direccion") + "   Tel.: " + valor(cfg, "lab.telefono"))
                    .setFont(regular).setFontSize(8).setFontColor(GRIS_PIE));
            izq.add(new Paragraph("Email: " + valor(cfg, "lab.email") + "   Web: " + valor(cfg, "lab.web"))
                    .setFont(regular).setFontSize(8).setFontColor(GRIS_PIE));
        }
        izq.add(new Paragraph(valor(cfg, "lab.desde") + "  *  Acreditado")
                .setFont(italic).setFontSize(7.5f).setFontColor(GRIS_PIE).setMarginTop(2));
        header.addCell(izq);

        Cell der = new Cell().setBorder(new SolidBorder(AZUL_CORP, 1.2f))
                .setPadding(8).setTextAlignment(TextAlignment.RIGHT);
        der.add(new Paragraph("No. Orden").setFont(bold).setFontSize(8).setFontColor(GRIS_PIE).setMarginBottom(1));
        der.add(new Paragraph(nulo(orden.getNumeroOrden())).setFont(bold).setFontSize(14).setFontColor(AZUL_CORP).setMarginBottom(5));
        der.add(new Paragraph("Recepcion:").setFont(bold).setFontSize(8).setFontColor(GRIS_PIE));
        der.add(new Paragraph(orden.getFechaRecepcion() != null ? orden.getFechaRecepcion().format(FMT_LARGO) : "—")
                .setFont(regular).setFontSize(8).setMarginBottom(4));
        der.add(new Paragraph("Entrega:").setFont(bold).setFontSize(8).setFontColor(GRIS_PIE));
        der.add(new Paragraph(orden.getFechaEntrega() != null ? orden.getFechaEntrega().format(FMT_CORTO) : "Pendiente")
                .setFont(regular).setFontSize(8));
        header.addCell(der);
        doc.add(header);

        // Línea azul
        Table linea = new Table(1).setWidth(UnitValue.createPercentValue(100)).setMarginBottom(8);
        linea.addCell(new Cell().setHeight(2).setBackgroundColor(AZUL_CORP).setBorder(Border.NO_BORDER));
        doc.add(linea);
    }

    private void agregarDatosPaciente(Document doc, Orden orden, PdfFont bold, PdfFont regular) {
        Map<String, String> cfg = configuracionService.obtenerConfiguracionLaboratorio();
        Paciente p = orden.getPaciente();
        Medico   m = orden.getMedicoReferente();
        DeviceRgb bgSec = new DeviceRgb(248, 249, 255);
        DeviceRgb borSec = new DeviceRgb(200, 200, 200);

        Table datos = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginBottom(10)
                .setBorder(new SolidBorder(borSec, 0.5f));

        Cell colPac = new Cell().setPadding(8).setBackgroundColor(bgSec)
                .setBorderRight(new SolidBorder(borSec, 0.5f))
                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderBottom(Border.NO_BORDER);
        colPac.add(campo("Paciente:     ", p.getNombreCompleto(), bold, regular));
        colPac.add(campo("Edad:          ", calcularEdad(p, orden), bold, regular));
        colPac.add(campo("Sexo:          ", p.getSexo() != null ? p.getSexo().name() : "—", bold, regular));
        colPac.add(campo("Codigo:       ", nulo(p.getCodigoPaciente()), bold, regular));
        datos.addCell(colPac);

        Cell colMed = new Cell().setPadding(8).setBackgroundColor(bgSec)
                .setBorder(Border.NO_BORDER);
        if (bool(cfg, "pdf.mostrarMedico")) {
            colMed.add(campo("Medico:         ", m != null ? m.getNombreCompleto() : "—", bold, regular));
            colMed.add(campo("Especialidad:", m != null ? nulo(m.getEspecialidad()) : "—", bold, regular));
        }
        colMed.add(campo("Tipo muestra:", nulo(orden.getTipoMuestra()), bold, regular));
        if (bool(cfg, "pdf.mostrarObservaciones")) {
            colMed.add(campo("Observaciones:", nulo(orden.getObservaciones()), bold, regular));
        }
        datos.addCell(colMed);
        doc.add(datos);
    }

    private void agregarResultados(Document doc, Orden orden, PdfFont bold, PdfFont regular) {
        Map<String, String> cfg = configuracionService.obtenerConfiguracionLaboratorio();
        boolean mostrarReferencias = bool(cfg, "pdf.mostrarReferencias");
        boolean soloConResultado = bool(cfg, "pdf.soloConResultado");
        List<Resultado> resultados = orden.getResultados() == null ? List.of() : orden.getResultados();

        var porArea = resultados.stream()
                .filter(Objects::nonNull)
                .filter(r -> r.getPrueba() != null)
                .filter(r -> !soloConResultado || (r.getValor() != null && !r.getValor().isBlank()))
                .sorted(Comparator.comparing(r -> r.getPrueba().getOrden()))
                .collect(Collectors.groupingBy(r -> r.getPrueba().getArea(), LinkedHashMap::new, Collectors.toList()));

        for (var area : porArea.entrySet()) {
            doc.add(new Paragraph(area.getKey()).setFont(bold).setFontSize(11)
                    .setFontColor(ColorConstants.WHITE).setTextAlignment(TextAlignment.CENTER)
                    .setBackgroundColor(AZUL_CORP).setPadding(4).setMarginTop(6).setMarginBottom(0));

            Table tabla = new Table(UnitValue.createPercentArray(
                    mostrarReferencias ? new float[]{32, 20, 9, 14, 25} : new float[]{42, 25, 10, 23}))
                    .setWidth(UnitValue.createPercentValue(100)).setFontSize(9.5f);

            List<String> columnas = mostrarReferencias
                    ? List.of("Prueba","Resultado","Alarma","Unidades","Intervalo de referencia")
                    : List.of("Prueba","Resultado","Alarma","Unidades");
            for (String col : columnas) {
                tabla.addHeaderCell(new Cell()
                        .add(new Paragraph(col).setFont(bold).setFontSize(9))
                        .setBackgroundColor(AZUL_CLARO)
                        .setBorderTop(new SolidBorder(AZUL_CORP, 0.5f))
                        .setBorderBottom(new SolidBorder(AZUL_CORP, 0.5f))
                        .setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                        .setPadding(3));
            }

            int fila = 0;
            for (Resultado res : area.getValue()) {
                Color bg = (fila % 2 == 0) ? ColorConstants.WHITE : new DeviceRgb(245, 247, 252);
                tabla.addCell(celda(res.getPrueba().getNombre(), bold, TextAlignment.LEFT, bg, ColorConstants.BLACK));

                Color colorVal = res.isAlarma() ? ROJO : ColorConstants.BLACK;
                tabla.addCell(celda(nulo(res.getValor()), bold, TextAlignment.LEFT, bg, colorVal));

                String alarmaText = "";
                Color colorAlarma = ColorConstants.BLACK;
                if (res.isAlarma()) {
                    alarmaText  = determinarTipo(res);
                    colorAlarma = alarmaText.equals("L") ? AZUL_L : ROJO;
                }
                tabla.addCell(celda(alarmaText, bold, TextAlignment.CENTER, bg, colorAlarma));
                tabla.addCell(celda(nulo(res.getPrueba().getUnidades()), regular, TextAlignment.CENTER, bg, ColorConstants.BLACK));
                if (mostrarReferencias) {
                    tabla.addCell(celda(nulo(res.getPrueba().getIntervaloReferencia()), regular, TextAlignment.LEFT, bg, GRIS_PIE));
                }
                fila++;
            }
            doc.add(tabla);
        }
    }

    private void agregarObservaciones(Document doc, Orden orden, PdfFont regular, PdfFont italic) {
        Map<String, String> cfg = configuracionService.obtenerConfiguracionLaboratorio();
        if (!bool(cfg, "pdf.mostrarObservaciones")) {
            return;
        }
        String obs = orden.getObservaciones();
        if (obs != null && !obs.isBlank()) {
            doc.add(new Paragraph("Observaciones / Comentarios:")
                    .setFont(regular).setFontSize(8.5f).setFontColor(AZUL_CORP).setMarginTop(10).setMarginBottom(2));
            doc.add(new Paragraph(obs)
                    .setFont(italic).setFontSize(8.5f).setFontColor(GRIS_PIE).setPaddingLeft(8));
        }

        // Leyenda
        doc.add(new Paragraph("\nLeyenda: H = Valor Alto (referencia superada)   L = Valor Bajo   * = Fuera de rango")
                .setFont(italic).setFontSize(7.5f).setFontColor(GRIS_PIE).setMarginTop(6));
    }

    private void agregarFirmas(Document doc, PdfFont bold, PdfFont regular) {
        Map<String, String> cfg = configuracionService.obtenerConfiguracionLaboratorio();
        String labCedula = valor(cfg, "lab.cedula");
        String labDirector = valor(cfg, "lab.director");
        doc.add(new Paragraph(" ").setFontSize(16));
        Table firmas = new Table(UnitValue.createPercentArray(new float[]{33, 33, 34}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(8);
        firmas.addCell(firma("Analista Responsable", "Firma y Sello", bold, regular));
        firmas.addCell(firma("Q.F.B. / Responsable de Laboratorio",
                "Ced. Prof.: " + (labCedula.isBlank() ? "___________" : labCedula), bold, regular));
        firmas.addCell(firma(labDirector, "Director Tecnico / Autorizado", bold, regular));
        doc.add(firmas);
    }

    private void agregarPie(Document doc, PdfFont regular, PdfFont italic) {
        Map<String, String> cfg = configuracionService.obtenerConfiguracionLaboratorio();
        Table linea = new Table(1).setWidth(UnitValue.createPercentValue(100)).setMarginTop(14);
        linea.addCell(new Cell().setHeight(1).setBackgroundColor(AZUL_CORP).setBorder(Border.NO_BORDER));
        doc.add(linea);

        Table pie = new Table(UnitValue.createPercentArray(new float[]{60, 40}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(4);
        pie.addCell(new Cell().setBorder(Border.NO_BORDER)
                .add(new Paragraph("Resultado confidencial. Consulte la interpretacion con su medico tratante.")
                        .setFont(italic).setFontSize(7.5f).setFontColor(GRIS_PIE)));
        pie.addCell(new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("Pagina 1 de 1   *   " + valor(cfg, "lab.nombre"))
                        .setFont(regular).setFontSize(7.5f).setFontColor(GRIS_PIE)));
        doc.add(pie);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private Paragraph campo(String label, String valor, PdfFont bold, PdfFont regular) {
        return new Paragraph()
                .add(new Text(label).setFont(bold).setFontSize(9))
                .add(new Text(nulo(valor)).setFont(regular).setFontSize(9))
                .setMarginBottom(2);
    }

    private Cell celda(String texto, PdfFont font, TextAlignment align, Color bg, Color color) {
        return new Cell()
                .add(new Paragraph(texto).setFont(font).setFontSize(9).setFontColor(color))
                .setBackgroundColor(bg)
                .setBorderTop(Border.NO_BORDER).setBorderLeft(Border.NO_BORDER).setBorderRight(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(new DeviceRgb(220, 220, 220), 0.3f))
                .setTextAlignment(align).setPadding(2.5f);
    }

    private Cell firma(String nombre, String titulo, PdfFont bold, PdfFont regular) {
        Cell c = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.CENTER).setPadding(6);
        c.add(new Paragraph(" ").setFontSize(18));
        c.add(new Paragraph("________________________________").setFont(regular).setFontSize(8).setFontColor(GRIS_PIE));
        c.add(new Paragraph(nombre).setFont(bold).setFontSize(8.5f));
        c.add(new Paragraph(titulo).setFont(regular).setFontSize(7.5f).setFontColor(GRIS_PIE));
        return c;
    }

    private String determinarTipo(Resultado res) {
        try {
            double val = Double.parseDouble(res.getValor().replace(",",".").trim());
            String ref = res.getPrueba().getIntervaloReferencia();
            if (ref != null && ref.contains("-")) {
                String[] p = ref.split("-");
                double min = Double.parseDouble(p[0].trim());
                double max = Double.parseDouble(p[1].trim());
                if (val < min) return "L";
                if (val > max) return "H";
            }
        } catch (Exception ignored) {}
        return "H";
    }

    private String calcularEdad(Paciente p, Orden orden) {
        if (p.getFechaNacimiento() == null) return "—";
        LocalDate ref = orden.getFechaRecepcion() != null ? orden.getFechaRecepcion().toLocalDate() : LocalDate.now();
        long anios = java.time.temporal.ChronoUnit.YEARS.between(p.getFechaNacimiento(), ref);
        if (anios == 0) {
            long meses = java.time.temporal.ChronoUnit.MONTHS.between(p.getFechaNacimiento(), ref);
            return meses + " mes(es)";
        }
        return anios + " año(s)";
    }

    private String nulo(String v) { return v == null || v.isBlank() ? "—" : v; }

    private String valor(Map<String, String> cfg, String clave) {
        return nulo(cfg.get(clave));
    }

    private Image crearLogo(String dataUrl) {
        if (dataUrl == null || dataUrl.isBlank() || !dataUrl.contains(",")) {
            return null;
        }
        try {
            String base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new Image(ImageDataFactory.create(bytes));
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean bool(Map<String, String> cfg, String clave) {
        return !"false".equalsIgnoreCase(cfg.getOrDefault(clave, "true"));
    }
}
