# App de ejemplo — SDK ID Digital (Android)

Esta app demuestra el Patrón B (puente web) descrito en [`.docs/sdk/cliente/`](../../.docs/sdk/cliente/README.md) y en [`.docs/sdk/primera-asociacion-app-integradora.md`](../../.docs/sdk/primera-asociacion-app-integradora.md) §2.2: login Keycloak → aviso de verificación pendiente → la SDK resuelve asociación o validación → `completeTransaction()` cierra el login.

Tiene Firebase Cloud Messaging configurado (proyecto propio, dedicado a esta app de ejemplo — simula la infraestructura FCM propia de un Integrador, separada de la del backend/app real de ID Digital). El aviso que en producción llegaría por push (`transactionId`, `type`, `documentNumber`, ver [`03-endpoint-push.md`](../../.docs/sdk/cliente/03-endpoint-push.md)) llega como un push real y la app resuelve sola la asociación/validación y completa la transacción — ver [`fcm/IDDigitalSampleFcmService.kt`](src/main/java/com/example/iddigital/fcm/IDDigitalSampleFcmService.kt). También se puede seguir completando a mano en la pantalla "Resolver verificación pendiente" para probar sin depender de FCM.

## Configuración previa

Completar `local.properties` (no se versiona) en la raíz de `id-digital-android-sdk/`:

```properties
API_KEY=<api key del sdk.Client de prueba, entregado por ID Digital>
API_BASE_URL=<opcional, ver abajo>
KEYCLOAK_BASE_URL=https://bqm-keycloak-dev.alabamasolutions.com
KEYCLOAK_REALM=bqm-realm
KEYCLOAK_CLIENT_ID=<client_id habilitado en ese realm para esta app>
KEYCLOAK_REDIRECT_URI=iddigitalsample://auth
```

- `KEYCLOAK_BASE_URL` y `KEYCLOAK_REALM` tienen default en el build (`app/build.gradle.kts`) apuntando al staging ya usado por la SDK; solo hace falta sobreescribirlos si se prueba contra otro ambiente.
- `KEYCLOAK_CLIENT_ID` no tiene default: hay que registrar un client en ese realm y completar acá su id. Ese client no debe agregarse a `MOBILE_CLIENTS` (ni a ningún mecanismo equivalente de ese Keycloak para clients móviles nativos): el flujo estándar del SDK usa el `redirect_uri` HTTPS normal, sin deep links nativos ni pasos adicionales (ver [`02-configuracion-keycloak.md`](../../.docs/sdk/cliente/02-configuracion-keycloak.md)).
- `KEYCLOAK_REDIRECT_URI` debe coincidir con el intent-filter de `AndroidManifest.xml` (`iddigitalsample://auth`) y estar registrado como redirect URI válido de ese client en Keycloak.
- `API_BASE_URL` es opcional: sin ella, la SDK usa `IDDigitalSDKEnvironment.STAGING` (hardcodeado en `MainActivity.kt`), que apunta a `auth.identificaciondigital.com.uy`. Para que `associate()`/`createValidationSession()`/`completeTransaction()` hablen con un backend propio (ej. un `id-2.0-backend` de desarrollo, docker-compose local, o el droplet compartido de DigitalOcean — ver [`.docs/sdk/entorno-desarrollo-digitalocean.md`](../../.docs/sdk/entorno-desarrollo-digitalocean.md)), completarla con `http://<host>/api/v2/sdk` — debe ser el **mismo** backend contra el que corre el login de Keycloak/mock BQM, o `completeTransaction()` va a fallar (el `transactionId` no existiría ahí). Si el host usa HTTP plano (no HTTPS), agregarlo también a [`network_security_config.xml`](src/main/res/xml/network_security_config.xml).

## Cómo correr el flujo completo (con push real)

Requiere que el backend de `id-2.0-backend` tenga configurado el mock BQM con Firebase (ver [`.docs/sdk/mock-bqm-push-auth.md`](../../.docs/sdk/mock-bqm-push-auth.md)):

1. **Copiar el token FCM**: abrir la app, ir a "Herramientas / debug" y tocar "Copiar token FCM". Pegarlo en `SDK_MOCK_BQM_FCM_TEST_DEVICE_TOKEN` del entorno del backend (y confirmar que existe `service-account-mock-bqm.json`) y reiniciar el backend.
2. **Iniciar sesión con Keycloak**: tocar el botón correspondiente. Se abre un Custom Tab con el login del realm configurado, que redirige al broker de ID Digital y crea la transacción pendiente.
3. **Esperar la notificación**: el backend llama al mock BQM, que envía un push real con `transactionId`/`type`/`documentNumber`. Al recibirlo, la app completa sola los campos de "Resolver verificación pendiente" y dispara automáticamente `associate()`/`createValidationSession()` seguido de `completeTransaction()`. El estado de cada paso se muestra en la lista debajo del botón "Resolver".
4. El navegador (todavía en la pantalla de espera) debería reflejar el login como autorizado en el siguiente polling.

