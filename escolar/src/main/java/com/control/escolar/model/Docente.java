package com.control.escolar.model;

public class Docente extends Persona {

    private int idAsignacion;
    private String numeroEmpleado;
    private String materia;

    public Docente() {
        super();
    }

    public Docente(int idAsignacion, int idDocente, String nombre, String apPaterno,
                   String apMaterno, String numeroEmpleado, String materia,
                   String carrera, String grupo) {
        super(idDocente, nombre, apPaterno, apMaterno, carrera, grupo);
        this.idAsignacion = idAsignacion;
        this.numeroEmpleado = numeroEmpleado;
        this.materia = materia;
    }

    public int getIdAsignacion() {
        return idAsignacion;
    }

    public void setIdAsignacion(int idAsignacion) {
        this.idAsignacion = idAsignacion;
    }

    public String getNumeroEmpleado() {
        return numeroEmpleado;
    }

    public void setNumeroEmpleado(String numeroEmpleado) {
        this.numeroEmpleado = numeroEmpleado;
    }

    public String getMateria() {
        return materia;
    }

    public void setMateria(String materia) {
        this.materia = materia;
    }
}