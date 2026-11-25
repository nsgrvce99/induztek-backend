# Induztek Terreno

Aplicación Android de registro de pruebas, equipos, instrumentos y actas de terreno. Proyecto DSY1105 de Duoc UC.

**Integrantes**  
Por completar por el equipo.

| Servicio | Puerto | Base de datos |
|---|---:|---|
| Eureka | 8761 | — |
| API Gateway | 8080 | — |
| Usuarios | 8081 | induztek_usuarios |
| Equipos e Instalaciones | 8082 | induztek_equipos |
| Instrumentos | 8083 | induztek_instrumentos |
| Protocolos | 8084 | induztek_protocolos |

Android → Gateway → servicios registrados en Eureka. Los servicios verifican la sesión vigente con Usuarios y usan Feign para validar referencias. Cada microservicio conserva sus propias tablas.

- [Arranque local y paquete para el instituto](Documentacion/GUIA_DE_EJECUCION.md)
- [Guía de defensa con capturas](Documentacion/GUIA_DE_DEFENSA.md)
- [Reglas y API del Gateway](Documentacion/ENDPOINTS.md)
- [Resultados de las pruebas](Documentacion/PRUEBAS.md)
- [Pruebas y limitaciones verificadas](Documentacion/ESTADO_PROYECTO.md)
- [Colección Postman](Documentacion/Induztek_Terreno.postman_collection.json)
- [Rúbrica](Documentacion/CUMPLIMIENTO_RUBRICA.md)

Android usa el Gateway por defecto para autenticación, catálogos, clientes, personal, protocolos y actas. Los datos históricos SQLite siguen disponibles mediante una compilación local explícita.

El APK firmado se genera con `gradlew.bat assembleAcademicoRelease`. Usa `http://10.0.2.2:8080/` en el emulador y acepta la IP privada del Gateway mediante `-Pinduztek.baseUrl` para una demostración en teléfono. La variante `produccion` exige HTTPS.
