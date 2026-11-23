package com.control.escolar.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;

import com.control.escolar.dao.AlumnoDAO;
import com.control.escolar.model.Alumno;

@Service
public class RespaldoLocalService {

    private static final String ARCHIVO_RESPALDO
            = "respaldo_alumnos.txt";

    // Guarda un alumno temporalmente en disco si MySQL no responde
    public void guardarRespaldoTemporal(Alumno a)
            throws IOException {
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream(
                                    ARCHIVO_RESPALDO, true),
                            StandardCharsets.UTF_8));
            String linea = String.join("|",
                    limpiar(a.getNombre()),
                    limpiar(a.getApPaterno()),
                    limpiar(a.getApMaterno()),
                    String.valueOf(a.getEdad()),
                    limpiar(a.getGrupo()),
                    limpiar(a.getCarrera()),
                    limpiar(a.getEspacioAcademico()),
                    String.valueOf(a.getSemestre()),
                    String.valueOf(a.getSaldo()),
                    limpiar(a.getEstatus() != null
                            ? a.getEstatus()
                            : "No inscrito")
            );
            writer.write(linea);
            writer.newLine();
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    // Cuenta cuantos registros estan pendientes por sincronizar
    public int contarPendientes() {
        File archivo = new File(ARCHIVO_RESPALDO);
        if (!archivo.exists()) {
            return 0;
        }
        int contador = 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(archivo),
                            StandardCharsets.UTF_8));
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    contador++;
                }
            }
        } catch (IOException ignored) {
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return contador;
    }

    // Lee el archivo local, sube los registros a MySQL
    // usando un Iterator y borra el archivo temporal
    public int sincronizarConBaseDeDatos(
            AlumnoDAO alumnoDAO) throws Exception {
        File archivo = new File(ARCHIVO_RESPALDO);
        if (!archivo.exists()) {
            return 0;
        }

        List<Alumno> pendientes = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(archivo),
                            StandardCharsets.UTF_8));
            String linea;
            while ((linea = reader.readLine()) != null) {
                if (linea.trim().isEmpty()) {
                    continue;
                }
                String[] partes = linea.split("\\|");
                if (partes.length >= 10) {
                    Alumno a = new Alumno();
                    a.setNombre(partes[0]);
                    a.setApPaterno(partes[1]);
                    a.setApMaterno(partes[2]);
                    a.setEdad(
                            Integer.parseInt(partes[3]));
                    a.setGrupo(partes[4]);
                    a.setCarrera(partes[5]);
                    a.setEspacioAcademico(partes[6]);
                    a.setSemestre(
                            Integer.parseInt(partes[7]));
                    a.setSaldo(
                            Double.parseDouble(partes[8]));
                    a.setEstatus(partes[9]);
                    a.setIdNCuenta(1);
                    pendientes.add(a);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        int sincronizados = 0;
        Iterator<Alumno> it = pendientes.iterator();
        while (it.hasNext()) {
            Alumno alumno = it.next();
            alumnoDAO.guardar(alumno);
            sincronizados++;
            it.remove();
        }

        // Si todos se subieron, se elimina el archivo temporal
        if (pendientes.isEmpty() && archivo.exists()) {
            archivo.delete();
        }

        return sincronizados;
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("|", " ").trim();
    }
}