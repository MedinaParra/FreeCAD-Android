# Guía de Compilación: FreeCAD Android Native

Esta guía documenta los pasos y requisitos necesarios para compilar la aplicación y sus bibliotecas nativas desde cero para la arquitectura ARM64-v8a.

## 1. Requisitos del Entorno de Desarrollo

Para compilar de manera exitosa el núcleo nativo de C++ y el frontend en Kotlin, necesitas:

*   **Android SDK:** API Level 30 o superior (API 36 recomendada).
*   **Android NDK:** Versión `25.x.xxxxxx` o superior.
*   **CMake:** Versión `3.22.1` o superior (administrado por el SDK Manager).
*   **JDK:** Java 17 o superior.
*   **Gradle:** Kotlin DSL compatible con AGP 8+.

---

## 2. Compilación de OpenCASCADE (OCCT)

Para compilar y enlazar la variante de modelado CAD real de OpenCASCADE para Android `arm64-v8a`, siga estos pasos:

### 2.1 Descargar y Compilar
Ejecute el script de compilación automatizado:
```bash
./scripts/build_opencascade_android.sh
```
Este script:
1. Lee las versiones fijas desde `third-party/versions.lock`.
2. Descarga la versión exacta y verifica el hash del commit de OCCT mediante `scripts/fetch_native_sources.sh`.
3. Invoca a CMake y Ninja con la toolchain oficial del NDK de Android.
4. Compila solo los módulos necesarios (Kernel, BRep, Modelado, Mallado y STEP/IGES), desactivando módulos pesados de escritorio.
5. Instala las cabeceras y las bibliotecas dinámicas `.so` en `third-party/opencascade/arm64-v8a/`.

### 2.2 Verificar Artefactos
Para validar que la compilación de OCCT se completó con éxito y generó todos los componentes requeridos, ejecute:
```bash
./scripts/verify_native_artifacts.sh
```
El script comprobará la existencia de cabeceras críticas (`Standard.hxx`), todas las bibliotecas de modelado y STEP, y reportará sus dependencias internas.

---

## 3. Compilación del Proyecto Completo

Para ejecutar la compilación incremental y generar el APK de depuración (Debug), puedes utilizar la terminal o Android Studio:

### Vía Terminal (Línea de Comandos)

Desde la raíz del proyecto, ejecuta:

```bash
# Compilar la aplicación completa (incluyendo NDK/C++ mediante CMake)
gradle assembleDebug
```

Esto generará el APK compilado en la ruta:
`/app/build/outputs/apk/debug/app-debug.apk`

---

## 3. Estructura de Compilación CMake

La compilación nativa de C++ se describe en `/app/src/main/cpp/CMakeLists.txt`. Cuando Gradle inicia la compilación:
1.  Gradle lee la configuración `externalNativeBuild` en `app/build.gradle.kts`.
2.  Llama al binario de CMake correspondiente al NDK activo.
3.  CMake compila los archivos `freecad_jni.cpp`, `NativeCadEngine.cpp` y `MeshData.cpp`.
4.  Genera la biblioteca de enlace dinámico `libfreecad_native.so` empaquetada dentro del APK para la arquitectura ARM64-v8a.

---

## 4. Resolución de Problemas Frecuentes

### Error: `UnsatisfiedLinkError` al iniciar la App
*   **Causa:** La biblioteca dinámica `libfreecad_native.so` no se cargó correctamente o la firma JNI no coincide con la declaración del método externo de Kotlin.
*   **Solución:** Verifica que el nombre de la función en C++ coincida exactamente con la ruta del package Kotlin. Ejemplo:
    `Java_com_medinaparra_freecadandroid_nativebridge_FreeCadNative_initialize`
    corresponde exactamente a:
    `com.medinaparra.freecadandroid.nativebridge.FreeCadNative.initialize`

### Error: El compilador no encuentra archivos de cabecera de C++
*   **Causa:** Rutas incorrectas en `CMakeLists.txt` o falta de la cabecera en el target de compilación.
*   **Solución:** Asegúrate de incluir los archivos `.hpp` dentro de las fuentes de `add_library` en CMake.
