package com.control.escolar.model;

public class CuentaUniversidad {

    private int idNCuenta;
    private double costoInscripcion;
    private double saldoAcumulado;

    public CuentaUniversidad() {
    }

    public CuentaUniversidad(int idNCuenta, double costoInscripcion, double saldoAcumulado) {
        this.idNCuenta = idNCuenta;
        this.costoInscripcion = costoInscripcion;
        this.saldoAcumulado = saldoAcumulado;
    }

    public int getIdNCuenta() {
        return idNCuenta;
    }

    public void setIdNCuenta(int idNCuenta) {
        this.idNCuenta = idNCuenta;
    }

    public double getCostoInscripcion() {
        return costoInscripcion;
    }

    public void setCostoInscripcion(double costoInscripcion) {
        this.costoInscripcion = costoInscripcion;
    }

    public double getSaldoAcumulado() {
        return saldoAcumulado;
    }

    public void setSaldoAcumulado(double saldoAcumulado) {
        this.saldoAcumulado = saldoAcumulado;
    }
}