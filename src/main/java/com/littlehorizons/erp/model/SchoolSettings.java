package com.littlehorizons.erp.model;

import java.util.Map;

/** School data printed on every invoice (table "configuracion_colegio"). */
public record SchoolSettings(String legalName, String nit, String address, String phone, String email,
                             String dianResolution, String invoicePrefix, int rangeFrom, int rangeTo, int vatPercent) {

    public static SchoolSettings fromMap(Map<String, String> values) {
        return new SchoolSettings(values.get("razon_social"), values.get("nit"), values.get("direccion"),
                values.get("telefono"), values.get("correo_electronico"), values.get("resolucion_dian"),
                values.getOrDefault("prefijo_factura", "LH"),
                Integer.parseInt(values.getOrDefault("rango_autorizado_desde", "1")),
                Integer.parseInt(values.getOrDefault("rango_autorizado_hasta", "5000")),
                Integer.parseInt(values.getOrDefault("porcentaje_iva", "19")));
    }
}
