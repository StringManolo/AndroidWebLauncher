# Android Web Launcher 🚀

Un launcher moderno para Android desarrollado con Kotlin, XML, HTML, CSS y JavaScript. 

## ✨ Características

### 🎨 Interfaz Moderna
- **Diseño Glassmorphism** con efectos de vidrio esmerilado
- **Tema oscuro moderno** con gradientes azules
- **Animaciones fluidas** y transiciones suaves
- **Responsive** que se adapta a orientación vertical y horizontal

### 🎯 Gestos Inteligentes
- **Tap rápido** (<500ms): Lanzar aplicación
- **Mantener 1s + arrastrar**: Reordenar apps
- **Mantener 2s**: Agregar/remover de favoritos
- **Mantener 4s**: Ocultar/mostrar aplicación
- **Mantener 6s**: Desinstalar (con confirmación)
- **Mantener 4s en el header**: Mostrar apps ocultas
- **Doble tap en header**: Selector de fondo

### 📱 Funcionalidades Principales
- **Dock con scroll horizontal** infinito para apps favoritas
- **Búsqueda en tiempo real** de aplicaciones
- **Reordenación por drag & drop** con feedback visual
- **Sistema de apps ocultas** para mantener el launcher limpio
- **Fondo de pantalla personalizable** desde galería
- **Reloj y fecha** en tiempo real

### 🔧 Personalización
- **Interfaz web completa** - modifica HTML/CSS/JS fácilmente
- **Temas CSS** con variables CSS fácilmente editables
- **Comportamiento personalizable** mediante JavaScript
- **Persistencia de configuraciones** en localStorage

## 🛠️ Instalación

### Configuración como Launcher Predeterminado
1. **Presiona el botón Home** en tu dispositivo
2. **Selecciona "Web Launcher"** de la lista
3. **Elige "Siempre"** para establecerlo como predeterminado

## 🎮 Cómo Usar

### Navegación Básica
- **Abrir apps**: Toca cualquier icono de aplicación
- **Buscar**: Escribe en la barra de búsqueda superior
- **Scroll**: Desliza verticalmente para ver todas las apps
- **Dock**: Desliza horizontalmente en el dock para ver más favoritos

### Gestos Avanzados
| Gestor | Duración | Acción |
|--------|----------|--------|
| 👆 Tap rápido | < 500ms | Lanzar app |
| 👆 Mantener + arrastrar | 1s + | Reordenar apps |
| 👆 Mantener | 2s | Agregar/remover favoritos |
| 👆 Mantener | 4s | Ocultar/mostrar app |
| 👆 Mantener | 6s | Desinstalar (con confirmación) |
| 👆 Mantener en header | 4s | Mostrar/ocultar apps ocultas |
| 👆👆 Doble tap en header | - | Cambiar fondo de pantalla |

### Personalización del Fondo
1. **Doble tap** en el área del header (hora/fecha)
2. **Selecciona "Galería"** para elegir una imagen
3. **O "Predeterminado"** para volver al fondo original

## 🏗️ Estructura del Proyecto

```
app/src/main/
├── java/com/stringmanolo/awl/
│   └── MainActivity.kt              # Actividad principal
├── res/
│   ├── layout/
│   │   └── activity_main.xml        # Layout del WebView
│   ├── xml/
│   │   └── file_paths.xml           # Configuración FileProvider
│   └── values/
│       └── strings.xml              # Recursos de texto
├── assets/
│   ├── launcher.html               # Interfaz web principal
│   ├── launcher.css                # Estilos y temas
│   └── launcher.js                 # Lógica y funcionalidades
└── AndroidManifest.xml             # Configuración y permisos
```

## 🎨 Personalización

### Modificar la Interfaz
Edita los archivos en `assets/` para personalizar completamente el launcher:

**`launcher.css`** - Apariencia visual:
```css
:root {
    --primary-bg: rgba(15, 20, 30, 0.85);
    --accent-color: #00d4ff;
    /* Modifica estos valores para cambiar colores */
}
```

**`launcher.html`** - Estructura:
```html
<!-- Modifica la estructura HTML para cambiar el layout -->
```

**`launcher.js`** - Comportamiento:
```javascript
// Ajusta los tiempos de los gestos
const GESTURE_TIMING = {
    TAP: 500,
    DRAG_START: 1000,
    // ... etc
};
```

### Agregar Nuevas Funcionalidades
1. **Extiende `MainActivity.kt`** para nuevas funciones nativas
2. **Agrega métodos** en `WebAppInterface`
3. **Llama desde JavaScript** usando `Android.nuevaFuncion()`

## 🔧 Troubleshooting

### Problemas Comunes

**Problemas de rendimiento:**
- Reduce la calidad de las imágenes de fondo
- Simplifica animaciones CSS complejas

## 📋 Permisos

| Permiso | Propósito |
|---------|-----------|
| `INTERNET` | Cargar recursos web externos (no necesario) |
| `QUERY_ALL_PACKAGES` | Listar aplicaciones instaladas |
| `READ_EXTERNAL_STORAGE` | Acceder a imágenes para fondo |

## 🤝 Contribuir

Las contribuciones son bienvenidas. Por favor:

1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 🐛 Reportar Issues

Si encuentras algún problema, por favor:

1. Revisa los issues existentes
2. Crea un nuevo issue con:
   - Descripción detallada
   - Pasos para reproducir
   - Capturas de pantalla (si aplica)
   - Información del dispositivo/Android version

## 🌟 Características Futuras

- [ ] Widgets Web y Nativos (inyección desde localStorage)
- [ ] Diferentes temas predefinidos
- [ ] Soporte para icon packs
- [ ] Cambio entre modo cuadrícula y modo lista
- [ ] Ajustes para editar apariencia
---

**Desarrollado con Kotlin y Web Technologies**

¿Preguntas o sugerencias? ¡Abre un issue o contribuye al proyecto!
