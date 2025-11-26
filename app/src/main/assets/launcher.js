// Variables globales
let allApps = [];
let favoriteApps = JSON.parse(localStorage.getItem('favoriteApps')) || [];
let hiddenApps = JSON.parse(localStorage.getItem('hiddenApps')) || [];
let appPositions = JSON.parse(localStorage.getItem('appPositions')) || {};

// Variables para gestión de gestos
let currentGesture = {
    appElement: null,
    packageName: null,
    startTime: 0,
    startX: 0,
    startY: 0,
    timer: null,
    isDragging: false,
    hasMoved: false,
    dragElement: null
};

// Variables para drag and drop
let dragScrollInterval = null;
let isReordering = false;

// Umbrales de gestos (en milisegundos)
const GESTURE_TIMING = {
    TAP: 500,
    DRAG_START: 1000,
    FAVORITE: 2000,
    HIDE: 4000,
    UNINSTALL: 6000
};

// Umbral de movimiento para considerar arrastre (en píxeles)
const MOVE_THRESHOLD = 20;

// Cargar aplicaciones desde Android
window.loadAllApps = function(apps) {
    allApps = apps;
    renderApps();
    updateTime();
    setInterval(updateTime, 1000);
    updateAppStats();
};

// Inicialización cuando el DOM está listo
document.addEventListener('DOMContentLoaded', function() {
    initializeEventListeners();
    updateTime();
    updateAppStats();
    
    // Prevenir comportamientos no deseados
    document.addEventListener('touchmove', function(e) {
        if (e.touches.length > 1) {
            e.preventDefault();
        }
    }, { passive: false });
});

function initializeEventListeners() {
    // Eventos de búsqueda
    const searchInput = document.getElementById('searchInput');
    searchInput.addEventListener('input', filterApps);
    
    // Eventos del modal
    document.getElementById('galleryBtn').addEventListener('click', selectWallpaperFromGallery);
    document.getElementById('defaultWallpaperBtn').addEventListener('click', useDefaultWallpaper);
    document.getElementById('cancelModalBtn').addEventListener('click', closeModal);
    
    // Eventos del selector de archivos
    document.getElementById('wallpaperInput').addEventListener('change', handleWallpaperSelect);
    
    // Eventos del header para mostrar apps ocultas
    const headerArea = document.getElementById('headerArea');
    setupHeaderGestureEvents(headerArea);
    
    // Evento de doble tap en header para abrir selector de fondo
    setupDoubleTapForWallpaper(headerArea);

    // Prevenir scroll durante drag
    document.addEventListener('touchmove', function(e) {
        if (currentGesture.isDragging || isReordering) {
            e.preventDefault();
        }
    }, { passive: false });
}

function setupHeaderGestureEvents(headerElement) {
    let headerPressTimer = null;

    headerElement.addEventListener('touchstart', function(e) {
        headerPressTimer = setTimeout(() => {
            showHiddenApps();
        }, GESTURE_TIMING.HIDE);
    });

    headerElement.addEventListener('touchend', function(e) {
        if (headerPressTimer) {
            clearTimeout(headerPressTimer);
            headerPressTimer = null;
        }
    });

    headerElement.addEventListener('touchmove', function(e) {
        if (headerPressTimer) {
            clearTimeout(headerPressTimer);
            headerPressTimer = null;
        }
    });

    // Eventos similares para mouse en el header
    headerElement.addEventListener('mousedown', function(e) {
        headerPressTimer = setTimeout(() => {
            showHiddenApps();
        }, GESTURE_TIMING.HIDE);
    });

    headerElement.addEventListener('mouseup', function(e) {
        if (headerPressTimer) {
            clearTimeout(headerPressTimer);
            headerPressTimer = null;
        }
    });

    headerElement.addEventListener('mouseleave', function(e) {
        if (headerPressTimer) {
            clearTimeout(headerPressTimer);
            headerPressTimer = null;
        }
    });
}

function setupDoubleTapForWallpaper(headerElement) {
    let lastTap = 0;
    headerElement.addEventListener('touchend', function(e) {
        const currentTime = new Date().getTime();
        const tapLength = currentTime - lastTap;
        if (tapLength < 300 && tapLength > 0) {
            openWallpaperPicker();
        }
        lastTap = currentTime;
    });
}

