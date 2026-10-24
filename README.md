# AutoMirror — espejo de pantalla del teléfono en el coche (Android Auto, USB)

App **libre** que muestra la pantalla del teléfono en la unidad de cabeza del coche
cuando lo conectas por cable. Compila en GitHub Actions y descargas el APK listo
para instalar en el teléfono.

## Cómo funciona

1. `MainActivity` pide el consentimiento de **MediaProjection** (captura de pantalla).
2. `MirrorService` (servicio en primer plano) mantiene la captura en una pantalla virtual.
3. El host de Android Auto (al conectar el cable) enlaza `MirrorCarAppService`.
4. `MirrorScreen` registra un `SurfaceCallback`; cuando el coche entrega su `Surface`,
   `MirrorService` redirige la captura directamente a esa surface (sin recomprimir:

   phone → VirtualDisplay → Surface del coche).
5. Con el template `MapWithContentTemplate`, la app aparece en el launcher del coche.

## Compilar (tu APK gratis sin PC)

1. Crea un repositorio (por ejemplo `automirror`) en github.com.
2. Sube el contenido de esta carpeta al repo.
3. Actions → **Build APK** → al terminar descarga el artifact `automirror-debug-apk`.
4. Instala el APK en el teléfono (permiso de fuentes desconocidas).

## Uso

1. Instala `AutoMirror` en el teléfono.
2. Abre la app y **Iniciar espejo** (consentimiento de captura + notificaciones).
3. Conecta el cable al coche.
4. En la columna de apps de Android Auto elige **AutoMirror** → verás la pantalla
   del teléfono en el display del coche.

## Estado actual (MVP sin control táctil táctil del coche)

- ✅ Espejo de pantalla (con cable en Android Auto).
- ❗ Los toques desde el display del coche NO actúan sobre el teléfono todavía
  (fase 2: inyección de gestos vía AccessibilityService, tal como hace ScreenOnAuto).

## Notas

- La app no envía datos a internet: todo es local.
- Desactiva batería optimizada para el app (HyperOS mi celular): común en la CUARta de batería.

- Firma: el APK debug se instala directo.
