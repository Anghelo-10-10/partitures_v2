Partituras Musicales - Aplicación Spring Boot

Aplicación para gestión de partituras musicales desarrollada con Spring Boot (Kotlin), Spring Data JPA y PostgreSQL.

Requisitos previos

- Java 21 (JDK 21)
- PostgreSQL 15+
- Gradle 8+
- Git (opcional)

Configuración inicial

1. **Clonar el repositorio**:
   ```bash
   git clone [URL_DEL_REPOSITORIO]
   cd partitures

2. Ejecutar El Proyecto.
   EN la terminal ejecuta: ./gradlew bootRun

   La aplicación se ejecuta en http://localhost:8080/.

   La App todavía no tiene interfaz gráfica, pero se puede probar conectando a la base de datos los métodos y peticiones.

   
4. Conexión a Base de Datos PGADMIN.

   Se debe agregar una nueva conexión o un nuevo server.
   
   En el nombre se puede colocar cualquier nombre.
   
   En Connection:
   
   HOST: localhost.
   
   PUERTO: Puerto: 5433 (Yo le puse ese) Tú coloca algun puerto que te funcione en caso de que te diga que ese puerto ya esta ocupado.
   
   USUARIO: admin
   
   PASSWORD: admin
   
   DB: partitures_db
   
4: Configuración técnica clave:

  Spring Boot 3.5.0

  Kotlin 1.9+

  PostgreSQL 15

  Hibernate 6.6.15
   
   