function renderApps() {
    const grid = document.getElementById('appsGrid');
    grid.innerHTML = '';

    // Ordenar apps según posiciones guardadas
    const sortedApps = [...allApps].sort((a, b) => {
        const posA = appPositions[a.packageName] || 0;
        const posB = appPositions[b.packageName] || 0;
        return posA - posB;
    });

    sortedApps.forEach((app, index) => {
        if (!appPositions[app.packageName]) {
            appPositions[app.packageName] = index;
        }

        // Si la app está oculta, no renderizar
        if (hiddenApps.includes(app.packageName)) {
            return;
        }

        const appElement = createAppElement(app);
        grid.appendChild(appElement);
    });

    renderDock();
    saveAppPositions();
}

function createAppElement(app) {
    const appElement = document.createElement('div');
    appElement.className = 'app-item';
    appElement.setAttribute('data-package', app.packageName);
    
    appElement.innerHTML = `
        <img src="data:image/png;base64,${app.icon}" 
             class="app-icon" alt="${app.name}"
             onerror="this.style.background='rgba(255,255,255,0.1)'">
        <div class="app-name">${app.name}</div>
    `;

    // Configurar eventos de gestos
    setupGestureEvents(appElement, app);

    return appElement;
}

function setupGestureEvents(appElement, app) {
    // Eventos táctiles
    appElement.addEventListener('touchstart', handleGestureStart.bind(null, appElement, app));
    appElement.addEventListener('touchmove', handleGestureMove);
    appElement.addEventListener('touchend', handleGestureEnd);
    
    // Eventos de ratón
    appElement.addEventListener('mousedown', handleGestureStart.bind(null, appElement, app));
    appElement.addEventListener('mousemove', handleGestureMove);
    appElement.addEventListener('mouseup', handleGestureEnd);
    appElement.addEventListener('mouseleave', handleGestureCancel);
}

function handleGestureStart(appElement, app, event) {
    const isTouch = event.type.includes('touch');
    const clientX = isTouch ? event.touches[0].clientX : event.clientX;
    const clientY = isTouch ? event.touches[0].clientY : event.clientY;

    // Reiniciar estado del gesto
    currentGesture = {
        appElement: appElement,
        packageName: app.packageName,
        startTime: Date.now(),
        startX: clientX,
        startY: clientY,
        timer: null,
        isDragging: false,
        hasMoved: false,
        dragElement: null
    };

    // Iniciar temporizador para feedback visual
    currentGesture.timer = setTimeout(() => {
        if (!currentGesture.hasMoved) {
            showGestureFeedback('Mover para reordenar', '↕️');
            appElement.classList.add('press-1s');
        }
    }, GESTURE_TIMING.DRAG_START);

    // Temporizador para favoritos
    setTimeout(() => {
        if (currentGesture.appElement === appElement && !currentGesture.hasMoved && !currentGesture.isDragging) {
            showGestureFeedback('Agregar a favoritos', '⭐');
            appElement.classList.remove('press-1s');
            appElement.classList.add('press-2s');
        }
    }, GESTURE_TIMING.FAVORITE);

    // Temporizador para ocultar
    setTimeout(() => {
        if (currentGesture.appElement === appElement && !currentGesture.hasMoved && !currentGesture.isDragging) {
            showGestureFeedback('Ocultar aplicación', '👁️');
            appElement.classList.remove('press-2s');
            appElement.classList.add('press-4s');
        }
    }, GESTURE_TIMING.HIDE);

    // Temporizador para desinstalar
    setTimeout(() => {
        if (currentGesture.appElement === appElement && !currentGesture.hasMoved && !currentGesture.isDragging) {
            showGestureFeedback('Desinstalar aplicación', '🗑️');
            appElement.classList.remove('press-4s');
            appElement.classList.add('press-6s');
        }
    }, GESTURE_TIMING.UNINSTALL);

    if (!isTouch) {
        event.preventDefault();
    }
}

