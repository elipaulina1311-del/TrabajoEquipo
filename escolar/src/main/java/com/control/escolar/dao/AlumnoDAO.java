package com.control.escolar.dao;

import com.control.escolar.model.Alumno;
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
public class AlumnoDAO implements GenericDAO<Alumno, Integer> {

    private final DataSource dataSource;

    public AlumnoDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void guardar(Alumno a) throws Exception {
        String sql = "INSERT INTO persona "
                   + "(Nombre, Ap_paterno, Ap_materno, Edad, grupo, carrera, espacio_academico, Semestre, saldo, estatus, idNCuenta) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, a.getNombre());
            ps.setString(2, a.getApPaterno());
            ps.setString(3, a.getApMaterno());
            ps.setInt(4, a.getEdad());
            ps.setString(5, a.getGrupo());
            ps.setString(6, a.getCarrera());
            ps.setString(7, a.getEspacioAcademico());
            ps.setInt(8, a.getSemestre());
            ps.setDouble(9, a.getSaldo());
            ps.setString(10, (a.getEstatus() == null || a.getEstatus().isBlank()) ? "No inscrito" : a.getEstatus());
            ps.setInt(11, a.getIdNCuenta() > 0 ? a.getIdNCuenta() : 1);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Error al guardar alumno en MySQL: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(null, ps, con);
        }
    }

    @Override
    public Alumno buscarPorId(Integer id) throws Exception {
        String sql = "SELECT * FROM persona WHERE idPersona = ?";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            ps.setInt(1, id);
            rs = ps.executeQuery();
            if (rs.next()) {
                return mapearAlumno(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new SQLException("Error al buscar alumno por ID: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(rs, ps, con);
        }
    }

    @Override
    public List<Alumno> listarTodos() throws Exception {
        List<Alumno> lista = new ArrayList<>();
        String sql = "SELECT * FROM persona ORDER BY idPersona DESC";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(mapearAlumno(rs));
            }
        } catch (SQLException e) {
            throw new SQLException("Error al listar alumnos: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(rs, ps, con);
        }
        return lista;
    }

    @Override
    public boolean actualizar(Alumno a) throws Exception {
        String sql = "UPDATE persona SET Nombre = ?, Ap_paterno = ?, Ap_materno = ?, Edad = ?, "
                   + "grupo = ?, carrera = ?, espacio_academico = ?, Semestre = ?, saldo = ?, estatus = ? "
                   + "WHERE idPersona = ?";
        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, a.getNombre());
            ps.setString(2, a.getApPaterno());
            ps.setString(3, a.getApMaterno());
            ps.setInt(4, a.getEdad());
            ps.setString(5, a.getGrupo());
            ps.setString(6, a.getCarrera());
            ps.setString(7, a.getEspacioAcademico());
            ps.setInt(8, a.getSemestre());
            ps.setDouble(9, a.getSaldo());
            ps.setString(10, a.getEstatus());
            ps.setInt(11, a.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new SQLException("Error al actualizar alumno: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(null, ps, con);
        }
    }

    @Override
    public boolean eliminar(Integer id) throws Exception {
        String sqlCredenciales = "DELETE FROM credenciales WHERE idPersona = ?";
        String sqlPersona = "DELETE FROM persona WHERE idPersona = ?";
        Connection con = null;
        PreparedStatement psCred = null;
        PreparedStatement psPer = null;

        try {
            con = dataSource.getConnection();
            con.setAutoCommit(false);

            // Se eliminan primero las credenciales asociadas para respetar integridad referencial
            psCred = con.prepareStatement(sqlCredenciales);
            psCred.setInt(1, id);
            psCred.executeUpdate();

            psPer = con.prepareStatement(sqlPersona);
            psPer.setInt(1, id);
            int filas = psPer.executeUpdate();

            con.commit();
            return filas > 0;
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new SQLException("Error al eliminar alumno: " + e.getMessage(), e);
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException ignored) {
                }
            }
            cerrarRecursos(null, psCred, null);
            cerrarRecursos(null, psPer, con);
        }
    }

    // Uso explícito de Colecciones e Iteradores (Tema A18) para filtrar alumnos en memoria
    public List<Alumno> filtrarConIterador(String busqueda, String estatusFiltro) throws Exception {
        List<Alumno> alumnos = listarTodos();
        Iterator<Alumno> iterador = alumnos.iterator();

        String texto = (busqueda != null) ? busqueda.trim().toLowerCase() : "";
        String estatus = (estatusFiltro != null) ? estatusFiltro.trim() : "";

        while (iterador.hasNext()) {
            Alumno actual = iterador.next();
            boolean coincideTexto = texto.isEmpty()
                    || actual.getNombreCompleto().toLowerCase().contains(texto)
                    || (actual.getGrupo() != null && actual.getGrupo().toLowerCase().contains(texto))
                    || (actual.getCarrera() != null && actual.getCarrera().toLowerCase().contains(texto))
                    || String.valueOf(actual.getId()).equals(texto);

            boolean coincideEstatus = estatus.isEmpty()
                    || estatus.equalsIgnoreCase("TODOS")
                    || (actual.getEstatus() != null && actual.getEstatus().equalsIgnoreCase(estatus));

            if (!coincideTexto || !coincideEstatus) {
                iterador.remove();
            }
        }
        return alumnos;
    }

    // Caso de Uso 5: Trámite de cambio de carrera / facultad validando que no existan adeudos
    public boolean cambiarCarrera(int idAlumno, String nuevaCarrera, String nuevoEspacio, String nuevoGrupo) throws Exception {
        Alumno alumno = buscarPorId(idAlumno);
        if (alumno == null) {
            throw new IllegalArgumentException("No se encontró el alumno con ID " + idAlumno + ".");
        }
        if (!"Inscrito".equalsIgnoreCase(alumno.getEstatus())) {
            throw new IllegalStateException("Trámite bloqueado: el alumno tiene inscripción/adeudo pendiente (Estatus: " + alumno.getEstatus() + ").");
        }

        String sql = "UPDATE persona SET carrera = ?, espacio_academico = ?, grupo = ? WHERE idPersona = ?";
        Connection con = null;
        PreparedStatement ps = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, nuevaCarrera);
            ps.setString(2, nuevoEspacio);
            ps.setString(3, nuevoGrupo);
            ps.setInt(4, idAlumno);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new SQLException("Error al procesar cambio de carrera: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(null, ps, con);
        }
    }

    private Alumno mapearAlumno(ResultSet rs) throws SQLException {
        Alumno a = new Alumno();
        a.setId(rs.getInt("idPersona"));
        a.setNombre(rs.getString("Nombre"));
        a.setApPaterno(rs.getString("Ap_paterno"));
        a.setApMaterno(rs.getString("Ap_materno"));
        a.setEdad(rs.getInt("Edad"));
        a.setGrupo(rs.getString("grupo"));
        a.setCarrera(rs.getString("carrera"));
        a.setEspacioAcademico(rs.getString("espacio_academico"));
        a.setSemestre(rs.getInt("Semestre"));
        a.setSaldo(rs.getDouble("saldo"));
        a.setEstatus(rs.getString("estatus"));
        a.setIdNCuenta(rs.getInt("idNCuenta"));
        return a;
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