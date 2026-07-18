# Plan de Portabilidad de FreeCAD a Android Nativo

Este documento proporciona una ruta de ingeniería lógica para migrar de forma progresiva y técnica el núcleo de FreeCAD al ecosistema móvil nativo de Android.

## 1. Clasificación de Componentes de FreeCAD

### Módulos Portables (Foco de Migración)
*   **FreeCAD Base / App:** Gestión del modelo de datos paramétrico, sistema de propiedades, árbol de dependencias geométricas y recómputo (`App::Document`, `App::Property`).
*   **Part Module (OpenCASCADE wrappers):** Encapsulación de operaciones de modelado sólido, representaciones BRep, extrusiones, cortes, uniones y fileteados (`Part::Feature`, `Part::TopoShape`).
*   **Sistema de Unidades (Units):** Gestión de precisión física de medidas (mm, pulgadas, etc.).
*   **Módulo de Importación/Exportación (Import):** STEP, IGES y exportadores visuales ligeros (STL, OBJ, glTF).

### Módulos que Deben Reemplazarse por Completo
*   **FreeCAD Gui (Capa de Escritorio):** Depende de Qt Widgets y librerías heredadas de escritorio. Se reemplaza completamente con **Jetpack Compose** en Android.
*   **Visualizador Coin3D:** Se reemplaza por nuestro motor OpenGL ES 3.0 / Vulkan optimizado para mallas pesadas y gestos multitáctiles móviles.

---

## 2. Planificación de Fases de Ingeniería

### Fase 0 (Actual): Investigación, Tubería JNI y Visor OpenGL ES 3.0
*   **Objetivo:** Validar la comunicación completa de datos geométricos de alta fidelidad desde C++ nativo hasta la superficie OpenGL de Android sin copias de arreglos pesados.
*   **Logrado:** Se construyó un pipeline DirectByteBuffer que transfiere mallas tridimensionales con normales e índices generados en C++ directamente a la GPU a través del renderizador GLES, controlado por una consola Python simulada y controles Compose interactivos.

### Fase 1: Vinculación Real de OpenCASCADE (Fase Siguiente)
*   **Objetivo:** Reemplazar el generador nativo manual de mallas por llamadas reales de modelado geométrico de OpenCASCADE.
*   **Tareas:**
    1. Compilar OpenCASCADE para la arquitectura ARM64-v8a utilizando la guía NDK.
    2. Importar los encabezados de OpenCASCADE en el CMake nativo del proyecto.
    3. Reemplazar `generateBoxMesh` por wrappers nativos de `BRepPrimAPI_MakeBox` y `BRepMesh_IncrementalMesh` para generar mallas paramétricas reales.

### Fase 2: Persistencia Documental Paramétrica y Room
*   **Objetivo:** Guardar el árbol paramétrico en formato `.fcadandroid` y base de datos local SQLite (Room).
*   **Estructura del Proyecto:** Un archivo ZIP que contiene un `document.json` describiendo los parámetros originales (e.g. Caja longitud=40) en vez de guardar únicamente la malla estática resultante. Esto preserva la naturaleza de modelado paramétrico original de FreeCAD.

### Fase 3: Runtime Completo de Python
*   **Objetivo:** Vincular un intérprete CPython embebido.
*   **Desafío:** Ejecutar macros en un hilo secundario y capturar `stdout` de forma asíncrona. Manejo de timeouts cooperativos y reinicio controlado en caso de bucles infinitos en macros.