Firebase (`SDK_MOCK_BQM_FCM_TEST_DEVICE_TOKEN` + `service-account-mock-bqm.json`) es un requisito del mock BQM, no un fallback opcional: si falta cualquiera de los dos, el push no llega y la transacción se queda esperando (ver [`.docs/sdk/mock-bqm-push-auth.md`](../../.docs/sdk/mock-bqm-push-auth.md)). Para probar sin depender del push, se puede seguir completando "Resolver verificación pendiente" a mano, copiando `transactionId` desde la URL `.../transaction-status/<transactionId>` que el navegador muestra tras el login.

## Cómo probar el fallback QR cross-device

Ver [`08-qr-cross-device.md`](../../.docs/sdk/cliente/08-qr-cross-device.md) para el flujo completo. El SPA ofrece el QR cuando la push (de asociación **o** de validación) no se pudo confirmar entregada (`sdk_push_failed=true`, ver [`sdk/tasks.py`](../../id-2.0-backend/backend/sdk/tasks.py)), así que para forzarlo en dev sin depender de una falla real de FCM:

1. En Django Admin → SDK → Clients, apuntar temporalmente `push_endpoint_url` del `sdk.Client` de prueba a una URL que devuelva `404` (o dejarlo vacío/inválido para que se agoten los reintentos) — cualquiera de los dos casos deja la transacción `IN_PROGRESS` con `sdk_push_failed=true` en vez de fallarla.
2. **Iniciar sesión con Keycloak** desde un navegador (puede ser en la laptop, para probar el caso cross-device real). El backend crea la transacción pendiente y, al no poder confirmar la push, la pantalla de espera muestra el QR en el siguiente polling.
3. En el teléfono (dispositivo físico, requiere cámara), abrir esta app de ejemplo. La sección **"Fallback QR cross-device"** (debajo de "Resolver verificación pendiente", pero independiente de ella — no usa `transactionId` ni depende de que haya llegado una push) enruta sola según si el dispositivo ya tiene una asociación local:
   - **Sin asociación local:** muestra campos de documento (`documentType`/`documentCountry` precargados con `ci`/`UY`; completá `documentNumber` con el documento real del citizen que va a escanear) y el botón **"Escanear QR (asociación)"**. Alternativamente, "Asociar vía QR" en "Herramientas / debug" hace lo mismo.
   - **Con asociación local:** muestra un picker Pin/Liveness y el botón **"Escanear QR (validación)"**. Alternativamente, "Validar Pin/Liveness vía QR" en "Herramientas / debug" hace lo mismo.
4. Tocar el botón correspondiente y apuntar la cámara al QR mostrado en el navegador. La SDK decodifica el token, corre Liveness/PIN, y cierra la transacción internamente — no hace falta llamar `completeTransaction()` por separado.
5. El navegador (todavía en la pantalla de espera) debería reflejar el login como autorizado en el siguiente polling; el `finishUrl` que recibe la app es solo informativo y nunca se abre ahí, porque este camino es siempre cross-device.

Para probar específicamente el camino de **validación** (paso 3, con asociación local): usar un teléfono que ya completó el flujo de asociación anteriormente (con o sin QR) antes de repetir los pasos 1-2 con una nueva transacción — el login del paso 2 puede ser el mismo citizen o cualquier otro, lo único que determina el camino es si el teléfono tiene asociación local, no de quién es la transacción pendiente detrás del QR.

**Por qué pide un documento solo en el camino de asociación:** el QR solo resuelve *cómo llega el `transactionId`* (por cámara en vez de por push); quién es el ciudadano para el desafío de Liveness/PIN es un problema aparte, y `Document` no tiene default para `type`/`country` en ningún método de la SDK (ver [`04-invocacion-sdk.md`](../../.docs/sdk/cliente/04-invocacion-sdk.md)). En un Integrador real ese documento ya está en el perfil logueado del citizen — la app de ejemplo lo pide a mano solo porque no tiene ningún sistema de usuarios propio. En el camino de validación no hace falta: la asociación local ya identifica al citizen.

## Sección "Herramientas / debug"

Debajo del flujo guiado quedan los métodos de la SDK expuestos de forma aislada (`canAssociate`, `associate`, `associateViaQrScan`, `isAssociated`, `removeAssociation`, `createValidationSession`, y un `completeTransaction` manual), útiles para probar cada uno por separado sin pasar por Keycloak, más el botón para copiar el token FCM del dispositivo.

## Fuera de alcance de esta app

- Resolución real de "a qué dispositivo notificar" por `documentNumber`: el mock BQM usa un único token de prueba fijo (`SDK_MOCK_BQM_FCM_TEST_DEVICE_TOKEN`), no una base de dispositivos por usuario.
- La app no hace exchange de tokens con Keycloak (no llama a `/protocol/openid-connect/token`): solo dispara el login y muestra el `code`/`error` final, a modo de confirmación de que el tramo web terminó.
- Manejo de un client en `MOBILE_CLIENTS` (o mecanismo equivalente) de un Keycloak de terceros: no es el camino estándar del SDK (ver [`02-configuracion-keycloak.md`](../../.docs/sdk/cliente/02-configuracion-keycloak.md)) y no se prueba activamente con el `KEYCLOAK_CLIENT_ID` configurado hoy en esta app. El código de referencia para ese caso (`KeycloakAuth.parseIdDigitalCallback`/`buildIdDigitalResumeUri`, intent-filter sin `host` en `AndroidManifest.xml`) queda en el repo, pero no aplica salvo que el Keycloak del Integrador fuerce ese patrón.
