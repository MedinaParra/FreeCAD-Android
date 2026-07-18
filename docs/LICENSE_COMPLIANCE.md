# Cumplimiento y Análisis de Licencias Open Source

El desarrollo de `FreeCAD Android Native` se basa firmemente en el ecosistema de software libre. Este documento detalla las obligaciones técnicas y legales para asegurar el cumplimiento completo de las licencias involucradas.

## 1. Análisis de las Dependencias de Software Libre

| Biblioteca | Licencia | Rol en el Proyecto | Obligaciones Principales |
| :--- | :--- | :--- | :--- |
| **FreeCAD Core** | **LGPL v2.1+** | Núcleo de datos paramétricos | Permitir relinkeo, publicar modificaciones del código nativo. |
| **OpenCASCADE** | **OCCT License** (Permisiva) | Kernel geométrico de sólidos | Mantener avisos de copyright y licencias intactos. |
| **CPython** | **PSF License** | Intérprete Python embebido | Compatibilidad permisiva comercial y libre. |
| **Jetpack Compose**| **Apache 2.0** | Capa visual de UI | Mantener avisos de copyright y atribuciones. |

---

## 2. Cumplimiento de la Licencia LGPL v2.1 en Android

Dado que utilizaremos componentes del código de FreeCAD (LGPL), el proyecto debe garantizar las libertades que otorga la licencia LGPL:

1.  **Enlace Dinámico:** Compilar los módulos de FreeCAD como bibliotecas compartidas independientes (`.so`), de modo que el código de la interfaz de usuario en Java/Kotlin se conecte de forma dinámica mediante JNI. Esto permite que un usuario final pueda teóricamente reemplazar o actualizar la porción dinámica de FreeCAD sin necesidad de que el autor proporcione el código de la interfaz propietaria de la app (si se optara por una licencia propietaria en el frontend).
2.  **Publicación del Código Modificado:** Cualquier cambio, corrección de errores o optimización realizada sobre el núcleo original de FreeCAD (las clases bajo LGPL) debe hacerse público bajo la misma licencia LGPL.
3.  **Sección de Licencias en la Aplicación:** La aplicación debe proporcionar una sección accesible ("Open Source Licencias") dentro de la interfaz donde se muestren los textos completos de las licencias utilizadas y se otorguen los créditos correspondientes a los desarrolladores originales de FreeCAD y OpenCASCADE.

---

## 3. Descargo de Atribución

Este análisis técnico representa una planificación de cumplimiento de buenas prácticas de ingeniería de software libre y no constituye asesoría legal definitiva.
