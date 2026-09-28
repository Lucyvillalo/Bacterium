# Laboratorio Bacterium — Sistema de gestión

Sistema web desarrollado en Java 17 + Spring Boot 3 con HTML/CSS/JS puro.  
Base de datos en AWS RDS (PostgreSQL).

---

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Java 21 + Spring Boot 3.2 |
| Frontend | HTML + CSS + JavaScript (sin frameworks) |
| Seguridad | Spring Security + JWT |
| Base de datos | PostgreSQL (AWS RDS) |
| PDF | iText 8 |
| Build | Maven 3.9+ |

---

## Estructura del proyecto

```
bacterium/
├── pom.xml
└── src/main/
    ├── java/com/bacterium/
    │   ├── BacteriumApplication.java       ← Entrada principal
    │   ├── model/
    │   │   ├── Usuario.java                ← Roles: ADMIN, LABORATORISTA, RECEPCION, PACIENTE
    │   │   ├── Paciente.java               ← Datos del paciente + código de acceso
    │   │   ├── Orden.java                  ← Orden de laboratorio
    │   │   └── Entidades.java              ← Resultado, Prueba, Médico
    │   ├── repository/
    │   │   └── Repositorios.java           ← JPA repositories
    │   ├── security/
    │   │   └── Seguridad.java              ← JWT filter + Spring Security config
    │   ├── service/
    │   │   └── PdfResultadoService.java    ← Generación de PDF con iText
    │   └── controller/
    │       ├── AuthYPacienteController.java
    │       └── OrdenController.java
    └── resources/
        ├── application.properties          ← Config producción (AWS RDS)
        ├── application-dev.properties      ← Config desarrollo (H2)
        └── static/
            ├── index.html                  ← Panel interno (admin/lab/recepción)
            ├── portal.html                 ← Portal público para pacientes
            ├── css/app.css
            └── js/app.js
```

---

## Requisitos previos

- Java 17 o superior (JDK)
- Maven 3.9+
- Cuenta AWS (para RDS en producción)

---

## Configuración local (desarrollo)

### 1. Clonar / abrir el proyecto en tu IDE (IntelliJ recomendado)

### 2. Variables de entorno para producción

Crea un archivo `.env` o configura en tu sistema:

```bash
DB_USERNAME=bacterium_user
DB_PASSWORD=tu_password_seguro
JWT_SECRET=tu_clave_jwt_de_al_menos_64_caracteres_generada_con_openssl
```

Para generar el JWT_SECRET:
```bash
openssl rand -base64 64
```

### 3. Ejecutar en modo desarrollo (H2 en memoria)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

El sistema queda disponible en: http://localhost:8080

- Panel interno: http://localhost:8080/index.html
- Portal pacientes: http://localhost:8080/portal.html
- Consola H2: http://localhost:8080/h2-console

Credenciales locales de prueba:

```text
Usuario: admin
Password: admin123
```

---

## Configuración AWS RDS

### Crear la instancia RDS

1. Ir a AWS Console → RDS → Create database
2. Elegir PostgreSQL 15
3. Tipo: db.t3.micro (costo ~$15/mes)
4. Habilitar Multi-AZ: No (en producción sí)
5. Nombre BD: `bacterium_db`
6. Usuario maestro: `bacterium_admin`
7. Habilitar backups automáticos (7 días de retención)
8. Anotar el endpoint que te da AWS

### Crear el esquema inicial

Conectarte a RDS con psql o DBeaver y ejecutar:

```sql
CREATE DATABASE bacterium_db;
CREATE USER bacterium_user WITH PASSWORD 'tu_password';
GRANT ALL PRIVILEGES ON DATABASE bacterium_db TO bacterium_user;
```

### Actualizar application.properties

```properties
spring.datasource.url=jdbc:postgresql://TU-ENDPOINT.rds.amazonaws.com:5432/bacterium_db
spring.jpa.hibernate.ddl-auto=update  # Primera vez: update; luego: validate
```

---

## Despliegue en AWS EC2

### Opción A: EC2 + JAR (más control)

```bash
# En la instancia EC2 (Ubuntu 24.04)
sudo apt update
sudo apt install -y openjdk-21-jre-headless

# Copiar el JAR
scp target/laboratorio-bacterium-1.0.0.jar ec2-user@TU-IP:/home/ec2-user/

# Ejecutar
java -jar laboratorio-bacterium-1.0.0.jar \
     --spring.profiles.active=prod \
     --DB_USERNAME=bacterium_user \
     --DB_PASSWORD=tu_password \
     --JWT_SECRET=tu_jwt_secret
```

### Opción B: Elastic Beanstalk (más fácil, AWS lo gestiona)

```bash
mvn package
# Subir el .jar en Elastic Beanstalk → Create Application → Java platform
```

---

## Funcionalidades implementadas

### Panel interno (index.html)
- Login con JWT (token guardado en localStorage)
- Navegación por secciones: Inicio, Pacientes, Órdenes, Resultados, Usuarios
- Dashboard con estadísticas y últimas órdenes
- CRUD de pacientes con búsqueda en tiempo real
- Descarga de PDF de resultados
- Control de acceso por rol

### Portal de pacientes (portal.html)
- Sin login: el paciente ingresa su código + número de orden
- Muestra si los resultados están disponibles
- Descarga directa del PDF

### API REST
- `POST /api/auth/login` → devuelve JWT
- `GET/POST/PUT /api/pacientes` → gestión de pacientes
- `GET /api/ordenes/{id}/pdf` → descarga PDF (requiere auth)
- `GET /api/ordenes/portal/{codigo}/{orden}/pdf` → PDF público

---

## Próximos pasos recomendados

1. **Módulo de resultados**: formulario para que el laboratorista ingrese valores prueba por prueba
2. **Catálogo de pruebas**: CRUD para administrar las pruebas disponibles por área
3. **Gestión de usuarios**: panel de admin para crear cuentas
4. **Notificaciones**: envío de email/SMS cuando los resultados están listos
5. **SSL/HTTPS**: configurar en AWS con ACM + Load Balancer

---

## Seguridad implementada

- Contraseñas con BCrypt (factor 12)
- JWT con expiración de 24h
- Roles y autorización por endpoint
- CSRF deshabilitado (API REST stateless)
- Variables de entorno para credenciales (nunca en código)

---

## Costo estimado AWS (mensual)

| Servicio | Costo |
|---------|-------|
| EC2 t3.small | ~$15 |
| RDS db.t3.micro | ~$15 |
| Route 53 + ACM | ~$5 |
| S3 / transferencia | ~$5 |
| **Total** | **~$40/mes** |