function handleGestureMove(event) {
    if (!currentGesture.appElement) return;

    const isTouch = event.type.includes('touch');
    const clientX = isTouch ? event.touches[0].clientX : event.clientX;
    const clientY = isTouch ? event.touches[0].clientY : event.clientY;

    const deltaX = Math.abs(clientX - currentGesture.startX);
    const deltaY = Math.abs(clientY - currentGesture.startY);
    const totalMove = Math.sqrt(deltaX * deltaX + deltaY * deltaY);

    // Verificar si se superó el umbral de movimiento
    if (totalMove > MOVE_THRESHOLD) {
        currentGesture.hasMoved = true;
        
        // Si ha pasado el tiempo mínimo para arrastre, activarlo
        const elapsedTime = Date.now() - currentGesture.startTime;
        if (elapsedTime >= GESTURE_TIMING.DRAG_START && !currentGesture.isDragging) {
            startDrag(currentGesture.appElement, clientX, clientY);
        }
        
        // Si estamos arrastrando, actualizar posición
        if (currentGesture.isDragging) {
            updateDragPosition(clientX, clientY);
            checkAutoScroll(clientY);
        }
    }

    if (!isTouch) {
        event.preventDefault();
    }
}

function handleGestureEnd(event) {
    if (!currentGesture.appElement) return;

    const elapsedTime = Date.now() - currentGesture.startTime;
    const isTouch = event.type.includes('touch');

    // Limpiar temporizadores y clases
    clearTimeout(currentGesture.timer);
    currentGesture.appElement.classList.remove('press-1s', 'press-2s', 'press-4s', 'press-6s');
    hideGestureFeedback();
    stopAutoScroll();

    // Determinar la acción basada en el tiempo y movimiento
    if (!currentGesture.hasMoved && !currentGesture.isDragging) {
        if (elapsedTime < GESTURE_TIMING.TAP) {
            // Tap rápido: lanzar app
            launchApp(currentGesture.packageName);
        } else if (elapsedTime >= GESTURE_TIMING.FAVORITE && elapsedTime < GESTURE_TIMING.HIDE) {
            // Mantenimiento de 2 segundos: favoritos
            toggleFavorite(currentGesture.packageName);
        } else if (elapsedTime >= GESTURE_TIMING.HIDE && elapsedTime < GESTURE_TIMING.UNINSTALL) {
            // Mantenimiento de 4 segundos: ocultar
            toggleAppVisibility(currentGesture.packageName);
        } else if (elapsedTime >= GESTURE_TIMING.UNINSTALL) {
            // Mantenimiento de 6 segundos: desinstalar (con confirmación)
            showUninstallConfirmation(currentGesture.packageName, currentGesture.appElement.querySelector('.app-name').textContent);
        }
    }

    // Finalizar arrastre si estaba activo
    if (currentGesture.isDragging) {
        endDrag();
    }

    // Reiniciar gesto actual
    currentGesture = {
        appElement: null,
        packageName: null,
        startTime: 0,
        startX: 0,
        startY: 0,
        timer: null,
        isDragging: false,
        hasMoved: false,
        dragElement: null
    };

    if (!isTouch) {
        event.preventDefault();
    }
}

function handleGestureCancel() {
    if (currentGesture.appElement) {
        clearTimeout(currentGesture.timer);
        currentGesture.appElement.classList.remove('press-1s', 'press-2s', 'press-4s', 'press-6s');
        hideGestureFeedback();
        stopAutoScroll();
        
        if (currentGesture.isDragging) {
            endDrag();
        }
        
        currentGesture = {
            appElement: null,
            packageName: null,
            startTime: 0,
            startX: 0,
            startY: 0,
            timer: null,
            isDragging: false,
            hasMoved: false,
            dragElement: null
        };
    }
}

// ========== SISTEMA DE DRAG AND DROP MEJORADO ==========

function startDrag(appElement, clientX, clientY) {
    currentGesture.isDragging = true;
    isReordering = true;
    
    // Crear elemento de arrastre
    const dragElement = appElement.cloneNode(true);
    dragElement.style.position = 'fixed';
    dragElement.style.zIndex = '10000';
    dragElement.style.pointerEvents = 'none';
    dragElement.style.transform = 'rotate(5deg) scale(1.1)';
    dragElement.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.5)';
    dragElement.classList.add('dragging');
    
    document.body.appendChild(dragElement);
    currentGesture.dragElement = dragElement;
    
    // Ocultar elemento original temporalmente
    appElement.style.opacity = '0.3';
    
    updateDragPosition(clientX, clientY);
    hideGestureFeedback();
}

function updateDragPosition(clientX, clientY) {
    if (!currentGesture.dragElement) return;
    
    const dragElement = currentGesture.dragElement;
    const rect = dragElement.getBoundingClientRect();
    
    // Posicionar el elemento de arrastre centrado en el puntero
    dragElement.style.left = (clientX - rect.width / 2) + 'px';
    dragElement.style.top = (clientY - rect.height / 2) + 'px';
    
    // Verificar reordenación
    checkReordering(clientX, clientY);
}

