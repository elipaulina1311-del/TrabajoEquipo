# 🎓 Sistema Integral de Gestión Universitaria — Control Alumno-Docente

Sistema web para la gestión académica y administrativa del **Centro Universitario UAEM Atlacomulco**, enfocado en el control de alumnos y docentes, autenticación segura por roles, pagos transaccionales de inscripción y emisión de reportes institucionales en PDF. Desarrollado para la unidad de aprendizaje **Paradigmas de Programación I (UAEMéx)** por el **Equipo X**.

---

## Integrantes y Ramas de Trabajo

El desarrollo colaborativo se organiza mediante ramas independientes integradas finalmente en `main`:

| Integrante | Usuario GitHub | Rama de Desarrollo | Módulo Principal |
| :--- | :--- | :--- | :--- |
| **Maritza Barrera Nava** | [@maritzabarrera296-netizen](https://github.com/maritzabarrera296-netizen) | [`feature/docentes-reportes`](https://github.com/elipaulina1311-del/TrabajoEquipo/tree/feature/docentes-reportes) | Gestión de Docentes, Asignaciones y Reportes PDF (`/admin/docentes`, `/docente/reportes`) |
| **Eliza Paulina Martínez Lusi** | [@elipaulina1311-del](https://github.com/elipaulina1311-del) | [`feature/alumnos-dao`](https://github.com/elipaulina1311-del/TrabajoEquipo/tree/feature/alumnos-dao) | Gestión de Alumnos, Patrón DAO Genérico y Respaldo Local (`/admin/alumnos`, `/respaldo`) |
| **Luis David Mendoza Cruz** | [@luisdavid184](https://github.com/luisdavid184) | [`feature/seguridad-inscripciones`](https://github.com/elipaulina1311-del/TrabajoEquipo/tree/feature/seguridad-inscripciones) | Autenticación SHA-256, Seguridad de Rutas, Dashboard e Inscripciones (`/login`, `/admin/dashboard`, `/alumno`) |

---

## Funcionalidades Principales

* **Seguridad y Control de Acceso:** Inicio de sesión con contraseñas cifradas mediante **SHA-256** (`PasswordUtil` y *triggers* automáticos en MySQL con prefijos `ALU`/`DOC`), sesiones HTTP (`HttpSession`) y bloqueo de URLs directas sin autenticación (`AuthInterceptor`) con permisos diferenciados por rol (`ADMINISTRADOR`, `DOCENTE` y `ALUMNO`).
* **Panel Administrativo (`/admin/dashboard`):** Resumen en tiempo real de alumnos registrados, estatus de inscripción, docentes activos y saldo acumulado en la cuenta universitaria (`cuenta_uaem_atlacomulco`).
* **Módulo de Inscripción y Pagos Transaccionales (`/alumno/inscripcion`):** Validación de saldo disponible ($\ge \$1,500.00$) y estatus escolar, prevención de cobros duplicados, descuento automático al saldo del estudiante, cambio de estatus a `Inscrito` y abono acumulativo a la cuenta institucional dentro de una misma transacción segura (`commit` / `rollback`).
* **Operaciones CRUD Completas con DAO y Herencia (`/admin/alumnos`, `/admin/docentes`):** Alta, consulta, actualización y baja en tiempo real sobre MySQL utilizando jerarquía de clases (`Persona` → `Alumno` / `Docente`), interfaz genérica (`GenericDAO<T, ID>`) y recorrido mediante colecciones e iteradores.
* **Reportes Institucionales en PDF (`/alumno/tira-materias`, `/docente/reportes`):** Emisión automatizada con **iText** de Tira de Materias (habilitada únicamente para alumnos inscritos), Reporte Individual de Asignaciones Docentes y Reporte Grupal filtrado por grupo y materia.
* **Validación, Manejo de Errores y Respaldo Local:** Validación de entradas con bloques `try - catch - finally`, alertas claras en pantalla y guardado temporal de emergencia en archivo local (`.txt`) ante fallos de conexión con MySQL.
* **Diseño Responsivo:** Interfaz web construida con **Thymeleaf** y **Bootstrap 5** adaptable a pantallas de escritorio y dispositivos móviles (menú colapsable y tablas responsivas).
