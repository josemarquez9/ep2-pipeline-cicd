# Justificaciones técnicas, conclusiones y reflexiones

## 1. Justificaciones técnicas del equipo

Para cada decisión: qué elegimos, qué alternativas existían, por qué y qué limitación aceptamos.

### 1.1 Dos microservicios en lenguajes distintos (Java/Spring Boot y Python/Flask)

- **Alternativas:** dos servicios en el mismo lenguaje, o un monolito.
- **Justificación:** la evaluación pide dos microservicios distintos entre sí. Usar Java y Python permite demostrar que el pipeline
  es independiente del lenguaje: cada uno tiene su propia herramienta de pruebas y cobertura (JUnit + JaCoCo y PyTest + pytest-cov),
  su gestor de dependencias (Maven y pip) y su Dockerfile. Spring Boot es un estándar para APIs con persistencia y validación;
  Flask es liviano y suficiente para un servicio de reportes que solo consulta a otro.
- **Limitación:** dos ecosistemas implican más herramientas que mantener.

### 1.2 Imágenes multi-etapa con usuario no root

- **Alternativas:** una sola etapa con JDK/Maven o pip dentro de la imagen final; ejecutar como root (valor por defecto).
- **Justificación:** con dos etapas se separan compilación y ejecución: la imagen final contiene solo el JRE (o el entorno virtual de
  Python) y la aplicación, sin compiladores ni herramientas de construcción. Eso reduce el tamaño y la superficie de ataque.
  El usuario sin privilegios (UID 10001) limita el daño si el proceso es comprometido, y el pipeline lo verifica en cada ejecución (`id -u`).
- **Limitación:** Dockerfiles más largos y dependencia de BuildKit para la caché de Maven.

### 1.3 GitHub Actions como motor de CI/CD (workflow principal + despliegue reutilizable)

- **Alternativas:** Jenkins, GitLab CI.
- **Justificación:** el código ya está en GitHub, por lo que no hay un servidor que instalar ni mantener; además integra de forma
  nativa el registro de imágenes (GHCR), los entornos con aprobación (Environments), Dependabot y los secretos. Separar el despliegue
  en un workflow reutilizable evita duplicar los pasos entre staging y producción: solo cambia el parámetro `environment`.
- **Limitación:** dependencia de un proveedor y de los minutos de ejecución disponibles.

### 1.4 Snyk junto con Dependabot (en lugar de SonarQube)

- **Alternativas:** SonarQube/SonarCloud; usar solo Dependabot.
- **Justificación:** la pauta admite SonarQube o Snyk. Snyk cubre con un solo token las dependencias, el código fuente y la imagen,
  y no exige instalar ni hospedar un servidor, a diferencia de SonarQube. Dependabot es complementario: actúa de forma continua fuera
  del pipeline (alertas y PR semanales), mientras que Snyk es la compuerta que frena el pipeline.
- **Limitación:** depende de una cuenta externa y de los límites del plan gratuito; SonarQube ofrece métricas de calidad de código
  (duplicación, deuda técnica) que Snyk no entrega.

### 1.5 Umbral de cobertura del 80 %

- **Alternativas:** 60 % o 100 %.
- **Justificación:** 80 % es un umbral habitual en la industria: obliga a cubrir los caminos relevantes (CRUD, validaciones y errores
  404, 409 y 503) sin forzar pruebas triviales solo para llegar a 100 %. Es parametrizable (`COVERAGE_MIN`) y se aplica igual a ambos servicios.
- **Limitación:** la cobertura mide líneas ejecutadas, no la calidad de las aserciones.

### 1.6 Umbral de severidad `high` para bloquear

- **Alternativas:** `low`, `medium` o `critical`.
- **Justificación:** bloquear desde `low` o `medium` genera ruido y detiene el pipeline por hallazgos poco explotables; bloquear solo
  `critical` deja pasar riesgos serios. `high` equilibra ambos extremos. Además, el escaneo de imágenes excluye las vulnerabilidades de
  la imagen base (`--exclude-base-image-vulns`), que el equipo no controla y que Dependabot actualiza por su cuenta.
- **Limitación:** las vulnerabilidades medias no bloquean; se siguen mediante Dependabot y el panel de Snyk.

### 1.7 Docker Compose con nginx como gateway y balanceador

- **Alternativas:** Kubernetes, Docker Swarm, Traefik.
- **Justificación:** la pauta pide orquestación con Docker Compose y, para cuatro servicios, es una herramienta proporcionada.
  nginx concentra la entrada (un solo puerto publicado), reparte la carga entre réplicas mediante el DNS round-robin de Docker,
  aplica cabeceras de seguridad y límite de peticiones, y oculta `/actuator`. Las réplicas, la CPU y la memoria se fijan por entorno (`deploy/env`).
- **Limitación:** un solo host; no hay actualizaciones progresivas ni autoescalado, que sí ofrecería Kubernetes.

### 1.8 Entorno simulado en el runner y GHCR como registro

- **Alternativas:** una máquina virtual en una nube real (AWS, Azure); Docker Hub.
- **Justificación:** la pauta permite un entorno simulado. Usar el runner evita costos y credenciales de nube, y levanta el sistema
  desde cero en cada despliegue, lo que prueba que es reproducible. GHCR se autentica con el `GITHUB_TOKEN` y queda enlazado al repositorio.
- **Limitación:** el entorno es efímero (se destruye al terminar el job), por lo que no hay un servicio persistente que consultar después;
  la evidencia queda en los artefactos y en el manifiesto de despliegue.

### 1.9 Etiquetado por SHA y despliegue con `--no-build`

