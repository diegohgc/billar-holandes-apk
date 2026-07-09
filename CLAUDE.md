# Billar Holandés — Wrapper Android (APK) de Sjoelen

Proyecto Android (Kotlin, package `com.diegohgc.billarholandes`) que empaqueta el juego web
"Billar Holandés" (Sjoelen 3D) como APK instalable. Calcado de `diegohgc/tandas-2`
(wrapper de Turnos Halterofilia), pero es un proyecto totalmente independiente —
package y repo propios, sin relación con `tandas-2` ni con `temperatura-android` (QuickTemp).

## Arquitectura
- Es un **WebView** (ver `app/src/main/java/com/diegohgc/billarholandes/MainActivity.kt`) que
  carga `https://diegohgc.github.io/billar-holandes/` (repo web aparte:
  `diegohgc/billar-holandes`, código fuente local en `../sjoelen-game`).
- La APK NO contiene el HTML. Para cambiar el juego se edita el HTML en el repo web; esta APK
  recoge los cambios sola al abrirse. Solo se recompila si cambia icono, nombre o la URL.
- A diferencia de Turnos Halterofilia, este juego no usa exportar/importar JSON nativo
  (solo `localStorage` para el récord), así que `MainActivity.kt` es un WebView simple sin
  lógica de descarga/subida de archivos.
- Banner de AdMob: pendiente de configurar (hueco ya reservado en el HTML del juego con
  la variable CSS `--ad-h`).

## Compilar la APK
- Desde Git Bash en la raíz: `./gradlew assembleDebug` (más fiable que los menús de Android Studio).
- Salida: `app/build/outputs/apk/debug/`.

## Notas para equipos modestos (Windows)
- Windows Defender ralentiza Android Studio: aceptar "Exclude folders" del aviso (permiso admin).
- `gradle.properties` usa `-Xmx4096m -XX:MaxMetaspaceSize=512m` (subido de 2GB por
  OutOfMemoryError en `mergeExtDexDebug`/R8).
- Si tras cambiar la memoria sigue el OOM: `./gradlew --stop` (mata el daemon viejo) y recompilar.

## Flujo de trabajo (2 ordenadores: A principal, B portátil)
- Al empezar: `git pull`. Al terminar: `git add -A && git commit -m "..." && git push`.
- La memoria de Claude Code es local de cada equipo y no se sincroniza; este CLAUDE.md viaja con el repo.
