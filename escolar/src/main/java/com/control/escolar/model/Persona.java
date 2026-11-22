package com.control.escolar.model;

public abstract class Persona {

    protected int id;
    protected String nombre;
    protected String apPaterno;
    protected String apMaterno;
    protected String carrera;
    protected String grupo;

    public Persona() {
    }

    public Persona(int id, String nombre, String apPaterno, String apMaterno, String carrera, String grupo) {
        this.id = id;
        this.nombre = nombre;
        this.apPaterno = apPaterno;
        this.apMaterno = apMaterno;
        this.carrera = carrera;
        this.grupo = grupo;
    }

    public String getNombreCompleto() {
        String n = (nombre != null) ? nombre : "";
        String p = (apPaterno != null) ? apPaterno : "";
        String m = (apMaterno != null) ? apMaterno : "";
        return (n + " " + p + " " + m).trim();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApPaterno() {
        return apPaterno;
    }

    public void setApPaterno(String apPaterno) {
        this.apPaterno = apPaterno;
    }

    public String getApMaterno() {
        return apMaterno;
    }

    public void setApMaterno(String apMaterno) {
        this.apMaterno = apMaterno;
    }

    public String getCarrera() {
        return carrera;
    }

    public void setCarrera(String carrera) {
        this.carrera = carrera;
    }

    public String getGrupo() {
        return grupo;
    }

    public void setGrupo(String grupo) {
        this.grupo = grupo;
    }
}