- **Alternativas:** usar `latest` y reconstruir en el servidor.
- **Justificación:** etiquetar con el SHA del commit hace que cada imagen sea inmutable y trazable, y `--no-build` garantiza que lo
  desplegado es exactamente la imagen ya probada y escaneada. Además permite volver a una versión anterior redesplegando su SHA.
- **Limitación:** se acumulan muchos tags; en un proyecto real habría que definir una política de limpieza.

### 1.10 Aprobación manual para producción

- **Alternativas:** despliegue totalmente automático.
- **Justificación:** una persona revisa el resultado de staging antes de que el cambio llegue a producción, y GitHub Environments deja
  registro de quién aprobó y cuándo, lo que aporta gobernanza.
- **Limitación:** agrega una espera humana; en repositorios privados, las reglas de protección de entornos requieren un plan de pago.

## 2. Conclusiones del proyecto (equipo)

**Cumplimiento por indicador (la evidencia está en el repositorio):**

- **IE1 · Contenedores:** se contenerizaron dos microservicios distintos con imágenes multi-etapa mínimas, usuario no root y
  healthcheck, construidas y publicadas por el pipeline.
- **IE2 · Pruebas automatizadas:** ambos servicios ejecutan pruebas unitarias y de integración en cada cambio, con una cobertura
  mínima del 80 % que actúa como compuerta; los informes quedan como artefactos de cada ejecución.
- **IE3 · Escalabilidad y seguridad:** el pipeline analiza dependencias, código e imagen con Snyk, bloquea desde la severidad `high`,
  genera una alerta automática y se complementa con Dependabot; las réplicas, los recursos y el endurecimiento de los contenedores se
  parametrizan por entorno.
- **IE4 · Despliegue y trazabilidad:** cada commit validado se despliega automáticamente en staging y luego en producción desde su
  imagen etiquetada con el SHA, con pruebas de humo y un manifiesto que permite seguir el camino commit → imagen → despliegue →
  contenedor en ejecución.
- **IE5 · Orquestación:** Docker Compose levanta cuatro servicios con orden de arranque por healthchecks, redes segmentadas,
  volumen de datos, réplicas y límites de recursos.

**Lecciones técnicas:**

1. Automatizar es más que ejecutar pruebas: las compuertas (cobertura, seguridad, política no root) hacen que el pipeline proteja la
   calidad en lugar de solo informarla.
2. La trazabilidad se logra encadenando identificadores (SHA → tag → digest → `/info`), no con un único mecanismo.
3. La seguridad es de capas (código, imagen, ejecución y pipeline) y ninguna capa basta por sí sola.
4. Un entorno simulado permite practicar el despliegue sin costo, aunque no reemplaza a uno persistente.

**Limitaciones y mejoras posibles:** migraciones de base de datos con Flyway o Liquibase en lugar de `ddl-auto=update`; despliegue en
una nube real con actualizaciones progresivas (o Kubernetes); gestión de secretos con Docker secrets o un gestor dedicado; pruebas de
extremo a extremo y de carga; y un análisis de calidad con SonarQube.

**Dificultades reales al ejecutar el pipeline:** **[AJUSTA: describan 1 o 2 problemas concretos que tuvieron (por ejemplo, con el
token de Snyk, con los permisos o con una prueba) y cómo los resolvieron; si no tuvieron ninguno, escriban qué fue lo más difícil de entender]**.

## 3. Reflexiones individuales

### Integrante 1 · [Nombre y apellido]

Con este proyecto entendí que un pipeline de CI/CD es una cadena de compuertas y no solo una automatización: cada etapa (pruebas,
cobertura mínima, escaneo de seguridad, política de contenedores sin root, pruebas de humo) decide si un cambio puede avanzar. También
comprendí por qué una imagen debe construirse una sola vez, escanearse y desplegarse tal cual, identificada por el SHA del commit:
así lo que llega a producción es exactamente lo que se probó.

Mi contribución fue **[AJUSTA SI TU ROL FUE OTRO: crear el repositorio y las ramas `main` y `develop`, abrir las cuentas de GitHub y
Snyk, configurar los secretos y Dependabot, y ejecutar y revisar el primer pipeline]**. Lo que más me costó fue **[AJUSTA: entender cómo
se conectan Snyk y los secretos con el workflow, y leer los logs de Actions para encontrar la causa de un fallo]**; lo resolví
**[AJUSTA: leyendo el mensaje de error del paso que falló y corrigiendo la configuración]**. De esta experiencia me llevo el hábito de
revisar la seguridad como parte del flujo de trabajo y no al final.

### Integrante 2 · [Nombre y apellido]

Lo más valioso fue ver cómo un mismo flujo sirve para dos tecnologías distintas: Java y Python se prueban y se empaquetan con
herramientas diferentes (JUnit y JaCoCo, PyTest y pytest-cov), pero ambos terminan en una imagen Docker y pasan por las mismas
compuertas. También comprendí la orquestación con Docker Compose: el orden de arranque con healthchecks, las redes separadas que
dejan la base de datos fuera del alcance del exterior y el gateway que reparte las peticiones entre réplicas.

Mi contribución fue **[AJUSTA SI TU ROL FUE OTRO: probar el sistema en mi computador con Docker en Linux, ejecutar las pruebas de humo,
hacer la demostración del bloqueo con un cambio que hace fallar una prueba y reunir las capturas de evidencia]**. Lo más difícil fue
**[AJUSTA: por ejemplo, los permisos de Docker en Linux o interpretar un error del pipeline]**, y lo resolví **[AJUSTA: cómo]**.
Aprendí que provocar una falla a propósito es una buena forma de comprobar que los controles realmente funcionan.
