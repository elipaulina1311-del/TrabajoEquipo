package com.control.escolar.dao; 
 
import com.control.escolar.model.Alumno; 
import com.control.escolar.model.Docente; 
import java.sql.Connection; 
import java.sql.PreparedStatement; 
import java.sql.ResultSet; 
import java.sql.SQLException; 
import java.util.ArrayList; 
import java.util.Iterator; 
import java.util.List; 
import javax.sql.DataSource; 
import org.springframework.stereotype.Repository; 
 
@Repository 
public class DocenteDAO implements GenericDAO<Docente, Integer> { 
 
    private final DataSource dataSource; 
 
    public DocenteDAO(DataSource dataSource) { 
        this.dataSource = dataSource; 
    } 
 
    @Override 
    public void guardar(Docente d) throws Exception { 
        String sql = "INSERT INTO docente " 
                   + "(idDocente, nombre, appaterno, apmaterno, numero_empleado, materia, carrera, grupo) " 
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"; 
        Connection con = null; 
        PreparedStatement ps = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            ps.setInt(1, d.getId()); 
            ps.setString(2, d.getNombre()); 
            ps.setString(3, d.getApPaterno()); 
            ps.setString(4, d.getApMaterno()); 
            ps.setString(5, d.getNumeroEmpleado()); 
            ps.setString(6, d.getMateria()); 
            ps.setString(7, d.getCarrera()); 
            ps.setString(8, d.getGrupo()); 
            ps.executeUpdate(); 
        } catch (SQLException e) { 
            throw new SQLException("Error al guardar docente en MySQL: " + e.getMessage(), e); 
        } finally { 
            cerrarRecursos(null, ps, con); 
        } 
    } 
 
    // Busca una fila específica por su llave primaria (idAsignacion) 
    @Override 
    public Docente buscarPorId(Integer idAsignacion) throws Exception { 
        String sql = "SELECT * FROM docente WHERE idAsignacion = ?"; 
        Connection con = null; 
        PreparedStatement ps = null; 
        ResultSet rs = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            ps.setInt(1, idAsignacion); 
            rs = ps.executeQuery(); 
            if (rs.next()) { 
                return mapearDocente(rs); 
            } 
            return null; 
        } catch (SQLException e) { 
            throw new SQLException("Error al buscar asignación de docente: " + e.getMessage(), 
e); 
        } finally { 
            cerrarRecursos(rs, ps, con); 
        } 
    } 
 
    @Override 
    public List<Docente> listarTodos() throws Exception { 
        List<Docente> lista = new ArrayList<>(); 
        String sql = "SELECT * FROM docente ORDER BY idAsignacion DESC"; 
        Connection con = null; 
        PreparedStatement ps = null; 
        ResultSet rs = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            rs = ps.executeQuery(); 
            while (rs.next()) { 
                lista.add(mapearDocente(rs)); 
            } 
        } catch (SQLException e) { 
            throw new SQLException("Error al listar docentes: " + e.getMessage(), e); 
        } finally { 
            cerrarRecursos(rs, ps, con); 
        } 
        return lista; 
    } 
 
    @Override 
    public boolean actualizar(Docente d) throws Exception { 
        String sql = "UPDATE docente SET idDocente = ?, nombre = ?, appaterno = ?, apmaterno = ?, " 
                   + "numero_empleado = ?, materia = ?, carrera = ?, grupo = ? " 
                   + "WHERE idAsignacion = ?"; 
        Connection con = null; 
        PreparedStatement ps = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            ps.setInt(1, d.getId()); 
            ps.setString(2, d.getNombre()); 
            ps.setString(3, d.getApPaterno()); 
            ps.setString(4, d.getApMaterno()); 
            ps.setString(5, d.getNumeroEmpleado()); 
            ps.setString(6, d.getMateria()); 
            ps.setString(7, d.getCarrera()); 
            ps.setString(8, d.getGrupo()); 
            ps.setInt(9, d.getIdAsignacion()); 
            return ps.executeUpdate() > 0; 
        } catch (SQLException e) { 
            throw new SQLException("Error al actualizar docente: " + e.getMessage(), e); 
        } finally { 
            cerrarRecursos(null, ps, con); 
        } 
    } 
 
    @Override 
    public boolean eliminar(Integer idAsignacion) throws Exception { 
        String sql = "DELETE FROM docente WHERE idAsignacion = ?"; 
        Connection con = null; 
        PreparedStatement ps = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            ps.setInt(1, idAsignacion); 
            return ps.executeUpdate() > 0; 
        } catch (SQLException e) { 
            throw new SQLException("Error al eliminar asignación docente: " + e.getMessage(), e); 
        } finally { 
            cerrarRecursos(null, ps, con); 
        } 
    } 
 
    // Uso de Colecciones e Iteradores para filtrar registros de docentes en memoria 
    public List<Docente> filtrarConIterador(String busqueda, String carreraFiltro) throws Exception { 
        List<Docente> docentes = listarTodos(); 
        Iterator<Docente> iterador = docentes.iterator(); 
 
        String texto = (busqueda != null) ? busqueda.trim().toLowerCase() : ""; 
        String carrera = (carreraFiltro != null) ? carreraFiltro.trim() : ""; 
 
        while (iterador.hasNext()) { 
            Docente d = iterador.next(); 
            boolean coincideTexto = texto.isEmpty() 
                    || d.getNombreCompleto().toLowerCase().contains(texto) 
                    || (d.getMateria() != null && d.getMateria().toLowerCase().contains(texto)) 
                    || (d.getGrupo() != null && d.getGrupo().toLowerCase().contains(texto)) 
                    || (d.getNumeroEmpleado() != null && 
d.getNumeroEmpleado().toLowerCase().contains(texto)) 
                    || String.valueOf(d.getId()).equals(texto); 
 
            boolean coincideCarrera = carrera.isEmpty() 
                    || carrera.equalsIgnoreCase("TODAS") 
                    || (d.getCarrera() != null && d.getCarrera().equalsIgnoreCase(carrera)); 
 
            if (!coincideTexto || !coincideCarrera) { 
                iterador.remove(); 
            } 
        } 
        return docentes; 
    } 
 
    // --- Consultas de apoyo para el módulo de Reportes PDF --- 
 
    public List<Docente> buscarAsignacionesPorIdDocente(int idDocente) throws Exception { 
        List<Docente> lista = new ArrayList<>(); 
        String sql = "SELECT * FROM docente WHERE idDocente = ? ORDER BY materia"; 
        Connection con = null; 
        PreparedStatement ps = null; 
        ResultSet rs = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            ps.setInt(1, idDocente); 
            rs = ps.executeQuery(); 
            while (rs.next()) { 
                lista.add(mapearDocente(rs)); 
            } 
        } catch (SQLException e) { 
            throw new SQLException("Error al consultar asignaciones del docente: " + 
e.getMessage(), e); 
        } finally { 
            cerrarRecursos(rs, ps, con); 
        } 
        return lista; 
    } 
 
    public List<Docente> buscarPorCarrera(String carrera) throws Exception { 
        List<Docente> lista = new ArrayList<>(); 
        String sql = "SELECT * FROM docente WHERE carrera = ? ORDER BY nombre, materia"; 
        Connection con = null; 
        PreparedStatement ps = null; 
        ResultSet rs = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            ps.setString(1, carrera); 
            rs = ps.executeQuery(); 
            while (rs.next()) { 
                lista.add(mapearDocente(rs)); 
            } 
        } catch (SQLException e) { 
            throw new SQLException("Error al consultar docentes por carrera: " + e.getMessage(), 
e); 
        } finally { 
            cerrarRecursos(rs, ps, con); 
        } 
        return lista; 
    } 
 
    public List<Alumno> listarAlumnosPorGrupoYMateria(String grupo, String materia, Integer 
idDocenteFiltro) throws Exception { 
        List<Alumno> alumnos = new ArrayList<>(); 
        String sql = (idDocenteFiltro != null) 
                ? "SELECT DISTINCT p.idPersona, p.Nombre, p.Ap_paterno, p.Ap_materno, p.Semestre, p.grupo, p.carrera " 
                  + "FROM persona p INNER JOIN docente d ON p.grupo = d.grupo " 
                  + "WHERE p.grupo = ? AND d.materia = ? AND d.idDocente = ? ORDER BY p.Ap_paterno, p.Nombre" 
                : "SELECT DISTINCT p.idPersona, p.Nombre, p.Ap_paterno, p.Ap_materno, p.Semestre, p.grupo, p.carrera " 
                  + "FROM persona p INNER JOIN docente d ON p.grupo = d.grupo " 
                  + "WHERE p.grupo = ? AND d.materia = ? ORDER BY p.Ap_paterno, p.Nombre"; 
 
        Connection con = null; 
        PreparedStatement ps = null; 
        ResultSet rs = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            ps.setString(1, grupo); 
            ps.setString(2, materia); 
            if (idDocenteFiltro != null) { 
                ps.setInt(3, idDocenteFiltro); 
            } 
            rs = ps.executeQuery(); 
            while (rs.next()) { 
                Alumno a = new Alumno(); 
                a.setId(rs.getInt("idPersona")); 
                a.setNombre(rs.getString("Nombre")); 
                a.setApPaterno(rs.getString("Ap_paterno")); 
                a.setApMaterno(rs.getString("Ap_materno")); 
                a.setSemestre(rs.getInt("Semestre")); 
                a.setGrupo(rs.getString("grupo")); 
                a.setCarrera(rs.getString("carrera")); 
                alumnos.add(a); 
            } 
        } catch (SQLException e) { 
            throw new SQLException("Error al consultar alumnos por grupo y materia: " + 
e.getMessage(), e); 
        } finally { 
            cerrarRecursos(rs, ps, con); 
        } 
        return alumnos; 
    } 
 
    public List<String> listarGruposDisponibles(Integer idDocenteFiltro) throws Exception { 
        return listarColumnaUnica("grupo", idDocenteFiltro); 
    } 
 
    public List<String> listarMateriasDisponibles(Integer idDocenteFiltro) throws Exception { 
        return listarColumnaUnica("materia", idDocenteFiltro); 
    } 
 
    public List<String> listarCarrerasDisponibles() throws Exception { 
        return listarColumnaUnica("carrera", null); 
    } 
 
    private List<String> listarColumnaUnica(String columna, Integer idDocenteFiltro) throws 
