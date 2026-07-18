package com.medinaparra.freecadandroid.ui

import android.opengl.GLSurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.medinaparra.freecadandroid.nativebridge.FreeCadNative
import com.medinaparra.freecadandroid.nativebridge.NativeSceneMesh
import com.medinaparra.freecadandroid.nativebridge.MacroExecutionResult
import com.medinaparra.freecadandroid.viewer.CadGLSurfaceView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.medinaparra.freecadandroid.viewmodel.CadViewModel
import com.medinaparra.freecadandroid.importer.ImportState

// State representer for CAD Objects in UI
data class CadObjectState(
    val id: Long,
    val name: String,
    val type: String, // "BOX" or "CYLINDER"
    val isVisible: Boolean = true,
    val tx: Float = 0f,
    val ty: Float = 0f,
    val tz: Float = 0f,
    val dim1: Float = 40f, // Box length / Cylinder radius
    val dim2: Float = 40f, // Box width / Cylinder height
    val dim3: Float = 40f  // Box height
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: CadViewModel = viewModel()) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Active document states from CadViewModel
    val activeDocId by viewModel.activeDocId.collectAsState()
    val activeDocName by viewModel.activeDocName.collectAsState()
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val objectsList by viewModel.objectsList.collectAsState()
    val selectedObjectId by viewModel.selectedObjectId.collectAsState()
    val pythonConsoleOutput by viewModel.pythonConsoleOutput.collectAsState()
    val nativeCapabilities by viewModel.nativeCapabilities.collectAsState()
    val importState by viewModel.importState.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }
    var qualityLevel by remember { mutableStateOf("Alta (24 segs)") }
    var maxExecutionTimeSecs by remember { mutableStateOf(5) }

    // Panel visibility states
    var isObjectTreeVisible by remember { mutableStateOf(true) }
    var isPropertiesPanelVisible by remember { mutableStateOf(true) }

    // local macro state variable kept for UI text editing
    var macroInputCode by remember { mutableStateOf(
        """
        # -*- coding: utf-8 -*-
        '''
        Macro FreeCAD - Polea motriz 145-CV-012 Pos. 6 / OT 2026-1633

        Fuentes modeladas:
          - Plano DPRO-4400203061-70112-200ME-008, conjunto posicion 6.
          - Informe de preservacion OT 2026-1633, Minera DGM.
          - Registro fotografico de recepcion/taller.

        Alcance:
          - Tambor, discos, cubos, eje escalonado y chaveteros segun plano.
          - Revestimiento SBR de 25 mm con morfologia romboidal discretizada.
          - Soportes SKF SNL 3268 simplificados y reemplazables.
          - Accesorios observados en taller (backstop y acoplamientos) como grupos
            independientes; no deben usarse para fabricar sin levantamiento adicional.

        Uso:
          1. Abra FreeCAD y ejecute esta macro.
          2. Edite las constantes de CONFIGURACION si desea ocultar detalles pesados.
          3. Para modificar cotas, cambie el diccionario P y vuelva a ejecutar.

        Sistema de coordenadas:
          - Eje de la polea: X.
          - Centro del tambor: X = 1931 mm desde el extremo izquierdo del eje.
          - Centro del eje: Y = 0, Z = 0.
        '''

        import math
        import os

        import FreeCAD as App
        import Part

        try:
            import FreeCADGui as Gui
        except Exception:
            Gui = None


        # =============================================================================
        # CONFIGURACION DE USUARIO
        # =============================================================================

        NOMBRE_DOCUMENTO = "Polea_Motriz_145CV012_Pos6_OT1633"

        INCLUIR_SOPORTES_SIMPLIFICADOS = True
        INCLUIR_BACKSTOP_SIMPLIFICADO = True
        INCLUIR_ACOPLAMIENTOS_FOTO = True
        INCLUIR_REVESTIMIENTO_ROMBOIDAL = True

        # El patron completo genera 1.120 placas ceramicas en un solo Compound.
        # Para equipos lentos use MODO_REVESTIMIENTO_LIGERO = True.
        MODO_REVESTIMIENTO_LIGERO = False

        # No se guarda automaticamente para no sobrescribir archivos del usuario.
        GUARDAR_FCSTD = False
        RUTA_FCSTD = os.path.join(os.path.expanduser("~"), NOMBRE_DOCUMENTO + ".FCStd")


        # =============================================================================
        # PARAMETROS PRINCIPALES [mm]
        # =============================================================================

        P = {
            # Disposicion general
            "eje_largo_total": 4369.0,
            "centro_tambor_desde_izq": 1931.0,
            "manto_largo": 1981.0,
            "dist_centros_soportes": 2718.0,
            "dist_centros_rodamientos_informe": 2653.0,

            # Tambor y revestimiento
            "manto_diametro_ext": 1219.0,
            "manto_espesor_min": 25.0,
            "revestimiento_espesor": 25.0,
            "revestimiento_diametro_ext": 1269.0,
            "placa_ceramica_altura": 5.0,
            "disco_diametro": 1240.0,
            "disco_espesor": 40.0,
            "disco_agujero": 465.0,

            # Eje y elementos de fijacion
            "eje_diametro_extremos": 320.0,
            "eje_diametro_bikon": 380.0,
            "eje_diametro_central": 390.0,
            "bikon_diametro_exterior": 465.0,
            "chaveta_ancho": 63.0,
            "chavetero_profundidad": 20.0,
            "chavetero_largo_izq": 292.0,
            "chavetero_largo_der": 813.0,

            # Soporte SKF SNL 3268 - envolvente informada / simplificada
            "soporte_largo_transversal_L": 1040.0,
            "soporte_ancho_cuerpo_J": 820.0,
            "soporte_espesor_axial_J1": 220.0,
            "soporte_altura_eje_M": 360.0,
            "soporte_perno": 36.0,
            "soporte_cota_S": 30.0,

            # Accesorios fotografiados / informe
            "acoplamiento_diametro_aprox": 565.0,
            "acoplamiento_largo_aprox": 240.0,
            "backstop_ancho": 880.0,
            "backstop_alto": 820.0,
            "backstop_espesor": 160.0,
        }


        # =============================================================================
        # UTILIDADES GEOMETRICAS Y GRAFICAS
        # =============================================================================

        XDIR = App.Vector(1, 0, 0)
        ZDIR = App.Vector(0, 0, 1)

        COLOR_ACERO = (0.66, 0.69, 0.73)
        COLOR_ACERO_OSCURO = (0.32, 0.35, 0.39)
        COLOR_NARANJO = (1.00, 0.32, 0.03)
        COLOR_NARANJO_OSCURO = (0.68, 0.16, 0.02)
        COLOR_GOMA = (0.055, 0.055, 0.065)
        COLOR_CERAMICO = (0.12, 0.12, 0.14)
        COLOR_AZUL = (0.04, 0.30, 0.68)
        COLOR_NEGRO = (0.025, 0.025, 0.03)
        COLOR_BRONCE = (0.35, 0.16, 0.055)


        def cilindro_x(radio, largo, x0):
            return Part.makeCylinder(float(radio), float(largo), App.Vector(float(x0), 0, 0), XDIR)


        def anillo_x(r_ext, r_int, largo, x0):
            exterior = cilindro_x(r_ext, largo, x0)
            interior = cilindro_x(r_int, largo + 2.0, x0 - 1.0)
            return exterior.cut(interior)


        def limpiar(shape):
            try:
                return shape.removeSplitter()
            except Exception:
                return shape


        def fusionar(shapes):
            shapes = [s for s in shapes if s is not None]
            if not shapes:
                return Part.Shape()
            resultado = shapes[0]
            for shape in shapes[1:]:
                resultado = resultado.fuse(shape)
            return limpiar(resultado)


        def agregar_shape(grupo, nombre, etiqueta, shape, color, transferencia=0):
            obj = doc.addObject("Part::Feature", nombre)
            obj.Label = etiqueta
            obj.Shape = shape
            grupo.addObject(obj)
            if Gui:
                obj.ViewObject.ShapeColor = color
                obj.ViewObject.LineColor = tuple(max(0.0, c * 0.55) for c in color)
                obj.ViewObject.Transparency = int(transferencia)
            return obj


        def prisma_hexagonal_z(radio, altura, x, y, z0):
            puntos = []
            for i in range(6):
                ang = math.radians(60.0 * i + 30.0)
                puntos.append(App.Vector(x + radio * math.cos(ang), y + radio * math.sin(ang), z0))
            puntos.append(puntos[0])
            cara = Part.Face(Part.makePolygon(puntos))
            return cara.extrude(App.Vector(0, 0, altura))


        def caja_tangencial_x(x0, largo, ancho_tangencial, radio0, altura_radial, angulo_deg):
            '''Diente rectangular radial, repetido alrededor del eje X.'''
            caja = Part.makeBox(largo, ancho_tangencial, altura_radial,
                                App.Vector(x0, -ancho_tangencial / 2.0, radio0))
            caja.rotate(App.Vector(0, 0, 0), XDIR, angulo_deg)
            return caja


        def placa_rombo_tangente(x, angulo, radio, largo_axial, ancho_arco, altura):
            '''Placa romboidal tangente al cilindro; evita booleanas pesadas.'''
            c = math.cos(angulo)
            s = math.sin(angulo)
            centro = App.Vector(x, radio * c, radio * s)
            ex = App.Vector(1, 0, 0)
            et = App.Vector(0, -s, c)
            er = App.Vector(0, c, s)

            p1 = centro - ex * (largo_axial / 2.0)
            p2 = centro + et * (ancho_arco / 2.0)
            p3 = centro + ex * (largo_axial / 2.0)
            p4 = centro - et * (ancho_arco / 2.0)
            alambre = Part.makePolygon([p1, p2, p3, p4, p1])
            return Part.Face(alambre).extrude(er * altura)


        def redondeado_extruido_x(x0, espesor, ancho_y, alto_z, radio_esquina):
            '''Prisma de seccion rectangular redondeada, extruido sobre X.'''
            r = min(radio_esquina, ancho_y / 2.0, alto_z / 2.0)
            formas = [
                Part.makeBox(espesor, ancho_y - 2.0 * r, alto_z,
                             App.Vector(x0, -ancho_y / 2.0 + r, -alto_z / 2.0)),
                Part.makeBox(espesor, ancho_y, alto_z - 2.0 * r,
                             App.Vector(x0, -ancho_y / 2.0, -alto_z / 2.0 + r)),
            ]
            for yy in (-ancho_y / 2.0 + r, ancho_y / 2.0 - r):
                for zz in (-alto_z / 2.0 + r, alto_z / 2.0 - r):
                    formas.append(Part.makeCylinder(r, espesor, App.Vector(x0, yy, zz), XDIR))
            return fusionar(formas)


        # =============================================================================
        # DOCUMENTO Y ARBOL DE MODELO
        # =============================================================================

        if App.ActiveDocument and App.ActiveDocument.Name == NOMBRE_DOCUMENTO:
            App.closeDocument(NOMBRE_DOCUMENTO)

        try:
            doc = App.newDocument(NOMBRE_DOCUMENTO, "Polea Motriz 145CV012 Pos.6 - OT1633")
        except TypeError:
            doc = App.newDocument(NOMBRE_DOCUMENTO)
            doc.Label = "Polea Motriz 145CV012 Pos.6 - OT1633"

        g_ref = doc.addObject("App::DocumentObjectGroup", "G00_Parametros_y_Referencias")
        g_ref.Label = "00 - Parametros y referencias"
        g_eje = doc.addObject("App::DocumentObjectGroup", "G01_Eje")
        g_eje.Label = "01 - Eje mecanizado"
        g_tambor = doc.addObject("App::DocumentObjectGroup", "G02_Tambor")
        g_tambor.Label = "02 - Manto, discos y cubos"
        g_revest = doc.addObject("App::DocumentObjectGroup", "G03_Revestimiento")
        g_revest.Label = "03 - Revestimiento romboidal"
        g_soportes = doc.addObject("App::DocumentObjectGroup", "G04_Soportes")
        g_soportes.Label = "04 - Soportes SKF SNL 3268 simplificados"
        g_accesorios = doc.addObject("App::DocumentObjectGroup", "G05_Accesorios")
        g_accesorios.Label = "05 - Accesorios observados en taller"


        # Hoja visible con trazabilidad de las cotas.
        tabla = doc.addObject("Spreadsheet::Sheet", "Parametros")
        tabla.Label = "Parametros y fuentes"
        g_ref.addObject(tabla)
        filas = [
            ("Parametro", "Valor", "Fuente / criterio"),
            ("Largo total eje", "4369 mm", "Plano DPRO-...-008"),
            ("Largo manto", "1981 mm", "Plano + control dimensional"),
            ("Diametro manto metalico", "1219 mm", "Plano; taller registra aprox. 1218 mm"),
            ("Diametro sobre revestimiento", "1269 mm", "Plano"),
            ("Espesor revestimiento", "25 mm", "Plano + informe de taller"),
            ("Espesor minimo manto", "25 mm", "Plano"),
            ("Distancia centros soportes", "2718 mm", "Plano + informe"),
            ("Distancia centros rodamientos", "2653 mm", "Informe de taller"),
            ("Eje extremos", "320 mm", "Plano + metrologia 319,81 a 319,94"),
            ("Asiento BIKON", "380 mm", "BIKON 2006 380x465"),
            ("Cuerpo central eje", "390 mm", "Plano"),
            ("Chaveteros", "63 P9 x 20 mm", "Seccion B del plano"),
            ("Soportes", "SKF SNL 3268", "Plano + informe"),
            ("Envolvente soporte", "L=1040; J=820; J1=220; M=360 mm", "Informe; geometria simplificada"),
            ("Backstop", "FALK 1165 NRTA", "Informe OT-1633; geometria simplificada"),
            ("Revestimiento taller", "84,3 Shore A", "Informe; aceptado por cliente"),
            ("Advertencia", "NO FABRICACION", "Validar accesorios y soportes con levantamiento final"),
        ]
        for i, fila in enumerate(filas, 1):
            tabla.set("A%d" % i, fila[0])
            tabla.set("B%d" % i, fila[1])
            tabla.set("C%d" % i, fila[2])
        tabla.setColumnWidth("A", 230)
        tabla.setColumnWidth("B", 260)
        tabla.setColumnWidth("C", 420)
        tabla.setStyle("A1:C1", "bold", "add")
        tabla.setBackground("A1:C1", (1.0, 0.65, 0.18))
        tabla.setBackground("A18:C18", (1.0, 0.82, 0.40))

        nota = doc.addObject("App::FeaturePython", "Nota_Alcance")
        nota.Label = "LEER - Alcance del modelo"
        nota.addProperty("App::PropertyString", "Plano", "Trazabilidad")
        nota.addProperty("App::PropertyString", "Informe", "Trazabilidad")
        nota.addProperty("App::PropertyString", "Alcance", "Trazabilidad")
        nota.addProperty("App::PropertyString", "Advertencia", "Trazabilidad")
        nota.Plano = "DPRO-4400203061-70112-200ME-008 / Posicion 6"
        nota.Informe = "OT 2026-1633 - Minera DGM - Rev.0"
        nota.Alcance = "Polea detallada; soportes, backstop y acoplamientos simplificados"
        nota.Advertencia = "Modelo de levantamiento. No fabricar accesorios sin verificacion dimensional."
        g_ref.addObject(nota)


        # =============================================================================
        # 01 - EJE ESCALONADO Y CHAVETEROS
        # =============================================================================

        eje_L = P["eje_largo_total"]
        x_centro_tambor = P["centro_tambor_desde_izq"]
        x_manto_izq = x_centro_tambor - P["manto_largo"] / 2.0
        x_manto_der = x_centro_tambor + P["manto_largo"] / 2.0

        # Los hombros exteriores se reconstruyen del plano de disposicion general.
        x_bikon_izq = x_manto_izq - 180.0
        x_bikon_der = x_manto_der + 180.0

        segmentos_eje = [
            cilindro_x(P["eje_diametro_extremos"] / 2.0, x_bikon_izq, 0.0),
            cilindro_x(P["eje_diametro_bikon"] / 2.0, x_manto_izq - x_bikon_izq, x_bikon_izq),
            cilindro_x(P["eje_diametro_central"] / 2.0, P["manto_largo"], x_manto_izq),
            cilindro_x(P["eje_diametro_bikon"] / 2.0, x_bikon_der - x_manto_der, x_manto_der),
            cilindro_x(P["eje_diametro_extremos"] / 2.0, eje_L - x_bikon_der, x_bikon_der),
        ]
        eje_shape = fusionar(segmentos_eje)

        kw = P["chaveta_ancho"]
        kd = P["chavetero_profundidad"]
        r_extremo = P["eje_diametro_extremos"] / 2.0
        corte_izq = Part.makeBox(P["chavetero_largo_izq"], kw, kd + 3.0,
                                 App.Vector(0, -kw / 2.0, r_extremo - kd))
        corte_der = Part.makeBox(P["chavetero_largo_der"], kw, kd + 3.0,
                                 App.Vector(eje_L - P["chavetero_largo_der"], -kw / 2.0,
                                            r_extremo - kd))
        eje_shape = limpiar(eje_shape.cut(corte_izq.fuse(corte_der)))
        agregar_shape(g_eje, "Eje_Escalonado", "Eje 42CrMo4+QT / 4140", eje_shape, COLOR_ACERO)

        # Chavetas como referencia visual; pueden ocultarse para ver los alojamientos.
        chaveta_izq = Part.makeBox(P["chavetero_largo_izq"] - 12.0, kw - 1.0, kd,
                                   App.Vector(6.0, -(kw - 1.0) / 2.0, r_extremo - kd + 0.5))
        chaveta_der = Part.makeBox(P["chavetero_largo_der"] - 12.0, kw - 1.0, kd,
                                   App.Vector(eje_L - P["chavetero_largo_der"] + 6.0,
                                              -(kw - 1.0) / 2.0, r_extremo - kd + 0.5))
        obj_chavetas = agregar_shape(g_eje, "Chavetas_Referencia", "Chavetas 63 P9 (referencia)",
                                     Part.makeCompound([chaveta_izq, chaveta_der]), COLOR_ACERO_OSCURO)
        if Gui:
            obj_chavetas.ViewObject.Visibility = False


        # =============================================================================
        # 02 - MANTO, DISCOS, CUBOS Y ELEMENTOS BIKON
        # =============================================================================

        r_manto = P["manto_diametro_ext"] / 2.0
        r_manto_int = r_manto - P["manto_espesor_min"]
        manto = anillo_x(r_manto, r_manto_int, P["manto_largo"], x_manto_izq)
        agregar_shape(g_tambor, "Manto_Metalico", "Manto S355J2 - espesor minimo 25 mm",
                      manto, COLOR_NARANJO)

        r_disco = P["disco_diametro"] / 2.0
        r_agujero = P["disco_agujero"] / 2.0
        t_disco = P["disco_espesor"]

        disco_izq = anillo_x(r_disco, r_agujero, t_disco, x_manto_izq - t_disco / 2.0)
        disco_der = anillo_x(r_disco, r_agujero, t_disco, x_manto_der - t_disco / 2.0)
        agregar_shape(g_tambor, "Discos_Laterales", "Discos laterales tipo Deckel",
                      Part.makeCompound([disco_izq, disco_der]), COLOR_NARANJO)

        # Cubos con transicion conica reconstruidos de las fotografias.
        cubo_izq = Part.makeCone(270.0, 380.0, 95.0,
                                 App.Vector(x_manto_izq - t_disco / 2.0 - 95.0, 0, 0), XDIR)
        cubo_der = Part.makeCone(380.0, 270.0, 95.0,
                                 App.Vector(x_manto_der + t_disco / 2.0, 0, 0), XDIR)
        taladro_cubo_izq = cilindro_x(r_agujero, 99.0, x_manto_izq - t_disco / 2.0 - 97.0)
        taladro_cubo_der = cilindro_x(r_agujero, 99.0, x_manto_der + t_disco / 2.0 - 2.0)
        cubos = Part.makeCompound([cubo_izq.cut(taladro_cubo_izq), cubo_der.cut(taladro_cubo_der)])
        agregar_shape(g_tambor, "Cubos_Conicos", "Cubos y transiciones de discos", cubos, COLOR_NARANJO)

        # Aros de fijacion BIKON 2006 380x465, representacion morfologica.
        r_bikon_ext = P["bikon_diametro_exterior"] / 2.0
        r_bikon_int = P["eje_diametro_bikon"] / 2.0
        bikon_izq = anillo_x(r_bikon_ext, r_bikon_int, 36.0, x_manto_izq - 48.0)
        bikon_der = anillo_x(r_bikon_ext, r_bikon_int, 36.0, x_manto_der + 12.0)
        agregar_shape(g_tambor, "BIKON_2006", "Elementos BIKON 2006 380x465",
                      Part.makeCompound([bikon_izq, bikon_der]), COLOR_ACERO_OSCURO)


        # =============================================================================
        # 03 - REVESTIMIENTO SBR Y PLACAS ROMBOIDALES
        # =============================================================================

        r_revest = P["revestimiento_diametro_ext"] / 2.0
        revestimiento = anillo_x(r_revest, r_manto, P["manto_largo"], x_manto_izq)
        agregar_shape(g_revest, "Base_SBR", "Revestimiento SBR 25 mm - 84,3 Shore A medido",
                      revestimiento, COLOR_GOMA)

        if INCLUIR_REVESTIMIENTO_ROMBOIDAL:
            if MODO_REVESTIMIENTO_LIGERO:
                n_axial = 18
                n_circ = 28
            else:
                n_axial = 28
                n_circ = 40

            margen_axial = 30.0
            paso_x = (P["manto_largo"] - 2.0 * margen_axial) / float(n_axial - 1)
            paso_arco = 2.0 * math.pi * r_revest / float(n_circ)
            largo_rombo = paso_x * 0.84
            ancho_rombo = paso_arco * 0.82
            # Se retrasa el plano tangente para que las cuatro puntas del rombo
            # intersecten levemente la base cilindrica y no queden flotando.
            radio_base_placa = math.sqrt(max(1.0, r_revest ** 2 - (ancho_rombo / 2.0) ** 2)) - 0.5
            placas = []
            for ia in range(n_axial):
                x = x_manto_izq + margen_axial + ia * paso_x
                desfase = (0.5 * math.pi * 2.0 / n_circ) if (ia % 2) else 0.0
                for ic in range(n_circ):
                    ang = 2.0 * math.pi * ic / float(n_circ) + desfase
                    placas.append(placa_rombo_tangente(
                        x, ang, radio_base_placa, largo_rombo, ancho_rombo,
                        P["placa_ceramica_altura"]
                    ))
            obj_placas = agregar_shape(g_revest, "Placas_Romboidales",
                                       "Morfologia romboidal (%d placas)" % len(placas),
                                       Part.makeCompound(placas), COLOR_CERAMICO)
            obj_placas.addProperty("App::PropertyInteger", "CantidadPlacas", "Revestimiento")
            obj_placas.addProperty("App::PropertyString", "Criterio", "Revestimiento")
            obj_placas.CantidadPlacas = len(placas)
            obj_placas.Criterio = "Distribucion morfologica desde fotografias; no es despiece de fabricacion"


        # =============================================================================
        # 04 - SOPORTES SKF SNL 3268 SIMPLIFICADOS
        # =============================================================================

        centro_soporte_izq = x_centro_tambor - P["dist_centros_soportes"] / 2.0
        centro_soporte_der = x_centro_tambor + P["dist_centros_soportes"] / 2.0


        def crear_soporte_snl(centro_x, fijo):
            t = P["soporte_espesor_axial_J1"]
            x0 = centro_x - t / 2.0
            ancho_base = P["soporte_largo_transversal_L"]
            z_base = -P["soporte_altura_eje_M"]
            alto_base = 82.0

            base = Part.makeBox(t, ancho_base, alto_base,
                                App.Vector(x0, -ancho_base / 2.0, z_base))
            for xx in (x0 + 45.0, x0 + t - 45.0):
                for yy in (-ancho_base / 2.0 + 105.0, ancho_base / 2.0 - 105.0):
                    agujero = Part.makeCylinder(P["soporte_perno"] / 2.0 + 3.0, alto_base + 8.0,
                                                App.Vector(xx, yy, z_base - 4.0), ZDIR)
                    base = base.cut(agujero)

            r_ext = 355.0
            r_int = 207.0
            aro = anillo_x(r_ext, r_int, t, x0)
            limite = Part.makeBox(t, P["soporte_ancho_cuerpo_J"], 720.0,
                                  App.Vector(x0, -P["soporte_ancho_cuerpo_J"] / 2.0, z_base))
            aro = aro.common(limite)

            hombro = Part.makeBox(t, 610.0, 95.0, App.Vector(x0, -305.0, 260.0))
            cuerpo = limpiar(fusionar([base, aro, hombro]))

            # Cuatro pernos de tapa y dos niples de engrase.
            pernos = []
            for xx in (centro_x - 62.0, centro_x + 62.0):
                for yy in (-275.0, 275.0):
                    pernos.append(prisma_hexagonal_z(31.0, 54.0, xx, yy, 290.0))

            niples = []
            for yy in (-92.0, 92.0):
                niples.append(Part.makeCylinder(12.0, 72.0,
                                                App.Vector(centro_x, yy, 355.0), ZDIR))
                niples.append(Part.makeCone(17.0, 9.0, 24.0,
                                            App.Vector(centro_x, yy, 427.0), ZDIR))

            # Rodamiento y sellos taconite visibles al ocultar la carcasa.
            rodamiento = anillo_x(205.0, 160.0, 150.0, centro_x - 75.0)
            sellos = []
            for xs in (x0 - 48.0, x0 - 18.0, x0 + t + 2.0, x0 + t + 32.0):
                sellos.append(anillo_x(222.0, 161.0, 17.0, xs))

            lado = "Fijo" if fijo else "Flotante"
            obj_cuerpo = agregar_shape(g_soportes, "Soporte_%s" % lado,
                                       "SKF SNL 3268 - lado %s (simplificado)" % lado.lower(),
                                       cuerpo, COLOR_NARANJO)
            agregar_shape(g_soportes, "Pernos_%s" % lado, "Pernos tapa soporte %s" % lado.lower(),
                          Part.makeCompound(pernos), COLOR_ACERO_OSCURO)
            agregar_shape(g_soportes, "Niples_%s" % lado, "Niples de engrase 1/8 DIN 3404",
                          Part.makeCompound(niples), COLOR_ACERO_OSCURO)
            obj_rod = agregar_shape(g_soportes, "Rodamiento_%s" % lado,
                                    "Rodamiento 23268 CAK/W33 - %s" % lado.lower(),
                                    rodamiento, COLOR_ACERO)
            agregar_shape(g_soportes, "Sellos_%s" % lado, "Sellos taconite TNF 76/320",
                          Part.makeCompound(sellos), COLOR_BRONCE)
            obj_cuerpo.addProperty("App::PropertyString", "Modelo", "Identificacion")
            obj_cuerpo.addProperty("App::PropertyString", "EstadoGeometria", "Identificacion")
            obj_cuerpo.Modelo = "SKF SNL 3268"
            obj_cuerpo.EstadoGeometria = "SIMPLIFICADA - reemplazar por CAD fabricante/levantamiento"
            if Gui:
                obj_rod.ViewObject.Transparency = 20


        if INCLUIR_SOPORTES_SIMPLIFICADOS:
            crear_soporte_snl(centro_soporte_izq, False)
            crear_soporte_snl(centro_soporte_der, True)


        # =============================================================================
        # 05 - ACCESORIOS FOTOGRAFIADOS (SIMPLIFICADOS Y OCULTABLES)
        # =============================================================================


        def crear_acoplamiento(izquierdo):
            diam = P["acoplamiento_diametro_aprox"]
            r_base = diam / 2.0 - 24.0
            largo = P["acoplamiento_largo_aprox"]
            t_corona = 82.0
            if izquierdo:
                x_hub = 0.0
                x_corona = 0.0
                nombre = "Izquierdo"
            else:
                x_hub = eje_L - largo
                x_corona = eje_L - t_corona
                nombre = "Derecho"

            hub = cilindro_x(205.0, largo, x_hub)
            corona = cilindro_x(r_base, t_corona, x_corona)
            dientes = []
            for i in range(48):
                dientes.append(caja_tangencial_x(
                    x_corona, t_corona, 23.0, r_base - 2.0, 28.0, i * 360.0 / 48.0
                ))
            agregar_shape(g_accesorios, "Cubo_Acoplamiento_%s" % nombre,
                          "Cubo acoplamiento grilla %s" % nombre.lower(), hub, COLOR_ACERO_OSCURO)
            agregar_shape(g_accesorios, "Corona_Acoplamiento_%s" % nombre,
                          "Medio acoplamiento dentado %s (aprox.)" % nombre.lower(),
                          corona, COLOR_NEGRO)
            agregar_shape(g_accesorios, "Dientes_Acoplamiento_%s" % nombre,
                          "48 dientes morfologicos %s" % nombre.lower(),
                          Part.makeCompound(dientes), COLOR_NEGRO)


        if INCLUIR_ACOPLAMIENTOS_FOTO:
            crear_acoplamiento(True)
            crear_acoplamiento(False)


        if INCLUIR_BACKSTOP_SIMPLIFICADO:
            t = P["backstop_espesor"]
            x0 = centro_soporte_izq - P["soporte_espesor_axial_J1"] / 2.0 - t - 8.0
            placa = redondeado_extruido_x(x0, t, P["backstop_ancho"], P["backstop_alto"], 85.0)
            agujero_central = cilindro_x(225.0, t + 4.0, x0 - 2.0)
            placa = placa.cut(agujero_central)

            # Cuatro perforaciones principales de esquina observadas en la carcasa azul.
            for yy in (-P["backstop_ancho"] / 2.0 + 100.0, P["backstop_ancho"] / 2.0 - 100.0):
                for zz in (-P["backstop_alto"] / 2.0 + 100.0, P["backstop_alto"] / 2.0 - 100.0):
                    placa = placa.cut(Part.makeCylinder(35.0, t + 4.0,
                                                        App.Vector(x0 - 2.0, yy, zz), XDIR))
            obj_bs = agregar_shape(g_accesorios, "Backstop_FALK_1165_NRTA",
                                   "Backstop FALK 1165 NRTA (morfologia simplificada)",
                                   limpiar(placa), COLOR_AZUL)
            obj_bs.addProperty("App::PropertyString", "EstadoGeometria", "Identificacion")
            obj_bs.EstadoGeometria = "SIMPLIFICADA DESDE FOTOS E INFORME"

            tapa = anillo_x(295.0, 165.0, 42.0, x0 - 35.0)
            agregar_shape(g_accesorios, "Tapa_Backstop", "Tapa circular backstop",
                          tapa, COLOR_AZUL)
            pernos_tapa = []
            for i in range(18):
                a = 2.0 * math.pi * i / 18.0
                pernos_tapa.append(Part.makeCylinder(
                    12.0, 28.0,
                    App.Vector(x0 - 55.0, 248.0 * math.cos(a), 248.0 * math.sin(a)), XDIR
                ))
            agregar_shape(g_accesorios, "Pernos_Tapa_Backstop", "18 pernos tapa backstop",
                          Part.makeCompound(pernos_tapa), COLOR_ACERO_OSCURO)


        # =============================================================================
        # PRESENTACION Y GUARDADO
        # =============================================================================

        doc.recompute()

        if Gui:
            Gui.activeDocument().activeView().viewAxonometric()
            Gui.activeDocument().activeView().fitAll()
            try:
                Gui.activeDocument().activeView().setAnimationEnabled(False)
            except Exception:
                pass

        if GUARDAR_FCSTD:
            doc.recompute()
            doc.saveAs(RUTA_FCSTD)
            App.Console.PrintMessage("Modelo guardado en: %s\n" % RUTA_FCSTD)

        App.Console.PrintMessage("\nMacro completada: Polea Motriz 145CV012 Pos.6 / OT-1633\n")
        App.Console.PrintMessage("Cotas principales: eje 4369; manto 1981; OD revestimiento 1269 mm.\n")
        App.Console.PrintMessage("Soportes y accesorios marcados como geometria simplificada.\n")
        """.trimIndent()
    ) }
    var isMacroRunning by remember { mutableStateOf(false) }

    // Hold reference to GLSurfaceView to request updates in real-time
    var glSurfaceViewRef by remember { mutableStateOf<CadGLSurfaceView?>(null) }



    // Trigger viewport redraws when the objects array gets updated
    fun triggerViewportRedraw() {
        glSurfaceViewRef?.let { view ->
            if (activeDocId != 0L) {
                view.renderer.setActiveDocument(activeDocId)
                view.renderer.requestMeshUpdate()
                view.requestRender()
            }
        }
    }

    fun createCacheFileFromUri(uri: android.net.Uri, suffix: String): java.io.File? {
        return try {
            val cacheFile = java.io.File(context.cacheDir, "temp_import_$suffix")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            context.contentResolver.openInputStream(uri)?.use { input ->
                cacheFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            cacheFile
        } catch (e: Exception) {
            println("Error copying uri to cache file: ${e.message}")
            null
        }
    }

    // Trigger viewport redraw when activeDocId updates
    LaunchedEffect(activeDocId) {
        if (activeDocId != 0L) {
            triggerViewportRedraw()
        }
    }

    val unifiedFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val fileName = context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) cursor.getString(nameIndex) else "archivo"
                } ?: "archivo"
                
                val lowerName = fileName.lowercase()
                if (lowerName.endsWith(".fcstd") || lowerName.endsWith(".fcstd1") || lowerName.endsWith(".step") || lowerName.endsWith(".stp")) {
                    viewModel.importCadFile(context, it, fileName)
                } else if (lowerName.endsWith(".fcmacro") || lowerName.endsWith(".py")) {
                    context.contentResolver.openInputStream(it)?.use { inputStream ->
                        val content = inputStream.bufferedReader().use { reader -> reader.readText() }
                        macroInputCode = content
                        Toast.makeText(context, "Macro cargada con éxito!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val cacheFile = createCacheFileFromUri(it, "model_generic")
                    if (cacheFile != null) {
                        val text = try { cacheFile.readText() } catch (e: Exception) { "" }
                        val isStep = text.contains("ISO-10303-21") || text.contains("HEADER;")
                        if (isStep) {
                            viewModel.importCadFile(context, it, fileName)
                        } else {
                            macroInputCode = text
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al abrir el archivo", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Auto-run has been disabled to start with an honest empty scene.

    // Helper to insert a Box
    fun addNewBox() {
        viewModel.addNewBox()
        triggerViewportRedraw()
        Toast.makeText(context, "Sólido insertado", Toast.LENGTH_SHORT).show()
    }

    // Helper to insert a Cylinder
    fun addNewCylinder() {
        viewModel.addNewCylinder()
        triggerViewportRedraw()
        Toast.makeText(context, "Sólido insertado", Toast.LENGTH_SHORT).show()
    }

    // Helper to insert a Sphere
    fun addNewSphere() {
        viewModel.addNewSphere()
        triggerViewportRedraw()
        Toast.makeText(context, "Esfera insertada", Toast.LENGTH_SHORT).show()
    }

    // Helper to insert a Cone
    fun addNewCone() {
        viewModel.addNewCone()
        triggerViewportRedraw()
        Toast.makeText(context, "Cono insertado", Toast.LENGTH_SHORT).show()
    }

    // Design layout colors
    val themeBg = if (isDarkTheme) Color(0xFF12151A) else Color(0xFFF0F2F5)
    val cardBg = if (isDarkTheme) Color(0xFF1E222B) else Color(0xFFFFFFFF)
    val accentColor = Color(0xFFFF5722) // CAD Orange
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val textSecColor = if (isDarkTheme) Color.LightGray else Color.DarkGray

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "CAD Engine Logo",
                            tint = accentColor,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "FreeCAD Android Native",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 18.sp,
                            color = textColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isObjectTreeVisible = !isObjectTreeVisible }) {
                        Icon(
                            imageVector = Icons.Default.AccountTree,
                            contentDescription = "Toggle Árbol de Objetos",
                            tint = if (isObjectTreeVisible) accentColor else textColor.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { isPropertiesPanelVisible = !isPropertiesPanelVisible }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Toggle Propiedades",
                            tint = if (isPropertiesPanelVisible) accentColor else textColor.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = { viewModel.setTheme(!isDarkTheme) }) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Theme Switcher",
                            tint = textColor
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Preferences",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color(0xFF1A1D24) else Color(0xFFE3E6EB)
                )
            )
        },
        containerColor = themeBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(themeBg)
        ) {
            // Main CAD Workspace split layout: Side Panel + GL Viewport + Property Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
            ) {
                // Background Viewport Rendering (occupies full viewport)
                AndroidView(
                    factory = { ctx ->
                        CadGLSurfaceView(ctx).apply {
                            glSurfaceViewRef = this
                            if (activeDocId != 0L) {
                                renderer.setActiveDocument(activeDocId)
                                renderer.requestMeshUpdate()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { view ->
                        if (activeDocId != 0L) {
                            view.renderer.setActiveDocument(activeDocId)
                        }
                    }
                )

                // Left Panel: Dynamic Document Tree (Árbol de Objetos)
                if (isObjectTreeVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                            .width(220.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(cardBg.copy(alpha = 0.9f))
                            .border(1.dp, textColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Icon(Icons.Default.Menu, "Tree", tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Árbol de Objetos", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                Spacer(Modifier.weight(1f))
                                IconButton(
                                    onClick = { isObjectTreeVisible = false },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Esconder panel",
                                        tint = textSecColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = textColor.copy(alpha = 0.15f))
                        
                        if (objectsList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Documento vacío", color = textSecColor, fontSize = 11.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                items(objectsList) { obj ->
                                    val isSelected = selectedObjectId == obj.id
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) accentColor.copy(alpha = 0.25f) else Color.Transparent)
                                            .clickable { viewModel.selectObject(obj.id) }
                                            .padding(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (obj.type == "BOX") Icons.Default.Category else Icons.Default.Layers,
                                            contentDescription = null,
                                            tint = if (isSelected) accentColor else textSecColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = obj.name,
                                            fontSize = 11.sp,
                                            color = textColor,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.toggleObjectVisibility(obj.id)
                                                triggerViewportRedraw()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (obj.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Visibilidad",
                                                tint = if (obj.isVisible) accentColor else textSecColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteObject(obj.id)
                                                triggerViewportRedraw()
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Borrar",
                                                tint = Color.Red.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Right Panel: Selected Object Properties (Propiedades de Selección)
                if (isPropertiesPanelVisible) {
                    selectedObjectId?.let { sId ->
                        val selObj = objectsList.find { it.id == sId }
                        if (selObj != null) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .width(220.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(cardBg.copy(alpha = 0.9f))
                                    .border(1.dp, textColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Info, "Props", tint = accentColor, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Propiedades", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                        Spacer(Modifier.weight(1f))
                                        IconButton(
                                            onClick = { isPropertiesPanelVisible = false },
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Esconder propiedades",
                                                tint = textSecColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    Text("Objeto: ${selObj.name}", fontSize = 10.sp, color = textSecColor, modifier = Modifier.padding(bottom = 6.dp))
                                    HorizontalDivider(color = textColor.copy(alpha = 0.15f))

                                Spacer(Modifier.height(8.dp))
                                Text("Traslación X: ${selObj.tx.toInt()} mm", fontSize = 10.sp, color = textColor)
                                Slider(
                                    value = selObj.tx,
                                    onValueChange = { newVal ->
                                        viewModel.updateObjectPosition(sId, newVal, selObj.ty, selObj.tz)
                                        triggerViewportRedraw()
                                    },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                )

                                Text("Traslación Y: ${selObj.ty.toInt()} mm", fontSize = 10.sp, color = textColor)
                                Slider(
                                    value = selObj.ty,
                                    onValueChange = { newVal ->
                                        viewModel.updateObjectPosition(sId, selObj.tx, newVal, selObj.tz)
                                        triggerViewportRedraw()
                                    },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                )

                                Text("Traslación Z: ${selObj.tz.toInt()} mm", fontSize = 10.sp, color = textColor)
                                Slider(
                                    value = selObj.tz,
                                    onValueChange = { newVal ->
                                        viewModel.updateObjectPosition(sId, selObj.tx, selObj.ty, newVal)
                                        triggerViewportRedraw()
                                    },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                )

                                Spacer(Modifier.height(4.dp))
                                HorizontalDivider(color = textColor.copy(alpha = 0.15f))
                                Spacer(Modifier.height(4.dp))

                                 when (selObj.type) {
                                    "BOX" -> {
                                        Text("Largo: ${selObj.dim1.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim1,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, newVal, selObj.dim2, selObj.dim3)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..200f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )

                                        Text("Ancho: ${selObj.dim2.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim2,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, selObj.dim1, newVal, selObj.dim3)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..200f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )

                                        Text("Alto: ${selObj.dim3.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim3,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, selObj.dim1, selObj.dim2, newVal)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..200f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )
                                    }
                                    "CYLINDER" -> {
                                        Text("Radio: ${selObj.dim1.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim1,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, newVal, selObj.dim2, 0f)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..100f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )

                                        Text("Altura: ${selObj.dim2.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim2,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, selObj.dim1, newVal, 0f)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..200f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )
                                    }
                                    "SPHERE" -> {
                                        Text("Radio: ${selObj.dim1.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim1,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, newVal, 0f, 0f)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..100f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )
                                    }
                                    "CONE" -> {
                                        Text("Radio Base: ${selObj.dim1.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim1,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, newVal, selObj.dim2, selObj.dim3)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..100f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )

                                        Text("Radio Cúspide: ${selObj.dim2.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim2,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, selObj.dim1, newVal, selObj.dim3)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 0f..100f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )

                                        Text("Altura: ${selObj.dim3.toInt()} mm", fontSize = 10.sp, color = textColor)
                                        Slider(
                                            value = selObj.dim3,
                                            onValueChange = { newVal ->
                                                viewModel.updateObjectDimensions(sId, selObj.dim1, selObj.dim2, newVal)
                                                triggerViewportRedraw()
                                            },
                                            valueRange = 1f..200f,
                                            colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // Floating Actions to insert Box/Cylinder primitives directly
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FloatingActionButton(
                            onClick = { addNewBox() },
                            containerColor = accentColor,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Category, "Añadir Caja")
                        }
                        FloatingActionButton(
                            onClick = { addNewCylinder() },
                            containerColor = Color(0xFF009688),
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Layers, "Añadir Cilindro")
                        }
                        FloatingActionButton(
                            onClick = { addNewSphere() },
                            containerColor = Color(0xFF9C27B0),
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.Lens, "Añadir Esfera")
                        }
                        FloatingActionButton(
                            onClick = { addNewCone() },
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.ChangeHistory, "Añadir Cono")
                        }
                        FloatingActionButton(
                            onClick = {
                                glSurfaceViewRef?.cameraController?.reset()
                                glSurfaceViewRef?.requestRender()
                            },
                            containerColor = Color(0xFF607D8B),
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.CenterFocusStrong, "Centrar Cámara")
                        }
                    }
                }
            }

            // Bottom Section: Python Console & Live Macro Editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(if (isDarkTheme) Color(0xFF0F1115) else Color(0xFFE0E3E8))
                    .border(1.dp, textColor.copy(alpha = 0.15f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Left Column: Live Python Macro Editor
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, "Editor", tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Editor de Macros Python", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 11.sp, color = textColor)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Button(
                                    onClick = {
                                        unifiedFilePickerLauncher.launch("*/*")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007ACC)),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Folder, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Abrir / Importar CAD", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        // Execute macro
                                        isMacroRunning = true
                                        viewModel.appendConsoleLog(">>> Ejecutando macro...\n")
                                        coroutineScope.launch {
                                            val result = FreeCadNative.executePythonMacro(activeDocId, macroInputCode, maxExecutionTimeSecs * 1000L)
                                            if (result != null) {
                                                if (result.success) {
                                                    viewModel.appendConsoleLog(result.stdout + "\n>>> Done. (${result.executionTimeMs} ms)\n")
                                                } else {
                                                    viewModel.appendConsoleLog(">>> Macro falló.\nSTDOUT:\n${result.stdout}\nSTDERR:\n${result.stderr}\n")
                                                }
                                            } else {
                                                viewModel.appendConsoleLog(">>> Macro finalizada o cancelada.\n")
                                            }
                                            isMacroRunning = false
                                        }
                                    },
                                    enabled = !isMacroRunning && nativeCapabilities.pythonAvailable,
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Ejecutar", fontSize = 10.sp, color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        isMacroRunning = false
                                        viewModel.appendConsoleLog(">>> Macro stopped cooperatively.\n")
                                    },
                                    enabled = isMacroRunning,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Stop, null, modifier = Modifier.size(12.dp), tint = Color.White)
                                    Spacer(Modifier.width(2.dp))
                                    Text("Detener", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }
                        
                        TextField(
                            value = macroInputCode,
                            onValueChange = { macroInputCode = it },
                            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = textColor),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, textColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = cardBg,
                                unfocusedContainerColor = cardBg,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    }

                    // Right Column: Live Python Console Logger
                    Column(
                        modifier = Modifier
                            .weight(0.5f)
                            .fillMaxHeight()
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Terminal, "Console", tint = Color(0xFF009688), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Consola de Salida", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 11.sp, color = textColor)
                            }
                            TextButton(
                                onClick = { viewModel.clearConsole() },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Text("Limpiar", fontSize = 10.sp, color = accentColor)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black)
                                .border(1.dp, Color.DarkGray)
                                .padding(6.dp)
                        ) {
                            LazyColumn {
                                item {
                                    Text(
                                        text = pythonConsoleOutput,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color.Green,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Settings & Configuration Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Configuración del Núcleo CAD", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Calidad de Triangulación adaptativa (OpenCASCADE Mesh):", fontSize = 12.sp, color = textSecColor)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Baja (8 segs)", "Media (16 segs)", "Alta (24 segs)").forEach { level ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { qualityLevel = level }
                            ) {
                                RadioButton(selected = (qualityLevel == level), onClick = { qualityLevel = level })
                                Text(level, fontSize = 11.sp, color = textColor)
                            }
                        }
                    }

                    HorizontalDivider()

                    Text("Tiempo Límite de Ejecución de Macros Python (Segundos):", fontSize = 12.sp, color = textSecColor)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = maxExecutionTimeSecs.toFloat(),
                            onValueChange = { maxExecutionTimeSecs = it.toInt() },
                            valueRange = 1f..15f,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("${maxExecutionTimeSecs}s", fontSize = 12.sp, color = textColor)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showSettingsDialog = false }) {
                    Text("Guardar")
                }
            }
        )
    }
}

private val DEFAULT_STEP_CONTENT = """
ISO-10303-21;
HEADER;
FILE_DESCRIPTION (( 'STEP AP214' ),
    '1' );
FILE_NAME ('Pieza3.STEP',
    '2026-07-18T01:31:37',
    ( '' ),
    ( '' ),
    'SwSTEP 2.0',
    'SolidWorks 2020',
    '' );
FILE_SCHEMA (( 'AUTOMOTIVE_DESIGN' ));
ENDSEC;

DATA;
#1 = DIRECTION ( 'NONE',  ( -0.000000000000000000, -0.000000000000000000, -1.000000000000000000 ) ) ;
#2 = FILL_AREA_STYLE_COLOUR ( '', #56 ) ;
#3 = ADVANCED_FACE ( 'NONE', ( #136 ), #28, .F. ) ;
#4 = DIRECTION ( 'NONE',  ( 1.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#5 = VECTOR ( 'NONE', #168, 1000.000000000000000 ) ;
#6 = EDGE_CURVE ( 'NONE', #106, #73, #150, .T. ) ;
#7 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 69.37225705329153413, 0.000000000000000000 ) ) ;
#8 = PRODUCT ( 'Pieza3', 'Pieza3', '', ( #120 ) ) ;
#9 = EDGE_LOOP ( 'NONE', ( #41, #74, #47, #184 ) ) ;
#10 = ORIENTED_EDGE ( 'NONE', *, *, #195, .F. ) ;
#11 = ADVANCED_FACE ( 'NONE', ( #59 ), #202, .T. ) ;
#12 = PLANE ( 'NONE',  #144 ) ;
#13 = EDGE_LOOP ( 'NONE', ( #135, #119, #200, #183 ) ) ;
#14 =( GEOMETRIC_REPRESENTATION_CONTEXT ( 3 ) GLOBAL_UNCERTAINTY_ASSIGNED_CONTEXT ( ( #66 ) ) GLOBAL_UNIT_ASSIGNED_CONTEXT ( ( #25, #147, #165 ) ) REPRESENTATION_CONTEXT ( 'NONE', 'WORKASPACE' ) );
#15 = ORIENTED_EDGE ( 'NONE', *, *, #137, .T. ) ;
#16 = DIRECTION ( 'NONE',  ( -0.000000000000000000, -1.000000000000000000, -0.000000000000000000 ) ) ;
#17 = DIRECTION ( 'NONE',  ( -1.000000000000000000, -0.000000000000000000, -0.000000000000000000 ) ) ;
#18 = ORIENTED_EDGE ( 'NONE', *, *, #35, .T. ) ;
#19 = AXIS2_PLACEMENT_3D ( 'NONE', #138, #111, #109 ) ;
#20 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 69.37225705329153413, 0.000000000000000000 ) ) ;
#21 = DIRECTION ( 'NONE',  ( -0.000000000000000000, -0.000000000000000000, -1.000000000000000000 ) ) ;
#22 = PRESENTATION_LAYER_ASSIGNMENT (  '', '', ( #170 ) ) ;
#23 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 0.000000000000000000, 109.9999999999999858 ) ) ;
#24 = LINE ( 'NONE', #83, #175 ) ;
#25 =( LENGTH_UNIT ( ) NAMED_UNIT ( * ) SI_UNIT ( .MILLI., .METRE. ) );
#26 = VECTOR ( 'NONE', #60, 1000.000000000000000 ) ;
#27 = FILL_AREA_STYLE ('',( #131 ) ) ;
#28 = PLANE ( 'NONE',  #80 ) ;
#29 = EDGE_CURVE ( 'NONE', #73, #169, #33, .T. ) ;
#30 = EDGE_LOOP ( 'NONE', ( #32, #10, #70, #122 ) ) ;
#31 = VECTOR ( 'NONE', #161, 1000.000000000000000 ) ;
#32 = ORIENTED_EDGE ( 'NONE', *, *, #84, .T. ) ;
#33 = LINE ( 'NONE', #103, #5 ) ;
#34 = LINE ( 'NONE', #157, #185 ) ;
#35 = EDGE_CURVE ( 'NONE', #73, #174, #152, .T. ) ;
#36 = SURFACE_STYLE_USAGE ( .BOTH. , #64 ) ;
#37 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#38 = UNCERTAINTY_MEASURE_WITH_UNIT (LENGTH_MEASURE( 1.000000000000000082E-05 ), #151, 'distance_accuracy_value', 'NONE');
#39 = STYLED_ITEM ( 'NONE', ( #117 ), #123 ) ;
#40 = ORIENTED_EDGE ( 'NONE', *, *, #57, .T. ) ;
#41 = ORIENTED_EDGE ( 'NONE', *, *, #46, .T. ) ;
#42 = DIRECTION ( 'NONE',  ( 1.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#43 = UNCERTAINTY_MEASURE_WITH_UNIT (LENGTH_MEASURE( 1.000000000000000082E-05 ), #177, 'distance_accuracy_value', 'NONE');
#44 = APPLICATION_PROTOCOL_DEFINITION ( 'draft international standard', 'automotive_design', 1998, #63 ) ;
#45 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 109.9999999999999858 ) ) ;
#46 = EDGE_CURVE ( 'NONE', #88, #169, #51, .T. ) ;
#47 = ORIENTED_EDGE ( 'NONE', *, *, #6, .F. ) ;
#48 = COLOUR_RGB ( '',0.7921568627450980005, 0.8196078431372548767, 0.9333333333333333481 ) ;
#49 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 0.000000000000000000, 109.9999999999999858 ) ) ;
#50 = DIRECTION ( 'NONE',  ( 0.000000000000000000, -0.000000000000000000, 1.000000000000000000 ) ) ;
#51 = LINE ( 'NONE', #7, #158 ) ;
#52 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 109.9999999999999858 ) ) ;
#53 = DIRECTION ( 'NONE',  ( 1.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#54 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 0.000000000000000000, 0.000000000000000000 ) ) ;
#55 = DIRECTION ( 'NONE',  ( -1.000000000000000000, 0.000000000000000000, -0.000000000000000000 ) ) ;
#56 = COLOUR_RGB ( '',0.7921568627450980005, 0.8196078431372548767, 0.9333333333333333481 ) ;
#57 = EDGE_CURVE ( 'NONE', #169, #86, #172, .T. ) ;
#58 = VERTEX_POINT ( 'NONE', #107 ) ;
#59 = FACE_OUTER_BOUND ( 'NONE', #13, .T. ) ;
#60 = DIRECTION ( 'NONE',  ( -1.000000000000000000, -0.000000000000000000, -0.000000000000000000 ) ) ;
#61 = VECTOR ( 'NONE', #104, 1000.000000000000000 ) ;
#62 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 109.9999999999999858 ) ) ;
#63 = APPLICATION_CONTEXT ( 'automotive_design' ) ;
#64 = SURFACE_SIDE_STYLE ('',( #160 ) ) ;
#65 = FACE_OUTER_BOUND ( 'NONE', #105, .T. ) ;
#66 = UNCERTAINTY_MEASURE_WITH_UNIT (LENGTH_MEASURE( 1.000000000000000082E-05 ), #25, 'distance_accuracy_value', 'NONE');
#67 = FACE_OUTER_BOUND ( 'NONE', #30, .T. ) ;
#68 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 1.000000000000000000, 0.000000000000000000 ) ) ;
#69 = ADVANCED_FACE ( 'NONE', ( #67 ), #12, .F. ) ;
#70 = ORIENTED_EDGE ( 'NONE', *, *, #118, .F. ) ;
#71 = EDGE_LOOP ( 'NONE', ( #40, #162, #142, #91 ) ) ;
#72 = SHAPE_DEFINITION_REPRESENTATION ( #114, #123 ) ;
#73 = VERTEX_POINT ( 'NONE', #100 ) ;
#74 = ORIENTED_EDGE ( 'NONE', *, *, #29, .F. ) ;
#75 = EDGE_CURVE ( 'NONE', #58, #173, #193, .T. ) ;
#76 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, -1.000000000000000000 ) ) ;
#77 = PRESENTATION_LAYER_ASSIGNMENT (  '', '', ( #39 ) ) ;
#78 = PLANE ( 'NONE',  #116 ) ;
#79 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#80 = AXIS2_PLACEMENT_3D ( 'NONE', #45, #102, #76 ) ;
#81 =( NAMED_UNIT ( * ) PLANE_ANGLE_UNIT ( ) SI_UNIT ( __DOLLAR_SIGN__, .RADIAN. ) );
#82 = FACE_OUTER_BOUND ( 'NONE', #9, .T. ) ;
#83 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 69.37225705329153413, 109.9999999999999858 ) ) ;
#84 = EDGE_CURVE ( 'NONE', #173, #88, #34, .T. ) ;
#85 = VECTOR ( 'NONE', #4, 1000.000000000000000 ) ;
#86 = VERTEX_POINT ( 'NONE', #37 ) ;
#87 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 69.37225705329153413, 109.9999999999999858 ) ) ;
#88 = VERTEX_POINT ( 'NONE', #95 ) ;
#89 =( NAMED_UNIT ( * ) PLANE_ANGLE_UNIT ( ) SI_UNIT ( __DOLLAR_SIGN__, .RADIAN. ) );
#90 = SURFACE_STYLE_USAGE ( .BOTH. , #167 ) ;
#91 = ORIENTED_EDGE ( 'NONE', *, *, #29, .T. ) ;
#92 = VECTOR ( 'NONE', #16, 1000.000000000000000 ) ;
#93 = AXIS2_PLACEMENT_3D ( 'NONE', #166, #134, #42 ) ;
#94 = ORIENTED_EDGE ( 'NONE', *, *, #6, .T. ) ;
#95 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 69.37225705329153413, 0.000000000000000000 ) ) ;
#96 = DIRECTION ( 'NONE',  ( 0.000000000000000000, -1.000000000000000000, 0.000000000000000000 ) ) ;
#97 = DIRECTION ( 'NONE',  ( 0.000000000000000000, -0.000000000000000000, -1.000000000000000000 ) ) ;
#98 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 109.9999999999999858 ) ) ;
#99 = PRODUCT_DEFINITION ( 'UNKNOWN', '', #171, #159 ) ;
#100 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 69.37225705329153413, 109.9999999999999858 ) ) ;
#101 = PRODUCT_RELATED_PRODUCT_CATEGORY ( 'part', '', ( #8 ) ) ;
#102 = DIRECTION ( 'NONE',  ( 1.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#103 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 69.37225705329153413, 109.9999999999999858 ) ) ;
#104 = DIRECTION ( 'NONE',  ( -0.000000000000000000, -1.000000000000000000, -0.000000000000000000 ) ) ;
#105 = EDGE_LOOP ( 'NONE', ( #18, #108, #182, #94 ) ) ;
#106 = VERTEX_POINT ( 'NONE', #128 ) ;
#107 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 0.000000000000000000, 109.9999999999999858 ) ) ;
#108 = ORIENTED_EDGE ( 'NONE', *, *, #181, .T. ) ;
#109 = DIRECTION ( 'NONE',  ( -1.000000000000000000, 0.000000000000000000, -0.000000000000000000 ) ) ;
#110 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 1.000000000000000000, 0.000000000000000000 ) ) ;
#111 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, -1.000000000000000000 ) ) ;
#112 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 0.000000000000000000, 109.9999999999999858 ) ) ;
#113 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 109.9999999999999858 ) ) ;
#114 = PRODUCT_DEFINITION_SHAPE ( 'NONE', 'NONE',  #99 ) ;
#115 = PRESENTATION_STYLE_ASSIGNMENT (( #36 ) ) ;
#116 = AXIS2_PLACEMENT_3D ( 'NONE', #191, #96, #97 ) ;
#117 = PRESENTATION_STYLE_ASSIGNMENT (( #90 ) ) ;
#118 = EDGE_CURVE ( 'NONE', #58, #106, #129, .T. ) ;
#119 = ORIENTED_EDGE ( 'NONE', *, *, #46, .F. ) ;
#120 = PRODUCT_CONTEXT ( 'NONE', #199, 'mechanical' ) ;
#121 = AXIS2_PLACEMENT_3D ( 'NONE', #62, #124, #50 ) ;
#122 = ORIENTED_EDGE ( 'NONE', *, *, #75, .T. ) ;
#123 = ADVANCED_BREP_SHAPE_REPRESENTATION ( 'Pieza3', ( #188, #93 ), #140 ) ;
#124 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 1.000000000000000000, 0.000000000000000000 ) ) ;
#125 = LINE ( 'NONE', #79, #85 ) ;
#126 = VECTOR ( 'NONE', #21, 1000.000000000000000 ) ;
#127 = PLANE ( 'NONE',  #139 ) ;
#128 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 69.37225705329153413, 109.9999999999999858 ) ) ;
#129 = LINE ( 'NONE', #112, #198 ) ;
#130 = EDGE_CURVE ( 'NONE', #86, #173, #125, .T. ) ;
#131 = FILL_AREA_STYLE_COLOUR ( '', #48 ) ;
#132 = ORIENTED_EDGE ( 'NONE', *, *, #130, .T. ) ;
#133 = MECHANICAL_DESIGN_GEOMETRIC_PRESENTATION_REPRESENTATION (  '', ( #39 ), #14 ) ;
#134 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 1.000000000000000000 ) ) ;
#135 = ORIENTED_EDGE ( 'NONE', *, *, #130, .T. ) ;
#136 = FACE_OUTER_BOUND ( 'NONE', #71, .T. ) ;
#137 = EDGE_CURVE ( 'NONE', #174, #86, #156, .T. ) ;
#138 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#139 = AXIS2_PLACEMENT_3D ( 'NONE', #52, #141, #55 ) ;
#140 =( GEOMETRIC_REPRESENTATION_CONTEXT ( 3 ) GLOBAL_UNCERTAINTY_ASSIGNED_CONTEXT ( ( #38 ) ) GLOBAL_UNIT_ASSIGNED_CONTEXT ( ( #151, #89, #148 ) ) REPRESENTATION_CONTEXT ( 'NONE', 'WORKASPACE' ) );
#141 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, -1.000000000000000000 ) ) ;
#142 = ORIENTED_EDGE ( 'NONE', *, *, #35, .F. ) ;
#143 = DIRECTION ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 1.000000000000000000 ) ) ;
#144 = AXIS2_PLACEMENT_3D ( 'NONE', #49, #176, #143 ) ;
#145 =( NAMED_UNIT ( * ) SI_UNIT ( __DOLLAR_SIGN__, .STERADIAN. ) SOLID_ANGLE_UNIT ( ) );
#146 = LINE ( 'NONE', #98, #180 ) ;
#147 =( NAMED_UNIT ( * ) PLANE_ANGLE_UNIT ( ) SI_UNIT ( __DOLLAR_SIGN__, .RADIAN. ) );
#148 =( NAMED_UNIT ( * ) SI_UNIT ( __DOLLAR_SIGN__, .STERADIAN. ) SOLID_ANGLE_UNIT ( ) );
#149 = EDGE_LOOP ( 'NONE', ( #132, #187, #197, #15 ) ) ;
#150 = LINE ( 'NONE', #87, #26 ) ;
#151 =( LENGTH_UNIT ( ) NAMED_UNIT ( * ) SI_UNIT ( .MILLI., .METRE. ) );
#152 = LINE ( 'NONE', #186, #92 ) ;
#153 = SURFACE_STYLE_FILL_AREA ( #27 ) ;
#154 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#155 = ADVANCED_FACE ( 'NONE', ( #82 ), #78, .F. ) ;
#156 = LINE ( 'NONE', #113, #31 ) ;
#157 = CARTESIAN_POINT ( 'NONE',  ( 70.92910916354463779, 0.000000000000000000, 0.000000000000000000 ) ) ;
#158 = VECTOR ( 'NONE', #17, 1000.000000000000000 ) ;
#159 = PRODUCT_DEFINITION_CONTEXT ( 'detailed design', #63, 'design' ) ;
#160 = SURFACE_STYLE_FILL_AREA ( #190 ) ;
#161 = DIRECTION ( 'NONE',  ( -0.000000000000000000, -0.000000000000000000, -1.000000000000000000 ) ) ;
#162 = ORIENTED_EDGE ( 'NONE', *, *, #137, .F. ) ;
#163 = CLOSED_SHELL ( 'NONE', ( #3, #201, #69, #155, #178, #11 ) ) ;
#164 =( GEOMETRIC_REPRESENTATION_CONTEXT ( 3 ) GLOBAL_UNCERTAINTY_ASSIGNED_CONTEXT ( ( #43 ) ) GLOBAL_UNIT_ASSIGNED_CONTEXT ( ( #177, #81, #145 ) ) REPRESENTATION_CONTEXT ( 'NONE', 'WORKASPACE' ) );
#165 =( NAMED_UNIT ( * ) SI_UNIT ( __DOLLAR_SIGN__, .STERADIAN. ) SOLID_ANGLE_UNIT ( ) );
#166 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#167 = SURFACE_SIDE_STYLE ('',( #153 ) ) ;
#168 = DIRECTION ( 'NONE',  ( -0.000000000000000000, -0.000000000000000000, -1.000000000000000000 ) ) ;
#169 = VERTEX_POINT ( 'NONE', #20 ) ;
#170 = STYLED_ITEM ( 'NONE', ( #115 ), #188 ) ;
#171 = PRODUCT_DEFINITION_FORMATION_WITH_SPECIFIED_SOURCE ( 'ANY', '', #8, .NOT_KNOWN. ) ;
#172 = LINE ( 'NONE', #154, #61 ) ;
#173 = VERTEX_POINT ( 'NONE', #54 ) ;
#174 = VERTEX_POINT ( 'NONE', #179 ) ;
#175 = VECTOR ( 'NONE', #1, 1000.000000000000000 ) ;
#176 = DIRECTION ( 'NONE',  ( -1.000000000000000000, 0.000000000000000000, 0.000000000000000000 ) ) ;
#177 =( LENGTH_UNIT ( ) NAMED_UNIT ( * ) SI_UNIT ( .MILLI., .METRE. ) );
#178 = ADVANCED_FACE ( 'NONE', ( #65 ), #127, .F. ) ;
#179 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 109.9999999999999858 ) ) ;
#180 = VECTOR ( 'NONE', #53, 1000.000000000000000 ) ;
#181 = EDGE_CURVE ( 'NONE', #174, #58, #146, .T. ) ;
#182 = ORIENTED_EDGE ( 'NONE', *, *, #118, .T. ) ;
#183 = ORIENTED_EDGE ( 'NONE', *, *, #130, .F. ) ;
#184 = ORIENTED_EDGE ( 'NONE', *, *, #195, .T. ) ;
#185 = VECTOR ( 'NONE', #68, 1000.000000000000000 ) ;
#186 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 0.000000000000000000, 109.9999999999999858 ) ) ;
#187 = ORIENTED_EDGE ( 'NONE', *, *, #75, .F. ) ;
#188 = MANIFOLD_SOLID_BREP ( 'Saliente-Extruir1', #163 ) ;
#189 = PLANE ( 'NONE',  #121 ) ;
#190 = FILL_AREA_STYLE ('',( #2 ) ) ;
#191 = CARTESIAN_POINT ( 'NONE',  ( 0.000000000000000000, 69.37225705329153413, 109.9999999999999858 ) ) ;
#192 = FACE_OUTER_BOUND ( 'NONE', #149, .T. ) ;
#193 = LINE ( 'NONE', #23, #126 ) ;
#194 = MECHANICAL_DESIGN_GEOMETRIC_PRESENTATION_REPRESENTATION (  '', ( #170 ), #164 ) ;
#195 = EDGE_CURVE ( 'NONE', #106, #88, #24, .T. ) ;
#196 = APPLICATION_PROTOCOL_DEFINITION ( 'draft international standard', 'automotive_design', 1998, #199 ) ;
#197 = ORIENTED_EDGE ( 'NONE', *, *, #181, .F. ) ;
#198 = VECTOR ( 'NONE', #110, 1000.000000000000000 ) ;
#199 = APPLICATION_CONTEXT ( 'automotive_design' ) ;
#200 = ORIENTED_EDGE ( 'NONE', *, *, #84, .F. ) ;
#201 = ADVANCED_FACE ( 'NONE', ( #192 ), #189, .F. ) ;
#202 = PLANE ( 'NONE',  #19 ) ;
ENDSEC;
END-ISO-10303-21;
""".trimIndent().replace("__DOLLAR_SIGN__", "$")
