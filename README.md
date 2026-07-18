# FreeCAD Android Native

Un prototipo nativo, sin conexión (completamente local) y de alto rendimiento para portar progresivamente las funcionalidades de **FreeCAD** al sistema operativo Android.

---

## 🚀 Logros del Prototipo (Fase 0)

1.  **Arquitectura Híbrida Compilable:** Configuración de la tubería Gradle Kotlin DSL + CMake NDK para compilar de manera reproducible en ARM64-v8a.
2.  **Puente JNI Eficiente con DirectByteBuffer:** Las mallas 3D pesadas se calculan y se estructuran en buffers binarios directos en C++ para evitar copias de arrays en la JVM y subirse directo a la GPU.
3.  **Visualizador 3D OpenGL ES 3.0 Integrado:** Renderizador de sólidos 3D con sombreado de Phong (iluminación dinámica, brillo especular, ambiente), grilla cartesiana y ejes de referencia (X=Rojo, Y=Verde, Z=Azul).
4.  **Gestos Multitáctiles en Pantalla:**
    *   **Un Dedo:** Órbita / Rotación espacial de la cámara.
    *   **Dos Dedos (Pinch):** Zoom interactivo.
    *   **Dos Dedos (Swipe):** Desplazamiento (Pan) de la cámara.
5.  **Entorno de Trabajo CAD en Compose:** Árbol de Objetos interactivo con controles de eliminación y visibilidad rápida, junto con paneles de traducción de precisión (X, Y, Z).
6.  **Micro-Entorno de Ejecución Python:** Consola inferior interactiva para macros y consola de depuración en tiempo real.

---

## 📁 Estructura del Repositorio

*   **`/app`**: Módulo Android nativo.
    *   `src/main/cpp/`: Código nativo en C++ (JNI, motores de mallas, administración documental).
    *   `src/main/java/com/medinaparra/freecadandroid/`: Interfaces Kotlin y vistas Compose.
*   **`/docs`**: Documentación de arquitectura, cumplimiento de licencias y plan de migración.
*   **`/scripts`**: Scripts de configuración cruzada de dependencias pesadas (OpenCASCADE).

---

## 🛠️ Cómo Compilar y Ejecutar

Asegúrate de contar con el SDK de Android y NDK instalado.

### 1. Inicializar Variables de Entorno (Opcional)
```bash
source scripts/setup_android_env.sh
```

### 1.5 Compilar OpenCASCADE (OBLIGATORIO para variante CAD real)
Antes de compilar la aplicación principal, compile las dependencias de modelado de OpenCASCADE para ARM64:
```bash
./scripts/build_opencascade_android.sh
# Opcional: Verificar artefactos generados
./scripts/verify_native_artifacts.sh
```

### 2. Ejecutar la Compilación Completa
```bash
gradle assembleDebug
```
El archivo APK instalable resultante se generará en la ruta:
`app/build/outputs/apk/debug/app-debug.apk`

---

## ⚖️ Licenciamiento Libre

Este proyecto se distribuye bajo los términos de la licencia **GNU LGPL v2.1** para garantizar la compatibilidad absoluta con el núcleo nativo de FreeCAD. Lee `/docs/LICENSE_COMPLIANCE.md` para más información sobre cumplimiento legal y distribución técnica.
