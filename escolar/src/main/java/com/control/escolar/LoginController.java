package com.control.escolar;

import com.control.escolar.dao.AuthDAO;
import com.control.escolar.service.InscripcionService;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Controller
public class LoginController {

    private final AuthDAO authDAO;
    private final InscripcionService inscripcionService;

    public LoginController(AuthDAO authDAO, InscripcionService inscripcionService) {
        this.authDAO = authDAO;
        this.inscripcionService = inscripcionService;
    }

    @GetMapping("/")
    public String inicio(HttpSession session) {
        String rol = (String) session.getAttribute("usuarioRol");
        if ("ADMINISTRADOR".equalsIgnoreCase(rol)) {
            return "redirect:/admin/dashboard";
        }
        if ("ALUMNO".equalsIgnoreCase(rol)) {
            return "redirect:/alumno/panel";
        }
        if ("DOCENTE".equalsIgnoreCase(rol)) {
            return "redirect:/docente/reportes";
        }
        return "redirect:/login";
    }



    @GetMapping("/login")
    public String mostrarLogin(@RequestParam(required = false) String error,
                               @RequestParam(required = false) String logout,
                               Model model) {
        if ("sin_sesion".equals(error)) {
            model.addAttribute("mensajeError", "Acceso bloqueado: debes iniciar sesión para ingresar a esa sección.");
        } else if ("no_autorizado".equals(error)) {
            model.addAttribute("mensajeError", "Permiso denegado: tu rol no tiene acceso a la ruta solicitada.");
        }
        if (logout != null) {
            model.addAttribute("mensajeExito", "Sesión cerrada correctamente.");
        }
        return "login";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String usuario,
                                @RequestParam String password,
                                HttpSession session,
                                RedirectAttributes redirect) {
        if (usuario == null || usuario.isBlank() || password == null || password.isBlank()) {
            redirect.addFlashAttribute("mensajeError", "Por favor, selecciona tu tipo de usuario e ingresa tu contraseña.");
            return "redirect:/login";
        }

        String tipoUsuario = usuario.trim().toLowerCase();

        try {
            switch (tipoUsuario) {
                case "administrador":
                    if (authDAO.validarAdministrador(password)) {
                        session.setAttribute("usuarioRol", "ADMINISTRADOR");
                        session.setAttribute("usuarioId", "ADMIN");
                        return "redirect:/admin/dashboard";
                    }
                    redirect.addFlashAttribute("mensajeError", "Contraseña de administrador incorrecta.");
                    return "redirect:/login";

                case "alumno":
                    Integer idAlumno = authDAO.autenticarAlumno(password);
                    if (idAlumno != null) {
                        session.setAttribute("usuarioRol", "ALUMNO");
                        session.setAttribute("usuarioId", idAlumno);
                        return "redirect:/alumno/panel";
                    }
                    redirect.addFlashAttribute("mensajeError", "Credenciales de alumno incorrectas (usa tu clave ALU + ID).");
                    return "redirect:/login";

                case "docente":
                    Integer idDocente = authDAO.autenticarDocente(password);
                    if (idDocente != null) {
                        session.setAttribute("usuarioRol", "DOCENTE");
                        session.setAttribute("usuarioId", idDocente);
                        return "redirect:/docente/reportes";
                    }
                    redirect.addFlashAttribute("mensajeError", "Credenciales de docente incorrectas (usa tu clave DOC + ID).");
                    return "redirect:/login";

                default:
                    redirect.addFlashAttribute("mensajeError", "Tipo de usuario no válido. Usa alumno, docente o administrador.");
                    return "redirect:/login";
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("mensajeError", "Error al conectar con la base de datos: " + e.getMessage());
            return "redirect:/login";
        } finally {
            System.out.println("[AUDITORÍA LOGIN] Intento de acceso procesado para rol: " + tipoUsuario);
        }
    }

    @GetMapping("/logout")
    public String cerrarSesion(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }

    @GetMapping("/admin/dashboard")
    public String mostrarDashboard(Model model) {
        try {
            Map<String, Object> metricas = inscripcionService.obtenerMetricasDashboard();
            model.addAllAttributes(metricas);
        } catch (Exception e) {
            model.addAttribute("mensajeError", "No se pudieron cargar las métricas en tiempo real: " + e.getMessage());
            model.addAttribute("totalAlumnos", 0);
            model.addAttribute("totalInscritos", 0);
            model.addAttribute("totalPendientes", 0);
            model.addAttribute("totalDocentes", 0);
            model.addAttribute("totalAsignaciones", 0);
            model.addAttribute("saldoUniversidad", 0.0);
            model.addAttribute("costoInscripcion", 1500.0);
        }
        return "admin/dashboard";
    }
}