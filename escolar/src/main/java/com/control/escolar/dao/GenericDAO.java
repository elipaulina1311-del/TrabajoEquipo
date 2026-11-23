package com.control.escolar.dao;

import java.util.List;

public interface GenericDAO<T, ID> {

    void guardar(T entidad) throws Exception;

    T buscarPorId(ID id) throws Exception;

    List<T> listarTodos() throws Exception;

    boolean actualizar(T entidad) throws Exception;

    boolean eliminar(ID id) throws Exception;
}