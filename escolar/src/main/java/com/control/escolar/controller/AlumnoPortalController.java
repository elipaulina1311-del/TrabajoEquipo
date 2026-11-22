package com.control.escolar.controller;

import com.control.escolar.dao.AlumnoDAO;
import com.control.escolar.model.Alumno;
import com.control.escolar.model.CuentaUniversidad;
import com.control.escolar.model.Docente;
import com.control.escolar.service.InscripcionService;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/alumno")
public class AlumnoPortalController {

    private final AlumnoDAO alumnoDAO;
    private final InscripcionService inscripcionService;

    public AlumnoPortalController(AlumnoDAO alumnoDAO, InscripcionService inscripcionService) {
        this.alumnoDAO = alumnoDAO;
        this.inscripcionService = inscripcionService;
    }

    @GetMapping("/panel")
    public String mostrarPanelAlumno(HttpSession session, Model model) {
        Integer idAlumno = obtenerIdSesion(session);
        Alumno alumno = null;
        CuentaUniversidad cuenta = new CuentaUniversidad(1, 1500.00, 0.00);
        List<Docente> materias = Collections.emptyList();

        try {
            if (idAlumno != null) {
                alumno = alumnoDAO.buscarPorId(idAlumno);
                cuenta = inscripcionService.obtenerCuentaInstitucional();
                if (alumno != null && alumno.getGrupo() != null) {
                    materias = inscripcionService.obtenerMateriasPorGrupo(alumno.getGrupo());
                }
            }
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error al consultar tu perfil académico: " + e.getMessage());
        } finally {
            model.addAttribute("alumno", alumno);
            model.addAttribute("cuenta", cuenta);
            model.addAttribute("materias", materias);
            boolean inscrito = alumno != null && "Inscrito".equalsIgnoreCase(alumno.getEstatus());
            model.addAttribute("estaInscrito", inscrito);
        }

        return "alumno/panel";
    }

    @PostMapping("/inscribirse")
    public String pagarInscripcion(HttpSession session, RedirectAttributes redirect) {
        Integer idAlumno = obtenerIdSesion(session);
        if (idAlumno == null) {
            return "redirect:/login?error=sin_sesion";
        }

        try {
            String resultado = inscripcionService.procesarInscripcion(idAlumno);
            if ("OK".equals(resultado)) {
                redirect.addFlashAttribute("mensajeExito",
                        "¡Inscripción completada con éxito! Se descontaron $1,500.00 de tu saldo y ya puedes descargar tu Tira de Materias.");
            } else {
                redirect.addFlashAttribute("mensajeAdvertencia", resultado);
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", "No se pudo completar el pago: " + e.getMessage());
        } finally {
            System.out.println("[AUDITORÍA INSCRIPCIÓN] Proceso de pago finalizado para alumno ID: " + idAlumno);
        }

        return "redirect:/alumno/panel";
    }

    @GetMapping("/pdf/tira-materias")
    public Object descargarTiraMaterias(HttpSession session, RedirectAttributes redirect) {
        Integer idAlumno = obtenerIdSesion(session);
        if (idAlumno == null) {
            return "redirect:/login?error=sin_sesion";
        }

        try {
            Alumno alumno = alumnoDAO.buscarPorId(idAlumno);
            byte[] pdf = inscripcionService.generarTiraMateriasPdf(alumno);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Tira_Materias_Alumno_" + idAlumno + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeAdvertencia", e.getMessage());
            return "redirect:/alumno/panel";
        } finally {
            System.out.println("[AUDITORÍA PDF] Solicitud de tira de materias procesada para alumno ID: " + idAlumno);
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
}