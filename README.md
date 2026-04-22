# 📱 Manual de Instalación – AcLoc

## 1. ✅ Requisitos del sistema

### Para instalación directa (APK)
- Dispositivo Android con versión **Android 7.0 (API 24)** o superior (*API 35 target API*)
- Espacio libre recomendado: **50 MB**
- Permitir instalación de aplicaciones desde orígenes desconocidos

### Para compilación desde código fuente
- Android Studio **Hedgehog (o superior)**
- Gradle **8.10.2**
- Android Gradle Plugin **8.6.1**
- Java Development Kit (JDK) **11**
- SDK de compilación: **API 35 (Android 14)**
- Conexión a internet para sincronizar dependencias

---

## 2. 📦 Archivos necesarios
- [`acloc-release.apk`](#) – Archivo APK compilado. Podrá encontrarse en el USB con la documentación en la carpeta `/apk/debug`
- Repositorio de código fuente: [`https://projectes.ieslamar.org/pauher/AcLoc_app.git`](#)

---

## 3. 🔧 Instalación en dispositivo Android

### Opción A: Instalar el APK directamente
1. Transfiere el archivo `acloc-release.apk` al dispositivo Android (por USB, Drive, correo, etc.).
2. En el dispositivo, ve a:  
   **Ajustes → Seguridad → Instalar apps desconocidas**, y permite la fuente desde donde lo vas a instalar (por ejemplo, el gestor de archivos).
3. Abre el archivo `.apk` y acepta la instalación.
4. Una vez instalada, abre la app desde el menú de aplicaciones.

> ⚠️ En versiones modernas de Android (10+), cada app desde donde se instala el APK (por ejemplo, Drive o Gestor de Archivos) debe tener el permiso habilitado individualmente.

### Opción B: Instalar desde Android Studio
1. Clona el repositorio (GitTea tiene una opción para hacerlo desde el repositorio en la web también):
   ```bash
   git clone https://projectes.ieslamar.org/pauher/AcLoc_app.git
   ```
2. Abre el proyecto con Android Studio.
3. Espera a que Gradle resuelva las dependencias.
4. Conecta un dispositivo Android con depuración USB activada o crea un emulador (esta última opción no está recomendada por el uso de GPS implícito en la app).
5. Ejecuta el proyecto con el botón ▶️ (Run).

---

## 4. 🔑 Permisos utilizados por la aplicación
La app ACLoc solicita los siguientes permisos:
- **ACCESS_FINE_LOCATION** – Para mostrar la ubicación del usuario en el mapa.
- **INTERNET** – Para acceder a servicios remotos a través de Retrofit y cargar imágenes.
- **ACCESS_COARSE_LOCATION** – Para una ubicación menos precisa cuando sea necesario.

---

## 5. 📚 Tecnologías y librerías utilizadas
- **Retrofit 2** – Consumo de APIs REST
- **Gson Converter** – Conversión de JSON a objetos Java
- **Google Maps SDK (v18.2.0)** – Muestra mapas interactivos
- **Glide & Picasso** – Carga y visualización de imágenes
- **OkHttp & Logging Interceptor** – Gestión de peticiones HTTP
- **Flexbox Layout** – Gestión avanzada de layouts
- **Material Design Components** – Interfaz de usuario moderna

---

## 6. 🚨 Posibles errores y soluciones

| Problema | Solución |
|----------|----------|
| Error al instalar el APK | Asegúrate de permitir "Orígenes desconocidos" en los ajustes del móvil |
| Error de sincronización en Gradle | Verifica conexión a Internet y que las versiones de Gradle estén correctas |
| App se cierra al iniciarse | Revisa permisos concedidos |
| minSdkVersion 24 incompatible | Asegúrate de usar un dispositivo con Android 7.0 o superior |

---

## 7. 📄 Notas adicionales
- La aplicación puede conectarse a una API REST (está configurada con la nube de LocationAPI en estos momentos) y mostrar resultados en mapas o mediante listas.
- Para poder ejecutar la aplicación en local, se deberá cambiar la constante `BASE_URL` en el archivo `Constants.java` a localhost o la url base del nuevo servidor en la nube.
- Se recomienda usar una cuenta de Google Maps con una API Key válida si se desea ampliar el uso en producción.
- La app requiere de conexión a internet

---

## 8. 🧪 Testing
- **Instrumentación**: androidx.test.runner.AndroidJUnitRunner
- Testing UI con Espresso
- Actualmente el testing realizado a la aplicación ha sido uno básico de recorrido de funcionamiento utilizando breakpoints y debugger.

---

## 9. 📍 Información de la app
- **ID del paquete**: com.ieslamar.acloc
- **Versión**: 1.0 (versionCode: 1)
- **SDK mínima**: API 24 (Android 7.0)
- **SDK objetivo**: API 35 (Android 14)
- **Compatibilidad de Java**: Java 11
