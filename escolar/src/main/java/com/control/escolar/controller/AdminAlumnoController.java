package com.control.escolar.controller;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.control.escolar.dao.AlumnoDAO;
import com.control.escolar.model.Alumno;
import com.control.escolar.service.RespaldoLocalService;

@Controller
@RequestMapping("/admin/alumnos")
public class AdminAlumnoController {

    private static final String REGEX_TEXTO
            = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s\\.]+$";
    private static final String REGEX_GRUPO
            = "^[a-zA-Z0-9\\-]+$";

    private final AlumnoDAO alumnoDAO;
    private final RespaldoLocalService respaldoLocalService;

    public AdminAlumnoController(
            AlumnoDAO alumnoDAO,
            RespaldoLocalService respaldoLocalService) {
        this.alumnoDAO = alumnoDAO;
        this.respaldoLocalService = respaldoLocalService;
    }

    @GetMapping
    public String listarAlumnos(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String estatus,
            @RequestParam(required = false) Integer editarId,
            Model model) {
        List<Alumno> lista = Collections.emptyList();
        Alumno alumnoForm = new Alumno();

        try {
            lista = alumnoDAO.filtrarConIterador(q, estatus);
            if (editarId != null && editarId > 0) {
                Alumno encontrado
                        = alumnoDAO.buscarPorId(editarId);
                if (encontrado != null) {
                    alumnoForm = encontrado;
                } else {
                    model.addAttribute("mensajeError",
                            "No se encontro un alumno con ID "
                            + editarId + ".");
                }
            }
        } catch (Exception e) {
            model.addAttribute("mensajeAdvertencia",
                    "Sin conexion con MySQL. "
                    + "Puedes registrar en respaldo local (.txt). "
                    + "Detalle: " + e.getMessage());
        } finally {
            model.addAttribute("alumnos", lista);
            model.addAttribute("alumnoForm", alumnoForm);
            model.addAttribute("q", q != null ? q : "");
            model.addAttribute("estatusSeleccionado",
                    estatus != null ? estatus : "TODOS");
            model.addAttribute("pendientesRespaldo",
                    respaldoLocalService.contarPendientes());
        }
        return "admin/alumnos";
    }

    @PostMapping("/guardar")
    public String guardarOActualizar(
            @ModelAttribute("alumnoForm") Alumno alumno,
            RedirectAttributes redirect) {
        String errorValidacion = validarAlumno(alumno);
        if (errorValidacion != null) {
            redirect.addFlashAttribute(
                    "mensajeError", errorValidacion);
            if (alumno.getId() > 0) {
                return "redirect:/admin/alumnos?editarId="
                        + alumno.getId();
            }
            return "redirect:/admin/alumnos";
        }

        try {
            if (alumno.getId() > 0) {
                boolean ok = alumnoDAO.actualizar(alumno);
                if (ok) {
                    redirect.addFlashAttribute("mensajeExito",
                            "Alumno ID " + alumno.getId()
                            + " actualizado correctamente.");
                } else {
                    redirect.addFlashAttribute("mensajeError",
                            "No se encontro el alumno a actualizar.");
                }
            } else {
                alumnoDAO.guardar(alumno);
                redirect.addFlashAttribute("mensajeExito",
                        "Alumno registrado correctamente en MySQL.");
            }
        } catch (SQLException sqlEx) {
            // Caso de Uso 6: si falla MySQL, respalda en .txt
            if (alumno.getId() == 0) {
                try {
                    respaldoLocalService
                            .guardarRespaldoTemporal(alumno);
                    redirect.addFlashAttribute(
                            "mensajeAdvertencia",
                            "Conexion con MySQL interrumpida. "
                            + "Se guardo en respaldo_alumnos.txt.");
                } catch (Exception ioEx) {
                    redirect.addFlashAttribute("mensajeError",
                            "Fallo MySQL y tambien el respaldo: "
                            + ioEx.getMessage());
                }
            } else {
                redirect.addFlashAttribute("mensajeError",
                        "Error de base de datos al actualizar: "
                        + sqlEx.getMessage());
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError",
                    "Error inesperado: " + e.getMessage());
        } finally {
            System.out.println(
                    "[AUDITORIA ALUMNOS] Operacion guardar "
                    + "finalizada para: "
                    + alumno.getNombreCompleto());
        }
        return "redirect:/admin/alumnos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminarAlumno(
            @PathVariable int id,
            RedirectAttributes redirect) {
        try {
            boolean eliminado = alumnoDAO.eliminar(id);
            if (eliminado) {
                redirect.addFlashAttribute("mensajeExito",
                        "Alumno ID " + id
                        + " eliminado correctamente.");
            } else {
                redirect.addFlashAttribute("mensajeError",
                        "No se encontro el alumno con ID "
                        + id + ".");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError",
                    "No se pudo eliminar el alumno: "
                    + e.getMessage());
        } finally {
            System.out.println(
                    "[AUDITORIA ALUMNOS] Intento de eliminacion "
                    + "para ID: " + id);
        }
        return "redirect:/admin/alumnos";
    }

    @PostMapping("/cambiar-carrera")
    public String cambiarCarrera(
            @RequestParam int idAlumno,
            @RequestParam String nuevaCarrera,
            @RequestParam String nuevoEspacio,
            @RequestParam String nuevoGrupo,
            RedirectAttributes redirect) {
        if (nuevaCarrera.isBlank()
                || nuevoEspacio.isBlank()
                || nuevoGrupo.isBlank()) {
            redirect.addFlashAttribute("mensajeError",
                    "Completa la nueva carrera, "
                    + "espacio academico y grupo.");
            return "redirect:/admin/alumnos";
        }

        try {
            boolean actualizado = alumnoDAO.cambiarCarrera(
                    idAlumno,
                    nuevaCarrera.trim(),
                    nuevoEspacio.trim(),
                    nuevoGrupo.trim());
            if (actualizado) {
                redirect.addFlashAttribute("mensajeExito",
                        "Cambio de carrera aplicado al alumno ID "
                        + idAlumno + " (sin adeudos).");
            }
        } catch (IllegalStateException
                | IllegalArgumentException regEx) {
            redirect.addFlashAttribute(
                    "mensajeAdvertencia", regEx.getMessage());
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError",
                    "Error al cambiar de carrera: "
                    + e.getMessage());
        } finally {
            System.out.println(
                    "[AUDITORIA ALUMNOS] Tramite de cambio "
                    + "de facultad procesado para ID: "
                    + idAlumno);
        }
        return "redirect:/admin/alumnos";
    }

    @PostMapping("/sincronizar")
    public String sincronizarRespaldo(
            RedirectAttributes redirect) {
        try {
            int total = respaldoLocalService
                    .sincronizarConBaseDeDatos(alumnoDAO);
            if (total > 0) {
                redirect.addFlashAttribute("mensajeExito",
                        "Sincronizacion completa: se subieron "
                        + total + " alumno(s) desde "
                        + "respaldo_alumnos.txt a MySQL.");
            } else {
                redirect.addFlashAttribute("mensajeAdvertencia",
                        "No hay registros pendientes "
                        + "por sincronizar.");
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError",
                    "Aun no es posible conectar con MySQL: "
                    + e.getMessage());
        } finally {
            System.out.println(
                    "[AUDITORIA RESPALDO] Intento de "
                    + "sincronizacion ejecutado.");
        }
        return "redirect:/admin/alumnos";
    }

    private String validarAlumno(Alumno a) {
        if (a.getNombre() == null
                || a.getNombre().isBlank()
                || a.getApPaterno() == null
                || a.getApPaterno().isBlank()
                || a.getApMaterno() == null
                || a.getApMaterno().isBlank()
                || a.getCarrera() == null
                || a.getCarrera().isBlank()
                || a.getEspacioAcademico() == null
                || a.getEspacioAcademico().isBlank()
                || a.getGrupo() == null
                || a.getGrupo().isBlank()) {
            return "Error: Todos los campos de texto "
                    + "son obligatorios.";
        }
        if (!a.getNombre().trim().matches(REGEX_TEXTO)
                || !a.getApPaterno().trim().matches(REGEX_TEXTO)
                || !a.getApMaterno().trim().matches(REGEX_TEXTO)
                || !a.getCarrera().trim().matches(REGEX_TEXTO)
                || !a.getEspacioAcademico().trim()
                        .matches(REGEX_TEXTO)) {
            return "Error de formato: Nombre, apellidos, "
                    + "carrera y espacio solo deben "
                    + "contener letras.";
        }
        if (!a.getGrupo().trim().matches(REGEX_GRUPO)) {
            return "Error de formato: El grupo solo acepta "
                    + "letras, numeros y guiones "
                    + "(ej. ICO-30 o 21).";
        }
        if (a.getEdad() < 15 || a.getEdad() > 99) {
            return "Error de formato: La edad debe estar "
                    + "entre 15 y 99 anos.";
        }
        if (a.getSemestre() < 1 || a.getSemestre() > 12) {
            return "Error de formato: El semestre debe estar "
                    + "entre 1 y 12.";
        }
        if (a.getSaldo() < 0) {
            return "Error de formato: El saldo no puede "
                    + "ser negativo.";
        }
        a.setNombre(a.getNombre().trim());
        a.setApPaterno(a.getApPaterno().trim());
        a.setApMaterno(a.getApMaterno().trim());
        a.setCarrera(a.getCarrera().trim());
        a.setEspacioAcademico(a.getEspacioAcademico().trim());
        a.setGrupo(a.getGrupo().trim());
        return null;
    }
}