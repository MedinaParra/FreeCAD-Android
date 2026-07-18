# Arquitectura del Sistema: FreeCAD Android Native

Este documento describe la arquitectura técnica detallada del primer prototipo nativo y sin conexión de FreeCAD para la plataforma Android.

## 1. Vista General de Arquitectura (Arquitectura Híbrida)

Para portar un software de la envergadura de FreeCAD a Android, se rechaza de plano cualquier solución basada en servidores Web, APIs remotas o virtualización de escritorios. Toda la ejecución de geometría y procesamiento es **local y offline**.

La arquitectura se divide en dos grandes mundos conectados de forma ultra eficiente:

```text
+-------------------------------------------------------+
|                Capa de Interfaz (Kotlin)              |
|   - Jetpack Compose (Modern Material Design 3)        |
|   - Gestores de Estado Reactivos (StateFlow)          |
|   - Room Database (Persistencia local)                |
+--------------------------+----------------------------+
                           |
                           |  Llamadas JNI Estables
                           v
+-------------------------------------------------------+
|                 Puente de Enlace (JNI)                |
|   - freecad_jni.cpp (Traductor bidireccional)         |
|   - DirectByteBuffers (Intercambio binario de mallas)  |
+--------------------------+----------------------------+
                           |
                           |  C++ Moderno (C++17)
                           v
+-------------------------------------------------------+
|                Capa de Núcleo CAD (C++)               |
|   - NativeCadEngine (Gestor documental)               |
|   - OpenCASCADE Technology (Kernel geométrico BRep)   |
|   - Intérprete CPython (Embebido local)              |
+-------------------------------------------------------+
```

---

## 2. Responsabilidades por Módulo

### `android-ui` (Kotlin + Jetpack Compose)
*   **Responsabilidad:** Capa visual declarativa adaptativa.
*   **Componentes:**
    *   `MainScreen`: Panel principal de trabajo, árbol de objetos, paneles de traducción y parámetros deslizables.
    *   `CadGLSurfaceView`: Integra la superficie de dibujo nativa de OpenGL ES 3.0 dentro del árbol composable.

### `native-bridge` (JNI)
*   **Responsabilidad:** Canal bidireccional de baja latencia entre el entorno JVM y el entorno nativo.
*   **Optimización Clave:** Para transferir mallas de millones de triángulos, se evita instanciar colecciones de objetos Kotlin. En su lugar, el puente entrega referencias directas mediante `DirectByteBuffer`. OpenGL carga estos buffers directo a la GPU.

### `native-core` (C++ + OpenCASCADE)
*   **Responsabilidad:** Modelado geométrico tridimensional sólido paramétrico.
*   **Funciones:**
    *   Crear primitivas geométricas paramétricas (Caja, Cilindro, Esfera, Cono, Toroide) utilizando OpenCASCADE (`BRepPrimAPI`).
    *   Ejecutar operaciones booleanas de unión, intersección y corte mediante el módulo `BRepAlgoAPI` de OpenCASCADE.
    *   Generar mallas triangulares mediante `BRepMesh_IncrementalMesh`.

### `macro-runtime` (Python Embebido)
*   **Responsabilidad:** Interpretar localmente macros compatibles con la API de FreeCAD.
*   **Estrategia:** Embeber CPython compilado de forma cruzada para ARM64-v8a. Exponer un wrapper C++ utilizando pybind11 o la API C clásica para inyectar los objetos `FreeCAD` y `Part`.

---

## 3. Flujo de Datos del Renderizado 3D

Para lograr renderizado a 60 FPS sin bloquear la interfaz de usuario:
1.  **Modificación:** El usuario desliza un control en Kotlin (e.g. Traslación X).
2.  **JNI Call:** Se envía el ID del objeto y las nuevas coordenadas mediante `translateObject`.
3.  **C++ Update:** El núcleo actualiza el placement y genera la malla de forma nativa.
4.  **Callback / Redraw:** Se solicita el redibujo en el hilo de OpenGL (`requestRender()`).
5.  **Direct Upload:** El renderizador consulta `getSceneMesh`, obtiene los direct buffers de vértices e índices, y los asocia a buffers de vídeo (VBO/IBO) locales de la GPU.
