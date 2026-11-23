package com.control.escolar.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.control.escolar.service.PasswordUtil;

@Repository
public class AuthDAO {

    private static final String HASH_ADMIN = PasswordUtil.encriptarSHA256("Admin1234");

    private final DataSource dataSource;

    public AuthDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean validarAdministrador(String passwordPlano) {
        if (passwordPlano == null || passwordPlano.isBlank()) {
            return false;
        }
        return HASH_ADMIN.equals(PasswordUtil.encriptarSHA256(passwordPlano.trim()));
    }

    public Integer autenticarAlumno(String passwordPlano) throws Exception {
        String hash = PasswordUtil.encriptarSHA256(passwordPlano.trim().toUpperCase());
        String sql = "SELECT idPersona FROM credenciales WHERE password_hash = ? LIMIT 1";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, hash);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("idPersona");
            }
            return null;
        } catch (SQLException e) {
            throw new SQLException("Error al validar credenciales de alumno: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(rs, ps, con);
        }
    }

    public Integer autenticarDocente(String passwordPlano) throws Exception {
        String hash = PasswordUtil.encriptarSHA256(passwordPlano.trim().toUpperCase());
        String sql = "SELECT idDocente FROM credenciales_docente WHERE password_hash = ? LIMIT 1";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            con = dataSource.getConnection();
            ps = con.prepareStatement(sql);
            ps.setString(1, hash);
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("idDocente");
            }
            return null;
        } catch (SQLException e) {
            throw new SQLException("Error al validar credenciales de docente: " + e.getMessage(), e);
        } finally {
            cerrarRecursos(rs, ps, con);
        }
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