function checkReordering(clientX, clientY) {
    const appElements = document.querySelectorAll('.app-item:not(.dragging)');
    let closestElement = null;
    let closestDistance = Infinity;
    
    appElements.forEach(element => {
        if (element.style.opacity !== '0.3') { // No considerar el elemento que se está arrastrando
            const rect = element.getBoundingClientRect();
            const elementCenterX = rect.left + rect.width / 2;
            const elementCenterY = rect.top + rect.height / 2;
            
            const distance = Math.sqrt(
                Math.pow(clientX - elementCenterX, 2) + 
                Math.pow(clientY - elementCenterY, 2)
            );
            
            if (distance < closestDistance) {
                closestDistance = distance;
                closestElement = element;
            }
        }
    });
    
    // Resaltar el elemento más cercano
    appElements.forEach(element => {
        element.style.transform = '';
        element.style.background = '';
    });
    
    if (closestElement && closestDistance < 100) { // Umbral de 100px
        closestElement.style.transform = 'scale(1.05)';
        closestElement.style.background = 'rgba(0, 212, 255, 0.2)';
        currentGesture.dropTarget = closestElement;
    } else {
        currentGesture.dropTarget = null;
    }
}

function endDrag() {
    if (currentGesture.dragElement) {
        // Eliminar elemento de arrastre
        currentGesture.dragElement.remove();
        currentGesture.dragElement = null;
    }
    
    // Restaurar opacidad del elemento original
    if (currentGesture.appElement) {
        currentGesture.appElement.style.opacity = '';
    }
    
    // Aplicar reordenación si hay un objetivo
    if (currentGesture.dropTarget) {
        const targetPackage = currentGesture.dropTarget.getAttribute('data-package');
        const sourcePackage = currentGesture.packageName;
        
        reorderApps(sourcePackage, targetPackage);
        
        // Restaurar estilo del objetivo
        currentGesture.dropTarget.style.transform = '';
        currentGesture.dropTarget.style.background = '';
    }
    
    // Restaurar todos los elementos
    document.querySelectorAll('.app-item').forEach(element => {
        element.style.transform = '';
        element.style.background = '';
    });
    
    currentGesture.isDragging = false;
    isReordering = false;
    currentGesture.dropTarget = null;
}

function reorderApps(sourcePackage, targetPackage) {
    const sourceIndex = allApps.findIndex(app => app.packageName === sourcePackage);
    const targetIndex = allApps.findIndex(app => app.packageName === targetPackage);
    
    if (sourceIndex !== -1 && targetIndex !== -1 && sourceIndex !== targetIndex) {
        // Mover la app en el array
        const [movedApp] = allApps.splice(sourceIndex, 1);
        allApps.splice(targetIndex, 0, movedApp);
        
        // Actualizar posiciones
        allApps.forEach((app, index) => {
            appPositions[app.packageName] = index;
        });
        
        saveAppPositions();
        renderApps();
        
        if (typeof Android !== 'undefined' && Android.showToast) {
            Android.showToast("📱 Apps reordenadas");
        }
    }
}

// ========== AUTO SCROLL DURING DRAG ==========

function checkAutoScroll(clientY) {
    const container = document.getElementById('appsContainer');
    const containerRect = container.getBoundingClientRect();
    const scrollThreshold = 100; // 100px desde los bordes
    
    // Detener scroll anterior
    stopAutoScroll();
    
    // Verificar si necesita scroll hacia arriba
    if (clientY < containerRect.top + scrollThreshold) {
        startAutoScroll(-1);
    }
    // Verificar si necesita scroll hacia abajo
    else if (clientY > containerRect.bottom - scrollThreshold) {
        startAutoScroll(1);
    }
}

function startAutoScroll(direction) {
    const container = document.getElementById('appsContainer');
    const scrollSpeed = 20;
    
    dragScrollInterval = setInterval(() => {
        container.scrollTop += scrollSpeed * direction;
    }, 16); // ~60fps
}

function stopAutoScroll() {
    if (dragScrollInterval) {
        clearInterval(dragScrollInterval);
        dragScrollInterval = null;
    }
}

