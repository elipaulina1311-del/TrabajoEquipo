package com.control.escolar.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);
        String rol = (session != null) ? (String) session.getAttribute("usuarioRol") : null;
        String ruta = request.getRequestURI();

        
        if (rol == null || rol.isBlank()) {
            response.sendRedirect("/login?error=sin_sesion");
            return false;
        }

        
        if (ruta.startsWith("/admin") && !"ADMINISTRADOR".equalsIgnoreCase(rol)) {
            response.sendRedirect("/login?error=no_autorizado");
            return false;
        }

        if (ruta.startsWith("/alumno") && !"ALUMNO".equalsIgnoreCase(rol)) {
            response.sendRedirect("/login?error=no_autorizado");
            return false;
        }

        if (ruta.startsWith("/docente")
                && !"DOCENTE".equalsIgnoreCase(rol)
                && !"ADMINISTRADOR".equalsIgnoreCase(rol)) {
            response.sendRedirect("/login?error=no_autorizado");
            return false;
        }

        return true;
    }


}
