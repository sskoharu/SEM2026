package org.app.schemas.session_object;

import lombok.Data;
import org.app.schemas.session_object.Params.type_3.IncumplimientoContratoParams;
import org.app.schemas.session_object.Params.type_4.InformacionFacturacionParams;
import org.app.schemas.session_object.Params.type_2.ServicioTecnicoParams;
import org.app.schemas.session_object.Params.type_1.ViciosOcultosParams;
import org.app.schemas.session_object.Params.type_5.ServiciosPublicosParams;
import org.app.schemas.session_object.Params.type_6.Contratos;
import org.app.schemas.session_object.Params.type_7.FinancierosCreditosCobranzasParams;
import org.app.schemas.session_object.Params.type_8.PracticasAbusivasParams;


@Data
public class SpecificClaimParameters {

    // Solo se llena el objeto correspondiente al ClaimType activo en la sesión
    private ViciosOcultosParams viciosOcultos;
    private ServicioTecnicoParams servicioTecnico;
    private IncumplimientoContratoParams incumplimientoContrato;
    private InformacionFacturacionParams informacionFacturacion;
    private ServiciosPublicosParams serviciosPublicos;
    private Contratos contratos;
    private FinancierosCreditosCobranzasParams serviciosFinancieros;
    private PracticasAbusivasParams practicasAbusivas;


}