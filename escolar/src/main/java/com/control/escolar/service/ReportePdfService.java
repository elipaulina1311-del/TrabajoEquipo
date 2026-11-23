package com.control.escolar.service;

import com.control.escolar.dao.DocenteDAO;
import com.control.escolar.model.Alumno;
import com.control.escolar.model.Docente;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.List;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class ReportePdfService {

    private final DocenteDAO docenteDAO;

    private static final Font F_TITULO = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(27, 94, 32));
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font F_NEGRITA = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
    private static final Font F_NORMAL = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_HEADER = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final BaseColor VERDE_UAEM = new BaseColor(46, 125, 50);

    public ReportePdfService(DocenteDAO docenteDAO) {
        this.docenteDAO = docenteDAO;
    }

    // 1. Reporte Individual de Asignaciones Docentes
    public byte[] generarReporteDocenteIndividual(int idDocente) throws Exception {
        List<Docente> asignaciones = docenteDAO.buscarAsignacionesPorIdDocente(idDocente);
        if (asignaciones.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron asignaciones para el docente con ID " + idDocente + ".");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 48, 48);

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();
            agregarEncabezadoInstitucional(documento, "Reporte Individual de Asignaciones Docentes");

            Docente titular = asignaciones.get(0);
            documento.add(new Paragraph("Docente: " + titular.getNombreCompleto(), F_NEGRITA));
            documento.add(new Paragraph("ID Docente: " + titular.getId() + "  |  Número de Empleado: " + titular.getNumeroEmpleado(), F_NORMAL));
            documento.add(new Paragraph("\nMaterias y Grupos Impartidos:", F_NEGRITA));

            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(8f);
            tabla.setWidths(new float[]{4f, 4f, 2f});

            agregarCabeceras(tabla, new String[]{"Materia", "Carrera", "Grupo"});

            Iterator<Docente> it = asignaciones.iterator();
            while (it.hasNext()) {
                Docente d = it.next();
                agregarCelda(tabla, d.getMateria());
                agregarCelda(tabla, d.getCarrera());
                agregarCelda(tabla, d.getGrupo());
            }

            documento.add(tabla);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return out.toByteArray();
    }

    // 2. Reporte Grupal de Alumnos por Grupo y Materia
    public byte[] generarReporteAlumnosPorGrupoMateria(String grupo, String materia, Integer idDocenteFiltro) throws Exception {
        List<Alumno> alumnos = docenteDAO.listarAlumnosPorGrupoYMateria(grupo, materia, idDocenteFiltro);
        if (alumnos.isEmpty()) {
            throw new IllegalArgumentException(
                    "No se encontraron alumnos en el grupo " + grupo + " para la materia " + materia + ".");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 48, 48);

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();
            agregarEncabezadoInstitucional(documento, "Lista Grupal de Alumnos Inscritos");

            documento.add(new Paragraph("Grupo: " + grupo + "   |   Materia: " + materia, F_NEGRITA));
            documento.add(new Paragraph("Total de alumnos listados: " + alumnos.size() + "\n\n", F_NORMAL));

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{3f, 3f, 3f, 1.5f});

            agregarCabeceras(tabla, new String[]{"Nombre", "Apellido Paterno", "Apellido Materno", "Semestre"});

            Iterator<Alumno> it = alumnos.iterator();
            while (it.hasNext()) {
                Alumno a = it.next();
                agregarCelda(tabla, a.getNombre());
                agregarCelda(tabla, a.getApPaterno());
                agregarCelda(tabla, a.getApMaterno());
                agregarCelda(tabla, String.valueOf(a.getSemestre()));
            }

            documento.add(tabla);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return out.toByteArray();
    }

    // 3. Reporte Departamental de Profesores por Carrera (para el Administrador)
    public byte[] generarReporteDocentesPorCarrera(String carrera) throws Exception {
        List<Docente> lista = docenteDAO.buscarPorCarrera(carrera);
        if (lista.isEmpty()) {
            throw new IllegalArgumentException("No se encontraron docentes registrados en la carrera: " + carrera + ".");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 48, 48);

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();
            agregarEncabezadoInstitucional(documento, "Reporte Departamental de Docentes por Carrera");

            documento.add(new Paragraph("Carrera: " + carrera + "\n\n", F_NEGRITA));

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{3.5f, 3.5f, 1.5f, 2.5f});

            agregarCabeceras(tabla, new String[]{"Nombre del Docente", "Materia", "Grupo", "No. Empleado"});

            Iterator<Docente> it = lista.iterator();
            while (it.hasNext()) {
                Docente d = it.next();
                agregarCelda(tabla, d.getNombreCompleto());
                agregarCelda(tabla, d.getMateria());
                agregarCelda(tabla, d.getGrupo());
                agregarCelda(tabla, d.getNumeroEmpleado());
            }

            documento.add(tabla);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return out.toByteArray();
    }

    private void agregarEncabezadoInstitucional(Document documento, String subtituloTexto) throws Exception {
        try {
            ClassPathResource recursoLogo = new ClassPathResource("static/img/logo.png");
            if (recursoLogo.exists()) {
                Image logo = Image.getInstance(recursoLogo.getInputStream().readAllBytes());
                logo.scaleToFit(90, 90);
                logo.setAlignment(Element.ALIGN_LEFT);
                documento.add(logo);
            }
        } catch (Exception ignored) {
            // Si no existe logo.png, genera el PDF sin interrumpir el proceso
        }

        Paragraph titulo = new Paragraph("UNIVERSIDAD AUTÓNOMA DEL ESTADO DE MÉXICO", F_TITULO);
        titulo.setAlignment(Element.ALIGN_CENTER);
        documento.add(titulo);

        Paragraph sub = new Paragraph("Centro Universitario UAEM Atlacomulco\n" + subtituloTexto + "\n\n", F_SUBTITULO);
        sub.setAlignment(Element.ALIGN_CENTER);
        documento.add(sub);
    }

    private void agregarCabeceras(PdfPTable tabla, String[] encabezados) {
        for (String texto : encabezados) {
            PdfPCell celda = new PdfPCell(new Phrase(texto, F_HEADER));
            celda.setBackgroundColor(VERDE_UAEM);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setPadding(6f);
            tabla.addCell(celda);
        }
    }

    private void agregarCelda(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Phrase(valor != null ? valor : "", F_NORMAL));
        celda.setPadding(5f);
        tabla.addCell(celda);
    }
}