Exception { 
        List<String> valores = new ArrayList<>(); 
        String sql = (idDocenteFiltro != null) 
                ? "SELECT DISTINCT " + columna + " FROM docente WHERE idDocente = ? ORDER BY " + columna 
                : "SELECT DISTINCT " + columna + " FROM docente ORDER BY " + columna; 
 
        Connection con = null; 
        PreparedStatement ps = null; 
        ResultSet rs = null; 
 
        try { 
            con = dataSource.getConnection(); 
            ps = con.prepareStatement(sql); 
            if (idDocenteFiltro != null) { 
                ps.setInt(1, idDocenteFiltro); 
            } 
            rs = ps.executeQuery(); 
            while (rs.next()) { 
                String val = rs.getString(1); 
                if (val != null && !val.isBlank()) { 
                    valores.add(val); 
                } 
            } 
        } catch (SQLException e) { 
            throw new SQLException("Error al cargar lista de " + columna + ": " + e.getMessage(), 
e); 
        } finally { 
            cerrarRecursos(rs, ps, con); 
        } 
        return valores; 
    } 
 
    private Docente mapearDocente(ResultSet rs) throws SQLException { 
        Docente d = new Docente(); 
        d.setIdAsignacion(rs.getInt("idAsignacion")); 
        d.setId(rs.getInt("idDocente")); 
        d.setNombre(rs.getString("nombre")); 
        d.setApPaterno(rs.getString("appaterno")); 
        d.setApMaterno(rs.getString("apmaterno")); 
        d.setNumeroEmpleado(rs.getString("numero_empleado")); 
        d.setMateria(rs.getString("materia")); 
        d.setCarrera(rs.getString("carrera")); 
        d.setGrupo(rs.getString("grupo")); 
        return d; 
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