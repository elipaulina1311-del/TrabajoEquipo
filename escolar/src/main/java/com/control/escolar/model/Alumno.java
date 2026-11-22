package com.control.escolar.model;

public class Alumno extends Persona {

    private int edad;
    private String espacioAcademico;
    private int semestre;
    private double saldo;
    private String estatus;
    private int idNCuenta;

    public Alumno() {
        super();
        this.estatus = "No inscrito";
        this.idNCuenta = 1;
    }

    public Alumno(int id, String nombre, String apPaterno, String apMaterno, int edad,
                  String grupo, String carrera, String espacioAcademico, int semestre,
                  double saldo, String estatus, int idNCuenta) {
        super(id, nombre, apPaterno, apMaterno, carrera, grupo);
        this.edad = edad;
        this.espacioAcademico = espacioAcademico;
        this.semestre = semestre;
        this.saldo = saldo;
        this.estatus = estatus;
        this.idNCuenta = idNCuenta;
    }

    public int getEdad() {
        return edad;
    }

    public void setEdad(int edad) {
        this.edad = edad;
    }

    public String getEspacioAcademico() {
        return espacioAcademico;
    }

    public void setEspacioAcademico(String espacioAcademico) {
        this.espacioAcademico = espacioAcademico;
    }

    public int getSemestre() {
        return semestre;
    }

    public void setSemestre(int semestre) {
        this.semestre = semestre;
    }

    public double getSaldo() {
        return saldo;
    }

    public void setSaldo(double saldo) {
        this.saldo = saldo;
    }

    public String getEstatus() {
        return estatus;
    }

    public void setEstatus(String estatus) {
        this.estatus = estatus;
    }

    public int getIdNCuenta() {
        return idNCuenta;
    }

    public void setIdNCuenta(int idNCuenta) {
        this.idNCuenta = idNCuenta;
    }
}