// ========== FUNCIÓN DE DESINSTALACIÓN CON CONFIRMACIÓN ==========

function showUninstallConfirmation(packageName, appName) {
    // Crear modal de confirmación
    const modal = document.createElement('div');
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.8);
        backdrop-filter: blur(10px);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 10000;
    `;
    
    modal.innerHTML = `
        <div style="
            background: var(--primary-bg);
            backdrop-filter: blur(40px);
            border: 1px solid var(--glass-border);
            border-radius: 20px;
            padding: 30px;
            max-width: 80%;
            text-align: center;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
        ">
            <div style="font-size: 1.4em; margin-bottom: 10px; color: var(--danger-color);">⚠️</div>
            <div style="font-size: 1.2em; margin-bottom: 10px; font-weight: 600;">Desinstalar aplicación</div>
            <div style="margin-bottom: 20px; color: var(--text-secondary);">
                ¿Estás seguro de que quieres desinstalar "<strong>${appName}</strong>"?
            </div>
            <div style="display: flex; gap: 10px; justify-content: center;">
                <button id="confirmUninstall" style="
                    padding: 12px 24px;
                    background: var(--danger-color);
                    color: white;
                    border: none;
                    border-radius: 10px;
                    cursor: pointer;
                    font-weight: 500;
                ">Desinstalar</button>
                <button id="cancelUninstall" style="
                    padding: 12px 24px;
                    background: var(--secondary-bg);
                    color: var(--text-primary);
                    border: none;
                    border-radius: 10px;
                    cursor: pointer;
                    font-weight: 500;
                ">Cancelar</button>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    
    // Event listeners para los botones
    document.getElementById('confirmUninstall').addEventListener('click', function() {
        if (typeof Android !== 'undefined' && Android.uninstallApp) {
            Android.uninstallApp(packageName);
        }
        modal.remove();
    });
    
    document.getElementById('cancelUninstall').addEventListener('click', function() {
        modal.remove();
    });
    
    // Cerrar modal al hacer click fuera
    modal.addEventListener('click', function(e) {
        if (e.target === modal) {
            modal.remove();
        }
    });
}

// ========== FUNCIONES EXISTENTES (actualizadas) ==========

function showGestureFeedback(text, icon) {
    const indicator = document.getElementById('gestureIndicator');
    const gestureText = document.getElementById('gestureText');
    const gestureIcon = document.getElementById('gestureIcon');
    
    gestureText.textContent = text;
    gestureIcon.textContent = icon;
    indicator.style.display = 'block';
}

function hideGestureFeedback() {
    const indicator = document.getElementById('gestureIndicator');
    indicator.style.display = 'none';
}

function showHiddenApps() {
    if (hiddenApps.length > 0) {
        hiddenApps = [];
        if (typeof Android !== 'undefined' && Android.showToast) {
            Android.showToast("👁️ Apps ocultas mostradas");
        }
    } else {
        hiddenApps = JSON.parse(localStorage.getItem('hiddenApps')) || [];
        if (typeof Android !== 'undefined' && Android.showToast) {
            Android.showToast("👁️‍🗨️ Apps ocultas restauradas");
        }
    }
    renderApps();
    updateAppStats();
}

function renderDock() {
    const dock = document.getElementById('dockScroll');
    dock.innerHTML = '';

    favoriteApps.forEach(packageName => {
        const app = allApps.find(a => a.packageName === packageName);
        if (app) {
            const dockApp = document.createElement('img');
            dockApp.className = 'dock-app';
            dockApp.src = `data:image/png;base64,${app.icon}`;
            dockApp.alt = app.name;
            dockApp.title = app.name;
            dockApp.onclick = () => launchApp(app.packageName);
            dock.appendChild(dockApp);
        }
    });

    if (favoriteApps.length === 0) {
        dock.innerHTML = '<div style="color: var(--text-secondary); padding: 16px; text-align: center; font-size: 14px;">Mantén 2s en cualquier app para agregar al dock</div>';
    }
}

function launchApp(packageName) {
    if (typeof Android !== 'undefined' && Android.launchApp) {
        Android.launchApp(packageName);
    }
}

