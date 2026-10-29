# Motion

Aplicación nativa para Android desarrollada en Kotlin y Jetpack Compose, diseñada para prevenir y reducir los síntomas del mareo por movimiento (cinetosis) al utilizar el dispositivo dentro de vehículos en marcha.

---

## Descripción

El mareo durante los viajes ocurre cuando existe un conflicto sensorial entre lo que perciben los ojos (una pantalla estática) y lo que siente el oído interno (la aceleración y las curvas del vehículo).

Motion proyecta indicadores visuales sutiles en la periferia de la pantalla mediante una superposición de sistema que se desplaza de manera inercial en respuesta al movimiento del vehículo. Esto permite al usuario leer o ver contenido mientras su visión periférica se mantiene alineada con el movimiento real del entorno.

---

## Características

- **Diseño minimalista:** Pantalla principal enfocada en un único botón central táctil para alternar entre iniciar y pausar el servicio.
- **Sin anuncios:** Experiencia limpia, sin interrupciones ni recolección de datos innecesaria.
- **Personalización en tiempo real:**
  - **Forma:** Círculos, cuadrados y líneas tenues.
  - **Tamaño:** Control deslizante de escala de los puntos.
  - **Color:** Paleta seleccionada con tonos de alto contraste y selector continuo de espectro.
- **Ajustes Rápidos (Quick Settings Tile):**
  - Botón integrado para el panel de notificaciones y ajustes rápidos del sistema.
  - Toque corto: Inicia o pausa el overlay al instante.
  - Pulsación larga: Abre directamente la configuración de apariencia de la aplicación.
- **Eficiencia y seguridad:**
  - Procesamiento de acelerómetro mediante doble filtro pasa-bajos para eliminar vibraciones y ruidos de alta frecuencia.
  - Suspensión automática del bucle de dibujo cuando el dispositivo está en reposo o con la pantalla apagada.
  - Configuración de opacidad que previene el bloqueo de eventos táctiles en aplicaciones de terceros y elementos del sistema.

---

## Requisitos

- Android 7.0 (API nivel 24) o superior.
- Dispositivo con sensor de acelerómetro.
- Permiso de superposición concedido («Mostrar encima de otras aplicaciones»).

---

## Compilación

Para compilar el proyecto en modo depuración y generar el archivo APK:

```bash
./gradlew assembleDebug
```

Para ejecutar las pruebas unitarias del motor físico:

```bash
./gradlew testDebugUnitTest
```
