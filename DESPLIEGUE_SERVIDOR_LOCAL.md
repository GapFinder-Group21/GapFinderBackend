# Despliegue en servidor + Google Cloud (guía local, NO subir a git)

## A. Servidor (Plan A)

1. **Crear el servidor**: Ubuntu 22.04 o 24.04, mínimo 2 GB de RAM (con 1 GB Maven/Spring se quedan sin memoria). Anota la IP pública.
2. **Firewall del proveedor**: abre SOLO los puertos 22 (SSH), 80 y 443 (TCP). Nada de 5432, 5050 ni 8080. El 80 es necesario aunque uses HTTPS: Caddy lo usa para pedir el certificado.
3. **Dominio**: en https://www.duckdns.org crea un subdominio gratis (por ejemplo `gapfinder21.duckdns.org`) y ponle la IP pública. Comprueba desde tu PC: `nslookup gapfinder21.duckdns.org` debe dar esa IP. (Si tienen dominio propio: registro tipo A hacia la IP.)
4. **Entrar al servidor**: `ssh <usuario>@<IP>`.
5. **Instalar Docker**:
   ```bash
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER
   ```
   Cierra la sesión SSH y vuelve a entrar. Comprueba: `docker --version` y `docker compose version`.
6. **Clonar el backend** (la rama con estos cambios ya debe estar en main o clona esa rama con `-b <rama>`):
   ```bash
   git clone https://github.com/GapFinder-Group21/GapFinderBackend.git
   cd GapFinderBackend
   ```
7. **Crear el `.env`** (`cp .env.example .env` y `nano .env`):
   ```
   GOOGLE_CLIENT_ID=<el real>
   GOOGLE_CLIENT_SECRET=<el real>
   GOOGLE_REDIRECT_URI=https://gapfinder21.duckdns.org/google/callback
   JWT_SECRET=<salida de: openssl rand -base64 48>
   DOMAIN=gapfinder21.duckdns.org
   ```
   Sin espacios alrededor del `=` ni comillas. `chmod 600 .env`.
8. **Levantar**:
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
   docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f app
   ```
   Espera la línea `Started GapfinderApplication`. La primera vez el build tarda varios minutos. Ctrl+C sale de los logs sin parar nada. Si falta una variable del `.env`, compose se detiene con el mensaje "Set ... in .env".
9. **Comprobar**:
   - `docker compose -f docker-compose.yml -f docker-compose.prod.yml ps` → db, app y caddy "running"; NO debe aparecer pgadmin.
   - `curl -i https://gapfinder21.duckdns.org/` → cualquier respuesta (incluso 401/403) significa que llegó. Si hay error de certificado, mira `docker logs gapfinder-caddy` (casi siempre es DNS mal apuntado o puerto 80/443 cerrado).
   - Desde un celular con **datos móviles** (no nuestro Wi-Fi) abre la app apuntando a `https://gapfinder21.duckdns.org` e inicia sesión.
10. **Chequeo de seguridad antes de hacerlo público**:
    - `docker ps` no muestra pgadmin ni `0.0.0.0:5432` ni `0.0.0.0:8080`.
    - Desde tu PC: `nc -vz <IP> 5432` y `nc -vz <IP> 8080` deben fallar.
    - JWT_SECRET es nuevo (no el que está en application.properties).
    - `.env` no está en git (`git status` en el servidor no lo lista; está en .gitignore).
    - La contraseña de Postgres sigue siendo `gapfinder`, pero la BD no es alcanzable desde fuera; si quieren más seguridad, cámbienla en el compose.

Actualizar luego: `git pull` y repetir el `up -d --build`. Los datos persisten en volúmenes. **Nunca** `down -v` en producción (borra la BD).

## B. Google Cloud Console (lo hace quien administra el proyecto)

1. https://console.cloud.google.com → selecciona el proyecto de GapFinder.
2. Menú ☰ → **APIs y servicios** → **Credenciales**.
3. En "IDs de cliente OAuth 2.0" abre el cliente de tipo **Aplicación web** que usa el backend (el del GOOGLE_CLIENT_ID).
4. En **URIs de redireccionamiento autorizados** → **Agregar URI** → `https://gapfinder21.duckdns.org/google/callback` (exacto: https, sin barra final, mismo dominio que `GOOGLE_REDIRECT_URI`). Conserva la de localhost. **Guardar** (puede tardar unos minutos en aplicar).
5. **APIs y servicios → Pantalla de consentimiento de OAuth** (Google Auth platform → Público): con la app en modo **Testing**, en "Usuarios de prueba" → **Add users** y agrega las cuentas Google de la demo. Si no, sale "Acceso bloqueado".
6. **APIs y servicios → Biblioteca** → busca **Google Calendar API** → debe decir "API habilitada" (si no, Habilitar).

## C. Cómo verificar en el backend desplegado

a) `GET https://<dominio>/google/auth-url` con un token válido (`Authorization: Bearer ...`). La `url` debe traer `redirect_uri=https://<dominio>/google/callback` (la librería de Google puede devolverlo SIN codificar o como `https%3A%2F%2F...`; ambos valen) y **nada de localhost**. Si trae localhost → falta `GOOGLE_REDIRECT_URI` en el `.env` o no se reinició la app (`up -d` de nuevo).
b) Abre esa url en un celular con datos móviles: Google pide elegir cuenta.
   - **Error 400: redirect_uri_mismatch** → la URI registrada en Google no es idéntica (http vs https, barra final, dominio distinto) o aún no se propagó.
   - **Acceso bloqueado / access_denied (app en pruebas)** → la cuenta no está como usuario de prueba.
   - **Error 401: invalid_client** → GOOGLE_CLIENT_ID/SECRET mal copiados o siguen los dummy.
c) Al aceptar, el navegador muestra "Cuenta de Google conectada correctamente. Ya puedes cerrar esta ventana." y el log del backend dice "Credenciales de Google guardadas para el usuario N" (`docker compose ... logs app | grep Credenciales`). Si muestra "El enlace para conectar Google no es válido o expiró": el link dura 10 minutos y es de un solo uso, o la app se reinició entre pedir el link y aceptar (los states están en memoria); genera otro.
d) En la app, importa el horario desde Google Calendar y verifica que aparezcan las clases. Si el import falla pero el paso c salió bien, revisa que Calendar API esté habilitada.
