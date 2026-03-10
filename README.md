# Rick & Morty — Android Senior Demo

> Aplicación Android que consume la [Rick and Morty API](https://rickandmortyapi.com/api/character) construida con arquitectura multi-módulo, Clean Architecture, SDUI con Firebase Remote Config y un sistema de diseño dark sci-fi. Cada decisión técnica está documentada con su justificación.

---

## ¿Por qué deberías contratarme?

Cualquiera puede seguir un tutorial y pegar código. La diferencia está en entender **por qué** se toma cada decisión y cuáles son sus trade-offs. Este proyecto lo demuestra:

- **Módulos independientes**: ninguna feature puede importar directamente el código de otra. Esto no es estético — es la diferencia entre un build de 8 minutos y uno de 45 cuando el proyecto escala.
- **SDUI real**: la UI entera se puede cambiar desde Firebase sin un nuevo APK. En producción, esto significa que producto puede hacer A/B testing de textos y comportamiento sin esperar un sprint de desarrollo.
- **Errores que no se tragan silenciosamente**: `CancellationException` rompe las coroutines si se captura accidentalmente. `BaseViewModel` lo previene en un solo lugar en lugar de en cada ViewModel.
- **Tipado explícito en toda la capa de dominio**: el UseCase devuelve `Result<T>`, no lanza excepciones. El caller decide qué hacer con el error — no el callee.
- **Paginación implementada desde cero**: `LazyListState` + `snapshotFlow` + detección de último elemento visible. Entender el mecanismo es más valioso que saber pasar parámetros a `Paging3`.
- **Tests sin mocks cuando es posible**: el integration test usa un `FakeCharacterRepository` en memoria, no `mockk { every { ... } }`. Los fakes son más mantenibles y más fieles al comportamiento real.

---

## Arquitectura

El proyecto sigue **MVVM + Clean Architecture** con separación estricta por capas:

```
┌──────────────────────────────────────────┐
│              Capa UI                     │
│  CharacterListScreen, CharacterCard,     │
│  LoadingSkeleton, ErrorView,             │
│  StateDemoBottomSheet                    │
└────────────────────┬─────────────────────┘
                     │ observa StateFlow
┌────────────────────▼─────────────────────┐
│          Capa de Presentación            │
│  CharacterListViewModel : BaseViewModel  │
│  CharacterUiState (sealed class)         │
│  ScreenState (ui + refresh + sdui)       │
└────────────────────┬─────────────────────┘
                     │ invoca
┌────────────────────▼─────────────────────┐
│            Capa de Dominio               │
│  GetCharactersUseCase → Result<T>        │
│  CharacterRepository (interfaz)          │
│  Character, CharacterStatus (modelos)    │
└────────────────────┬─────────────────────┘
                     │ implementado por
┌────────────────────▼─────────────────────┐
│            Capa de Datos                 │
│  CharacterRepositoryImpl                 │
│  RickMortyApi (Retrofit)                 │
│  CharacterDto → CharacterMapper          │
└──────────────────────────────────────────┘
```

---

## Módulos y decisiones de diseño

El proyecto está dividido en módulos Gradle independientes. Cada módulo solo conoce a sus dependencias declaradas — ninguna feature accede al código de otra directamente. Esto no es solo una decisión de organización: es lo que permite compilaciones incrementales rápidas y límites de responsabilidad claros.

---

### `core:network`

Configura **Retrofit 2** + **OkHttp 4** con un `HttpLoggingInterceptor` en debug y define la interfaz `RickMortyApi`.

**¿Por qué Retrofit y no Ktor?** Retrofit es la opción establecida en el ecosistema Android. Soporta `suspend functions` nativamente, tiene generación de código en tiempo de compilación y el equipo de cualquier empresa Android ya lo conoce. Ktor sería válido en KMP, pero aquí no aplica.

**¿Por qué OkHttp?** Retrofit lo usa como transport layer. OkHttp gestiona connection pooling, retry on failure y el interceptor de logging. Configurarlo explícitamente permite añadir interceptores de autenticación en el futuro sin tocar `Retrofit`.

**¿Por qué Kotlinx Serialization y no Gson/Moshi?** Kotlinx Serialization es procesado en tiempo de compilación con KSP — no usa reflection en runtime. Gson usa reflection, lo que tiene coste en arranque y puede fallar con ProGuard/R8. Moshi con codegen es similar, pero Kotlinx es el estándar de Kotlin multiplataforma.

---

### `core:ui`

Sistema de diseño centralizado: `Color.kt`, `Typography.kt`, `Theme.kt`.

**¿Por qué un módulo separado para el tema?** Si el tema vive en `app/`, cada módulo que necesite colores o tipografía dependería de `app/`, creando una dependencia inversa que rompe Clean Architecture. Al estar en `core:ui`, cualquier módulo lo importa sin ciclos.

**¿Por qué dark theme forzado sin dynamic color?** Dynamic color (Material You) toma los colores del wallpaper del sistema. Para una app con identidad visual propia (paleta sci-fi específica), dynamic color destruiría la consistencia. Se fuerza el dark scheme con `darkColorScheme()` explícito.

**Paleta de colores y su origen:**
```kotlin
val PortalTeal   = Color(0xFF00D4AA) // acento principal — verde portal
val DeepSpace    = Color(0xFF050510) // fondo — negro casi puro con tinte azul
val SpaceSurface = Color(0xFF0D0D1A) // superficie de cards
val DeadRed      = Color(0xFFFF4D6D) // error y estado muerto
val StarWhite    = Color(0xFFF0F0FF) // texto primario — blanco con tinte frío
val MutedBlue    = Color(0xFF9696BE) // texto secundario
```
Extraída de un proyecto de referencia web (React + CSS variables) y adaptada al sistema semántico de Material 3.

---

### `core:sdui` — Server-Driven UI

**La decisión técnica más diferenciadora del proyecto.**

En producción, el negocio necesita cambiar textos, flags, labels y comportamiento sin esperar un release (que en Android puede tardar días por revisión de Play Store). SDUI lo resuelve: la app descarga su configuración de Firebase Remote Config y adapta la UI en runtime.

**Flujo completo:**
```
Firebase Remote Config (JSON)
        ↓ fetchAndActivate()
  ScreenConfigDto  (capa de datos — Kotlinx Serialization)
        ↓ toDomain()
  ScreenConfig     (capa de dominio — modelos puros sin dependencias)
        ↓ StateFlow
  UI Composables   (consumen la config como parámetros)
```

**¿Por qué un DTO separado del modelo de dominio?**
El DTO refleja la estructura del JSON externo — si Firebase cambia el nombre de un campo, solo se toca el DTO y el mapper. El modelo de dominio es estable y no depende de la fuente de datos.

**¿Por qué un `fallbackConfig()`?**
Si Firebase no responde (sin internet, timeout, error de configuración), la app muestra la UI con valores por defecto razonables en lugar de crashear o quedarse en blanco.

**Qué es configurable desde Remote Config:**
- Título y visibilidad del TopBar
- Shape de la imagen en las cards (`circle` / `square`)
- Labels de estado (`Vivo`, `Muerto`, `Desconocido`)
- Textos del panel de demo de estados
- Mensaje de error y label del botón Reintentar
- Número de skeletons de carga

---

### `core:presentation`

Contiene `BaseViewModel`: clase base abstracta que centraliza el lanzamiento de coroutines con manejo correcto de errores.

```kotlin
fun launchWithErrorHandling(
    onError: (Throwable) -> Unit,
    block: suspend CoroutineScope.() -> Unit
) {
    viewModelScope.launch {
        try { block() }
        catch (e: CancellationException) { throw e }
        catch (e: Throwable) { onError(e) }
    }
}
```

**¿Por qué no `runCatching`?**
`runCatching` envuelve **todas** las excepciones incluyendo `CancellationException`. Cuando una coroutine es cancelada (p.ej. el usuario sale de la pantalla), Kotlin usa `CancellationException` como señal de control. Si se atrapa, la coroutine sigue ejecutándose aunque ya no debería — memoria leakeada, llamadas a red innecesarias, y potencialmente crashes en producción al intentar actualizar estado de un ViewModel destruido.

**¿Por qué no `try/catch` en cada ViewModel?**
Porque es código duplicado. Si en el futuro se necesita añadir logging de errores con Crashlytics, se añade en un solo lugar: `BaseViewModel.launchWithErrorHandling`.

---

### `feature:characters`

Única feature del proyecto. Organizada siguiendo Clean Architecture en 4 capas:

| Capa | Responsabilidad | Regla |
|---|---|---|
| `data/` | Implementa repositorios, mapea DTOs | Puede importar `domain/` y librerías de red |
| `domain/` | Modelos puros, interfaces, use cases | **Sin dependencias de Android ni librerías externas** |
| `presentation/` | ViewModel, estado UI | Puede importar `domain/` y coroutines |
| `ui/` | Composables, pantallas | Solo consume `presentation/` y `core:ui` |

**¿Por qué Clean Architecture en una app de una sola pantalla?**
Porque el proyecto demuestra escalabilidad, no solo funcionalidad. En una app real con 10 features, esta estructura permite que cada feature sea desarrollada y testeada de forma independiente. Los tests unitarios de la capa `domain/` no necesitan Android SDK — corren en JVM pura, lo que los hace extremadamente rápidos.

---

## Manejo de estados

```kotlin
sealed class CharacterUiState {
    object Loading : CharacterUiState()
    data class Success(val characters: ImmutableList<Character>, val hasNextPage: Boolean) : CharacterUiState()
    data class LoadingMore(val characters: ImmutableList<Character>) : CharacterUiState()
    data class Error(val message: String) : CharacterUiState()
}
```

El ViewModel combina tres `StateFlow` en un `ScreenState` mediante `combine()`, garantizando una única fuente de verdad para toda la UI.

### Panel de demo de estados
Presiona el botón **⚙** en la pantalla para abrir el panel y forzar cualquier estado:
- **Simular carga** — shimmer skeleton animado
- **Simular error** — vista de error con botón Reintentar
- **Restaurar datos** — recarga desde página 1

Los textos del panel vienen de Firebase Remote Config (`demo` block en `screen_config`).

---

## SDUI — JSON de Firebase Remote Config

Parámetro: `screen_config`

```json
{
  "topBar": { "title": "Rick & Morty", "visible": true },
  "card": {
    "imageShape": "circle",
    "showStatusChip": true,
    "statusLabels": { "alive": "Vivo", "dead": "Muerto", "unknown": "Desconocido" }
  },
  "errorView": { "title": "Algo salió mal", "retryLabel": "Reintentar" },
  "list": { "skeletonCount": 6, "animationDurationMs": 300 },
  "demo": {
    "title": "Demo de estados",
    "subtitle": "Fuerza un estado de UI para verificar cada escenario.",
    "loadingLabel": "Simular carga",
    "loadingDescription": "Muestra el skeleton animado",
    "restoreLabel": "Restaurar datos",
    "restoreDescription": "Recarga desde la página 1",
    "errorLabel": "Simular error",
    "errorDescription": "Muestra la vista de error con Reintentar"
  }
}
```

---

## Stack técnico

| Librería | Versión | Decisión |
|---|---|---|
| **Jetpack Compose** | BOM 2024.09 | UI declarativa — menos código, mejor gestión de estado |
| **Hilt** | 2.51.1 | DI estándar en Android; integración nativa con Compose y ViewModel |
| **Retrofit + OkHttp** | 2.11 / 4.12 | Cliente HTTP de facto; soporte nativo de coroutines |
| **Kotlinx Serialization** | 1.7.3 | Más rápido que Gson/Moshi con Kotlin; necesario para el módulo SDUI |
| **Coil** | 2.7.0 | Image loader Kotlin-first; `AsyncImage` nativo, caché memoria + disco |
| **Firebase Remote Config** | BoM 33 | SDUI — configuración en tiempo real sin nuevo release |
| **ImmutableCollections** | 0.3.8 | Evita recomposiciones innecesarias en `LazyColumn` con listas grandes |
| **MockK + Turbine** | 1.13 / 1.1 | Testing de coroutines y Flow sin boilerplate |
| **KSP** | 2.0.21 | Procesador de anotaciones; más rápido que KAPT |

---

## Tests

### Unit tests
| Clase | Qué cubre |
|---|---|
| `CharacterListViewModelTest` | Loading → Success → Error → retry → refresh |
| `CharacterRepositoryImplTest` | Mapeo DTO → dominio para los 3 estados; propagación de error |
| `GetCharactersUseCaseTest` | Delegación al repositorio; `Result<T>` correcto |
| `CharacterListViewModelIntegrationTest` | Flujo completo con fake repository (sin mocks) |

### UI tests (Compose)
| Test | Qué cubre |
|---|---|
| Skeleton visible en Loading | Shimmer presente; nombres de personajes ausentes |
| Card renderiza correctamente | Nombre y status visibles |
| Vista de error + Reintentar | Mensaje y botón presentes |
| Callback de retry | Click en botón invoca el lambda |

---

## Estructura de módulos

```
RickAndMorty/
├── app/                          # Entry point, Hilt setup, navegación
├── core/
│   ├── network/                  # Retrofit, OkHttp, interceptores
│   ├── ui/                       # Design system (Color, Theme, Typography)
│   ├── sdui/                     # Server-Driven UI (Firebase RC → dominio)
│   └── presentation/             # BaseViewModel
├── feature/
│   └── characters/
│       ├── data/                 # API, DTOs, mappers, repositorio impl
│       ├── domain/               # Modelos, interfaz repo, use case
│       ├── presentation/         # ViewModel, UiState, ScreenState
│       └── ui/                   # Pantalla, componentes, tema
└── build-logic/                  # Convention plugins de Gradle
```

---

## Ejecutar el proyecto

```bash
# Compilar
./gradlew assembleDebug

# Tests unitarios
./gradlew test

# Tests de UI (requiere emulador o dispositivo)
./gradlew connectedAndroidTest
```

**SDK mínimo:** 24 (Android 7.0)  
**SDK objetivo:** 36  
**Kotlin:** 2.0.21  
**Jetpack Compose BOM:** 2024.09

---

## Qué añadiría con más tiempo

1. **Room + Paging 3** — caché offline con `RemoteMediator`; scroll infinito verdadero
2. **Pantalla de detalle** — perfil completo del personaje con `NavController`
3. **Búsqueda** — `SearchBar` con debounce y filtrado reactivo
4. **CI/CD** — GitHub Actions con lint + tests en cada PR
5. **Crashlytics** — reporte de errores en producción