function toggleFavorite(packageName) {
    const index = favoriteApps.indexOf(packageName);
    if (index > -1) {
        favoriteApps.splice(index, 1);
        if (typeof Android !== 'undefined' && Android.showToast) {
            Android.showToast("❌ Removido de favoritos");
        }
    } else {
        favoriteApps.push(packageName);
        if (typeof Android !== 'undefined' && Android.showToast) {
            Android.showToast("⭐ Agregado a favoritos");
        }
    }
    localStorage.setItem('favoriteApps', JSON.stringify(favoriteApps));
    renderApps();
    updateAppStats();
}

function toggleAppVisibility(packageName) {
    const index = hiddenApps.indexOf(packageName);
    if (index > -1) {
        hiddenApps.splice(index, 1);
        if (typeof Android !== 'undefined' && Android.showToast) {
            Android.showToast("👁️ App mostrada");
        }
    } else {
        hiddenApps.push(packageName);
        if (typeof Android !== 'undefined' && Android.showToast) {
            Android.showToast("👁️‍🗨️ App ocultada");
        }
    }
    localStorage.setItem('hiddenApps', JSON.stringify(hiddenApps));
    renderApps();
    updateAppStats();
}

function filterApps() {
    const searchTerm = document.getElementById('searchInput').value.toLowerCase();
    const appItems = document.querySelectorAll('.app-item');
    
    let visibleCount = 0;
    appItems.forEach(item => {
        const appName = item.querySelector('.app-name').textContent.toLowerCase();
        if (appName.includes(searchTerm)) {
            item.classList.remove('hidden');
            visibleCount++;
        } else {
            item.classList.add('hidden');
        }
    });
    
    updateAppStats(visibleCount);
}

function updateTime() {
    const now = new Date();
    const timeString = now.toLocaleTimeString('es-ES', { 
        hour: '2-digit', 
        minute: '2-digit' 
    });
    const dateString = now.toLocaleDateString('es-ES', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
    });

    document.getElementById('currentTime').textContent = timeString;
    document.getElementById('currentDate').textContent = 
        dateString.charAt(0).toUpperCase() + dateString.slice(1);
}

function updateAppStats(visibleCount = null) {
    const totalVisible = visibleCount !== null ? visibleCount : allApps.filter(app => !hiddenApps.includes(app.packageName)).length;
    const statsElement = document.getElementById('appStats');
    
    statsElement.innerHTML = `
        ${totalVisible} apps • ${favoriteApps.length} favoritos • ${hiddenApps.length} ocultas
    `;
}

function saveAppPositions() {
    localStorage.setItem('appPositions', JSON.stringify(appPositions));
}

// Funciones para fondo de pantalla
function openWallpaperPicker() {
    document.getElementById('wallpaperModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('wallpaperModal').style.display = 'none';
}

function selectWallpaperFromGallery() {
    document.getElementById('wallpaperInput').click();
    closeModal();
}

function useDefaultWallpaper() {
    document.body.style.backgroundImage = '';
    localStorage.removeItem('customWallpaper');
    closeModal();
    if (typeof Android !== 'undefined' && Android.showToast) {
        Android.showToast("🎨 Fondo restaurado");
    }
}

function handleWallpaperSelect(e) {
    const file = e.target.files[0];
    if (file) {
        // Validar tipo de archivo
        if (!file.type.startsWith('image/')) {
            if (typeof Android !== 'undefined' && Android.showToast) {
                Android.showToast("❌ Solo se permiten imágenes");
            }
            return;
        }

        // Validar tamaño (máximo 10MB)
        if (file.size > 10 * 1024 * 1024) {
            if (typeof Android !== 'undefined' && Android.showToast) {
                Android.showToast("❌ Imagen demasiado grande (máx. 10MB)");
            }
            return;
        }

        const reader = new FileReader();
        reader.onload = function(e) {
            const imageUrl = e.target.result;
            document.body.style.backgroundImage = `url(${imageUrl})`;
            localStorage.setItem('customWallpaper', imageUrl);
            if (typeof Android !== 'undefined' && Android.showToast) {
                Android.showToast("🎨 Fondo actualizado");
            }
        };
        reader.onerror = function() {
            if (typeof Android !== 'undefined' && Android.showToast) {
                Android.showToast("❌ Error al cargar la imagen");
            }
        };
        reader.readAsDataURL(file);
    }
}

// Cargar fondo personalizado al iniciar
const savedWallpaper = localStorage.getItem('customWallpaper');
if (savedWallpaper) {
    document.body.style.backgroundImage = `url(${savedWallpaper})`;
}