package com.control.escolar.controller;

import com.control.escolar.dao.DocenteDAO;
import com.control.escolar.model.Docente;
import com.control.escolar.service.ReportePdfService;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class DocenteController {

    private static final String REGEX_TEXTO = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s\\.]+$";
    private static final String REGEX_MATERIA = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ0-9\\s\\.\\-]+$";
    private static final String REGEX_ALFANUM = "^[a-zA-Z0-9\\s\\-]+$";

    private final DocenteDAO docenteDAO;
    private final ReportePdfService reportePdfService;

    public DocenteController(DocenteDAO docenteDAO, ReportePdfService reportePdfService) {
        this.docenteDAO = docenteDAO;
        this.reportePdfService = reportePdfService;
    }

    // ==================== CRUD ADMINISTRADOR (/admin/docentes) ====================

    @GetMapping("/admin/docentes")
    public String listarDocentes(@RequestParam(required = false) String q,
                                 @RequestParam(required = false) String carrera,
                                 @RequestParam(required = false) Integer editarId,
                                 Model model) {
        List<Docente> lista = Collections.emptyList();
        List<String> carreras = Collections.emptyList();
        Docente docenteForm = new Docente();

        try {
            lista = docenteDAO.filtrarConIterador(q, carrera);
            carreras = docenteDAO.listarCarrerasDisponibles();

            if (editarId != null && editarId > 0) {
                Docente encontrado = docenteDAO.buscarPorId(editarId);
                if (encontrado != null) {
                    docenteForm = encontrado;
                } else {
                    model.addAttribute("mensajeError", "No se encontró la asignación con ID " + editarId + ".");
                }
            }
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error al cargar el catálogo de docentes: " + e.getMessage());
        } finally {
            model.addAttribute("docentes", lista);
            model.addAttribute("carreras", carreras);
            model.addAttribute("docenteForm", docenteForm);
            model.addAttribute("q", q != null ? q : "");
            model.addAttribute("carreraSeleccionada", carrera != null ? carrera : "TODAS");
        }
        return "admin/docentes";
    }

    @PostMapping("/admin/docentes/guardar")
    public String guardarOActualizarDocente(@ModelAttribute("docenteForm") Docente docente,
                                            RedirectAttributes redirect) {
        String error = validarDocente(docente);
        if (error != null) {
            redirect.addFlashAttribute("mensajeError", error);
            return docente.getIdAsignacion() > 0
                    ? "redirect:/admin/docentes?editarId=" + docente.getIdAsignacion()
                    : "redirect:/admin/docentes";
        }

        try {
            if (docente.getIdAsignacion() > 0) {
                boolean actualizado = docenteDAO.actualizar(docente);
                if (actualizado) {
                    redirect.addFlashAttribute("mensajeExito",
                            "Asignación ID " + docente.getIdAsignacion() + " actualizada correctamente.");
                } else {
                    redirect.addFlashAttribute("mensajeError", "No se encontró el registro de docente a actualizar.");
                }
            } else {
                docenteDAO.guardar(docente);
                redirect.addFlashAttribute("mensajeExito", "Docente y asignación registrados correctamente en MySQL.");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", "Error en base de datos: " + e.getMessage());
        } finally {
            System.out.println("[AUDITORÍA DOCENTES] Operación guardar/actualizar finalizada para ID Docente: " + docente.getId());
        }

        return "redirect:/admin/docentes";
    }

    @PostMapping("/admin/docentes/eliminar/{idAsignacion}")
    public String eliminarDocente(@PathVariable int idAsignacion, RedirectAttributes redirect) {
        try {
            boolean eliminado = docenteDAO.eliminar(idAsignacion);
            if (eliminado) {
                redirect.addFlashAttribute("mensajeExito", "Asignación ID " + idAsignacion + " eliminada correctamente.");
            } else {
                redirect.addFlashAttribute("mensajeError", "No se encontró la asignación con ID " + idAsignacion + ".");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", "Error al eliminar asignación: " + e.getMessage());
        } finally {
            System.out.println("[AUDITORÍA DOCENTES] Intento de eliminación finalizado para asignación: " + idAsignacion);
        }
        return "redirect:/admin/docentes";
    }

    // ==================== PORTAL Y REPORTES PDF (/docente/reportes) ====================

    @GetMapping("/docente/reportes")
    public String vistaReportes(HttpSession session, Model model) {
        String rol = (String) session.getAttribute("usuarioRol");
        boolean esDocente = "DOCENTE".equalsIgnoreCase(rol);
        Integer idDocenteSesion = obtenerIdSesion(session);

        List<String> grupos = Collections.emptyList();
        List<String> materias = Collections.emptyList();
        List<String> carreras = Collections.emptyList();

        try {
            Integer filtro = esDocente ? idDocenteSesion : null;
            grupos = docenteDAO.listarGruposDisponibles(filtro);
            materias = docenteDAO.listarMateriasDisponibles(filtro);
            if (!esDocente) {
                carreras = docenteDAO.listarCarrerasDisponibles();
            }
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error al cargar filtros de reportes: " + e.getMessage());
        } finally {
            model.addAttribute("esDocente", esDocente);
            model.addAttribute("rol", rol);
            model.addAttribute("idDocenteSesion", idDocenteSesion != null ? idDocenteSesion : "");
            model.addAttribute("grupos", grupos);
            model.addAttribute("materias", materias);
            model.addAttribute("carreras", carreras);
        }

        return "docente/reportes";
    }

    @GetMapping("/docente/pdf/individual")
    public Object descargarReporteIndividual(@RequestParam(required = false) Integer idDocente,
                                             HttpSession session,
                                             RedirectAttributes redirect) {
        String rol = (String) session.getAttribute("usuarioRol");
        boolean esDocente = "DOCENTE".equalsIgnoreCase(rol);

        try {
            // Si el usuario es docente, se fuerza su propio ID de sesión para impedir consultas ajenas
            Integer idObjetivo = esDocente ? obtenerIdSesion(session) : idDocente;
            if (idObjetivo == null || idObjetivo <= 0) {
                redirect.addFlashAttribute("mensajeError", "Ingresa un ID de docente válido para generar el reporte.");
                return "redirect:/docente/reportes";
            }

            byte[] pdf = reportePdfService.generarReporteDocenteIndividual(idObjetivo);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Reporte_Docente_" + idObjetivo + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/docente/reportes";
        } finally {
            System.out.println("[AUDITORÍA PDF] Solicitud de reporte individual docente procesada.");
        }
    }

    @GetMapping("/docente/pdf/grupal")
    public Object descargarReporteGrupal(@RequestParam String grupo,
                                         @RequestParam String materia,
                                         HttpSession session,
                                         RedirectAttributes redirect) {
        String rol = (String) session.getAttribute("usuarioRol");
        boolean esDocente = "DOCENTE".equalsIgnoreCase(rol);

        if (grupo == null || grupo.isBlank() || materia == null || materia.isBlank()) {
            redirect.addFlashAttribute("mensajeError", "Selecciona un grupo y una materia válidos.");
            return "redirect:/docente/reportes";
        }

        try {
            Integer filtro = esDocente ? obtenerIdSesion(session) : null;
            byte[] pdf = reportePdfService.generarReporteAlumnosPorGrupoMateria(grupo.trim(), materia.trim(), filtro);
            String nombreArchivo = ("Reporte_Grupo_" + grupo + "_" + materia + ".pdf").replaceAll("\\s+", "_");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/docente/reportes";
        } finally {
            System.out.println("[AUDITORÍA PDF] Solicitud de reporte grupal procesada (" + grupo + " - " + materia + ").");
        }
    }

    @GetMapping("/docente/pdf/carrera")
    public Object descargarReportePorCarrera(@RequestParam String carrera,
                                             HttpSession session,
                                             RedirectAttributes redirect) {
        String rol = (String) session.getAttribute("usuarioRol");
        if (!"ADMINISTRADOR".equalsIgnoreCase(rol)) {
            redirect.addFlashAttribute("mensajeError", "Solo el administrador puede emitir reportes departamentales por carrera.");
            return "redirect:/docente/reportes";
        }

        if (carrera == null || carrera.isBlank()) {
            redirect.addFlashAttribute("mensajeError", "Selecciona una carrera válida.");
            return "redirect:/docente/reportes";
        }

        try {
            byte[] pdf = reportePdfService.generarReporteDocentesPorCarrera(carrera.trim());
            String nombreArchivo = ("Reporte_Carrera_" + carrera + ".pdf").replaceAll("\\s+", "_");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + nombreArchivo)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", e.getMessage());
            return "redirect:/docente/reportes";
        } finally {
            System.out.println("[AUDITORÍA PDF] Solicitud de reporte por carrera procesada: " + carrera);
        }
    }

    private Integer obtenerIdSesion(HttpSession session) {
        Object idObj = session.getAttribute("usuarioId");
        if (idObj == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(idObj));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String validarDocente(Docente d) {
        if (d.getId() <= 0) {
            return "Error de formato: El ID del docente debe ser un número entero mayor a 0.";
        }
        if (d.getNombre() == null || d.getNombre().isBlank()
                || d.getApPaterno() == null || d.getApPaterno().isBlank()
                || d.getApMaterno() == null || d.getApMaterno().isBlank()
                || d.getNumeroEmpleado() == null || d.getNumeroEmpleado().isBlank()
                || d.getMateria() == null || d.getMateria().isBlank()
                || d.getCarrera() == null || d.getCarrera().isBlank()
                || d.getGrupo() == null || d.getGrupo().isBlank()) {
            return "Error: Todos los campos del formulario son obligatorios.";
        }
        if (!d.getNombre().trim().matches(REGEX_TEXTO)
                || !d.getApPaterno().trim().matches(REGEX_TEXTO)
                || !d.getApMaterno().trim().matches(REGEX_TEXTO)
                || !d.getCarrera().trim().matches(REGEX_TEXTO)) {
            return "Error de formato: Nombre, apellidos y carrera solo deben contener letras.";
        }
        if (!d.getMateria().trim().matches(REGEX_MATERIA)) {
            return "Error de formato: La materia contiene caracteres no permitidos.";
        }
        if (!d.getNumeroEmpleado().trim().matches(REGEX_ALFANUM)
                || !d.getGrupo().trim().matches(REGEX_ALFANUM)) {
            return "Error de formato: Número de empleado y grupo solo aceptan letras, números y guiones.";
        }

        d.setNombre(d.getNombre().trim());
        d.setApPaterno(d.getApPaterno().trim());
        d.setApMaterno(d.getApMaterno().trim());
        d.setNumeroEmpleado(d.getNumeroEmpleado().trim());
        d.setMateria(d.getMateria().trim());
        d.setCarrera(d.getCarrera().trim());
        d.setGrupo(d.getGrupo().trim());
        return null;
    }
}