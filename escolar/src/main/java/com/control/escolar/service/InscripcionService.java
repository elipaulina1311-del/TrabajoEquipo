package com.control.escolar.service;

import com.control.escolar.model.Alumno;
import com.control.escolar.model.CuentaUniversidad;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class InscripcionService {

    private final DataSource dataSource;

    private static final Font F_TITULO = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD, new BaseColor(27, 94, 32));
    private static final Font F_SUBTITULO = new Font(Font.FontFamily.HELVETICA, 11, Font.NORMAL, BaseColor.DARK_GRAY);
    private static final Font F_NEGRITA = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.BLACK);
    private static final Font F_NORMAL = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, BaseColor.BLACK);
    private static final Font F_HEADER = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);
    private static final BaseColor VERDE_UAEM = new BaseColor(46, 125, 50);

    public InscripcionService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // 1. Transacción atómica de pago de inscripción ($1,500.00)
    public String procesarInscripcion(int idAlumno) throws Exception {
        Connection con = null;
        PreparedStatement psCheck = null;
        PreparedStatement psUpdateAlu = null;
        PreparedStatement psUpdateUni = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();
            con.setAutoCommit(false);

            String sqlCheck = "SELECT p.saldo, p.estatus, c.idNCuenta, c.costo_inscripcion "
                            + "FROM persona p "
                            + "INNER JOIN cuenta_uaem_atlacomulco c ON p.idNCuenta = c.idNCuenta "
                            + "WHERE p.idPersona = ? FOR UPDATE";
            psCheck = con.prepareStatement(sqlCheck);
            psCheck.setInt(1, idAlumno);
            rs = psCheck.executeQuery();

            if (!rs.next()) {
                con.rollback();
                return "No se encontró el registro del alumno o su cuenta institucional vinculada.";
            }

            String estatus = rs.getString("estatus");
            double saldoActual = rs.getDouble("saldo");
            double costoInscripcion = rs.getDouble("costo_inscripcion");
            int idCuenta = rs.getInt("idNCuenta");

            if ("Inscrito".equalsIgnoreCase(estatus)) {
                con.rollback();
                return "Ya te encuentras inscrito. No es necesario volver a pagar.";
            }

            if (saldoActual < costoInscripcion) {
                con.rollback();
                return String.format("Saldo insuficiente. Tu saldo actual es de $%.2f y la inscripción cuesta $%.2f.",
                        saldoActual, costoInscripcion);
            }

            // Descuento al alumno y actualización de estatus
            String sqlAlu = "UPDATE persona SET saldo = saldo - ?, estatus = 'Inscrito' WHERE idPersona = ?";
            psUpdateAlu = con.prepareStatement(sqlAlu);
            psUpdateAlu.setDouble(1, costoInscripcion);
            psUpdateAlu.setInt(2, idAlumno);
            psUpdateAlu.executeUpdate();

            // Abono acumulativo a la misma fila de la cuenta universitaria
            String sqlUni = "UPDATE cuenta_uaem_atlacomulco SET saldo_acumulado = saldo_acumulado + ? WHERE idNCuenta = ?";
            psUpdateUni = con.prepareStatement(sqlUni);
            psUpdateUni.setDouble(1, costoInscripcion);
            psUpdateUni.setInt(2, idCuenta);
            psUpdateUni.executeUpdate();

            con.commit();
            return "OK";
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new SQLException("Error durante la transacción de inscripción: " + e.getMessage(), e);
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
            cerrarRecursos(rs, psCheck, null);
            cerrarRecursos(null, psUpdateAlu, null);
            cerrarRecursos(null, psUpdateUni, con);
        }
    }

    // 2. Consulta de cuenta institucional (para portal alumno y dashboard)
    public CuentaUniversidad obtenerCuentaInstitucional() throws Exception {
        String sql = "SELECT idNCuenta, costo_inscripcion, saldo_acumulado FROM cuenta_uaem_atlacomulco LIMIT 1";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            if (rs.next()) {
                return new CuentaUniversidad(
                        rs.getInt("idNCuenta"),
                        rs.getDouble("costo_inscripcion"),
                        rs.getDouble("saldo_acumulado")
                );
            }
            return new CuentaUniversidad(1, 1500.00, 0.00);
        } catch (SQLException e) {
            throw new SQLException("Error al consultar cuenta universitaria: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(rs, ps, con);
        }
    }

    // 3. Métricas en tiempo real para el Dashboard del Administrador
    public Map<String, Object> obtenerMetricasDashboard() throws Exception {
        Map<String, Object> metricas = new HashMap<>();
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();

            String sqlAlumnos = "SELECT COUNT(*) AS total, "
                              + "SUM(CASE WHEN estatus = 'Inscrito' THEN 1 ELSE 0 END) AS inscritos, "
                              + "SUM(CASE WHEN estatus <> 'Inscrito' OR estatus IS NULL THEN 1 ELSE 0 END) AS pendientes "
                              + "FROM persona";
            ps = con.prepareStatement(sqlAlumnos);
            rs = ps.executeQuery();
            if (rs.next()) {
                metricas.put("totalAlumnos", rs.getInt("total"));
                metricas.put("totalInscritos", rs.getInt("inscritos"));
                metricas.put("totalPendientes", rs.getInt("pendientes"));
            }
            rs.close();
            ps.close();

            String sqlDocentes = "SELECT COUNT(DISTINCT idDocente) AS docentes, COUNT(*) AS asignaciones FROM docente";
            ps = con.prepareStatement(sqlDocentes);
            rs = ps.executeQuery();
            if (rs.next()) {
                metricas.put("totalDocentes", rs.getInt("docentes"));
                metricas.put("totalAsignaciones", rs.getInt("asignaciones"));
            }

            CuentaUniversidad cuenta = obtenerCuentaInstitucional();
            metricas.put("saldoUniversidad", cuenta.getSaldoAcumulado());
            metricas.put("costoInscripcion", cuenta.getCostoInscripcion());
        } catch (SQLException e) {
            throw new SQLException("Error al cargar métricas del dashboard: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(rs, ps, con);
        }
        return metricas;
    }

    // 4. Consulta de materias del grupo del alumno
    public List<Docente> obtenerMateriasPorGrupo(String grupo) throws Exception {
        List<Docente> materias = new ArrayList<>();
        String sql = "SELECT idDocente, nombre, appaterno, apmaterno, materia, carrera, grupo "
                   + "FROM docente WHERE grupo = ? ORDER BY materia";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, grupo);
            rs = ps.executeQuery();
            while (rs.next()) {
                Docente d = new Docente();
                d.setId(rs.getInt("idDocente"));
                d.setNombre(rs.getString("nombre"));
                d.setApPaterno(rs.getString("appaterno"));
                d.setApMaterno(rs.getString("apmaterno"));
                d.setMateria(rs.getString("materia"));
                d.setCarrera(rs.getString("carrera"));
                d.setGrupo(rs.getString("grupo"));
                materias.add(d);
            }
        } catch (SQLException e) {
            throw new SQLException("Error al consultar materias del grupo: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(rs, ps, con);
        }
        return materias;
    }

    // 5. Generación de PDF: Tira de Materias y Comprobante de Inscripción (solo alumnos inscritos)
    public byte[] generarTiraMateriasPdf(Alumno alumno) throws Exception {
        if (alumno == null) {
            throw new IllegalArgumentException("No se encontró la información del estudiante.");
        }
        if (!"Inscrito".equalsIgnoreCase(alumno.getEstatus())) {
            throw new IllegalStateException("Acceso bloqueado: debes completar tu pago de inscripción ($1,500.00) para descargar tu Tira de Materias.");
        }

        List<Docente> materias = obtenerMateriasPorGrupo(alumno.getGrupo());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 36, 36, 48, 48);

        try {
            PdfWriter.getInstance(documento, out);
            documento.open();

            try {
                ClassPathResource recursoLogo = new ClassPathResource("static/img/logo.png");
                if (recursoLogo.exists()) {
                    Image logo = Image.getInstance(recursoLogo.getInputStream().readAllBytes());
                    logo.scaleToFit(90, 90);
                    logo.setAlignment(Element.ALIGN_LEFT);
                    documento.add(logo);
                }
            } catch (Exception ignored) {
            }

            Paragraph titulo = new Paragraph("UNIVERSIDAD AUTÓNOMA DEL ESTADO DE MÉXICO", F_TITULO);
            titulo.setAlignment(Element.ALIGN_CENTER);
            documento.add(titulo);

            Paragraph sub = new Paragraph("Centro Universitario UAEM Atlacomulco\nComprobante de Inscripción y Tira de Materias\n\n", F_SUBTITULO);
            sub.setAlignment(Element.ALIGN_CENTER);
            documento.add(sub);

            documento.add(new Paragraph("ID Alumno: " + alumno.getId() + "   |   Estatus: " + alumno.getEstatus(), F_NEGRITA));
            documento.add(new Paragraph("Nombre Completo: " + alumno.getNombreCompleto(), F_NORMAL));
            documento.add(new Paragraph("Carrera: " + alumno.getCarrera() + "   |   Plantel: " + alumno.getEspacioAcademico(), F_NORMAL));
            documento.add(new Paragraph(String.format("Grupo: %s   |   Semestre: %d   |   Saldo Restante: $%.2f\n\n",
                    alumno.getGrupo(), alumno.getSemestre(), alumno.getSaldo()), F_NORMAL));

            documento.add(new Paragraph("Tira de Materias Asignadas:", F_NEGRITA));

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(8f);
            tabla.setWidths(new float[]{3.5f, 3.5f, 3f, 1.5f});

            String[] encabezados = {"Materia", "Docente Titular", "Carrera", "Grupo"};
            for (String h : encabezados) {
                PdfPCell c = new PdfPCell(new Phrase(h, F_HEADER));
                c.setBackgroundColor(VERDE_UAEM);
                c.setHorizontalAlignment(Element.ALIGN_CENTER);
                c.setPadding(6f);
                tabla.addCell(c);
            }

            if (materias.isEmpty()) {
                PdfPCell vacia = new PdfPCell(new Phrase("Aún no hay materias asignadas al grupo " + alumno.getGrupo(), F_NORMAL));
                vacia.setColspan(4);
                vacia.setHorizontalAlignment(Element.ALIGN_CENTER);
                vacia.setPadding(8f);
                tabla.addCell(vacia);
            } else {
                Iterator<Docente> it = materias.iterator();
                while (it.hasNext()) {
                    Docente d = it.next();
                    agregarCelda(tabla, d.getMateria());
                    agregarCelda(tabla, d.getNombreCompleto());
                    agregarCelda(tabla, d.getCarrera());
                    agregarCelda(tabla, d.getGrupo());
                }
            }

            documento.add(tabla);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return out.toByteArray();
    }

    private void agregarCelda(PdfPTable tabla, String valor) {
        PdfPCell celda = new PdfPCell(new Phrase(valor != null ? valor : "", F_NORMAL));
        celda.setPadding(5f);
        tabla.addCell(celda);
    }

    private void cerrarRecursos(ResultSet rs, PreparedStatement ps, Connection con) {
        try {
            if (rs != null) rs.close();
        } catch (SQLException ignored) {
        }
        try {
            if (ps != null) ps.close();
        } catch (SQLException ignored) {
        }
        try {
            if (con != null) con.close();
        } catch (SQLException ignored) {
        }
    }
}