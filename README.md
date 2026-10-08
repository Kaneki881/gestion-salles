# Gestion Salles

A Java application for managing rooms (salles) and users (utilisateurs), built with JPA/Hibernate on an in-memory H2 database. It shows a layered design (model + service), generic CRUD operations, custom queries, and Bean Validation.

## Technologies

- Java 22 (set in `pom.xml`)
- Maven
- Hibernate Core 5.6.5.Final (JPA 2.2 implementation)
- Hibernate Validator 6.2.0.Final (Bean Validation)
- H2 Database 2.1.214 (in-memory)
- SLF4J Simple (logging)
- JUnit 4.13.2 (tests)

## Project structure

```
gestion-salles
├── pom.xml
└── src
    ├── main
    │   ├── java/com/example
    │   │   ├── App.java                      # Demo of CRUD operations
    │   │   ├── model
    │   │   │   ├── Salle.java                # Room entity
    │   │   │   └── Utilisateur.java          # User entity
    │   │   └── service
    │   │       ├── CrudService.java          # Generic CRUD interface
    │   │       ├── SalleService.java         # Room queries
    │   │       └── UtilisateurService.java   # User queries
    │   └── resources/META-INF
    │       └── persistence.xml               # Persistence unit "gestion-salles"
    └── test/java/com/example/service
        ├── SalleServiceTest.java
        └── UtilisateurServiceTest.java
```

## Data model

### Salle (table `salles`)

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | Generated primary key |
| `nom` | `String` | Required, 2 to 100 characters |
| `capacite` | `Integer` | Required, between 1 and 1000 |
| `description` | `String` | Optional, up to 500 characters |
| `disponible` | `Boolean` | Required, defaults to `true` |
| `etage` | `Integer` | Optional, 0 or more |

### Utilisateur (table `utilisateurs`)

| Field | Type | Constraints |
|---|---|---|
| `id` | `Long` | Generated primary key |
| `nom` | `String` | Required, 2 to 50 characters |
| `prenom` | `String` | Required, 2 to 50 characters |
| `email` | `String` | Required, valid email format, unique |
| `dateNaissance` | `LocalDate` | Optional, must be in the past |
| `telephone` | `String` | Optional, 10 to 15 digits with an optional leading `+` |

Validation messages are written in French in the entity annotations.

## Services

`CrudService<T, ID>` defines the operations shared by every entity: `save`, `findById`, `findAll`, `update`, `delete` and `deleteById`. `SalleService` and `UtilisateurService` build on it (through `AbstractCrudService`) and add their own queries:

- `SalleService`
  - `findByDisponible(boolean)`: rooms by availability
  - `findByCapaciteMinimum(int)`: rooms with at least the given capacity
- `UtilisateurService`
  - `findByEmail(String)`: a user by email, returned as an `Optional`

## What the demo does

`App.main` creates the `EntityManagerFactory` for the `gestion-salles` persistence unit, then runs two scenarios:

1. **Users:** creates two users, lists them, finds one by ID and one by email, updates a phone number, deletes one user, and lists the rest.
2. **Rooms:** creates three rooms (one unavailable), lists them, finds one by ID, filters by availability and by minimum capacity, updates a capacity, deletes one room, and lists the rest.

## Configuration

The persistence unit is defined in `src/main/resources/META-INF/persistence.xml`:

- H2 in-memory database `jdbc:h2:mem:testdb`, user `sa`, empty password
- `hibernate.hbm2ddl.auto=create-drop`: tables are created at startup and dropped at shutdown
- `hibernate.show_sql=true` and `hibernate.format_sql=true`: generated SQL is printed in the console

Because the database lives in memory, all data is lost when the program ends.

## Build and run

You need a JDK 22 and Maven.

```bash
# Run the demo
mvn compile exec:java -Dexec.mainClass=com.example.App

# Run the tests
mvn test
```

## Tests
<img width="1549" height="902" alt="Screenshot 2026-10-08 011410" src="https://github.com/user-attachments/assets/00fbd14f-350a-4169-a703-b9c9b68f8248" />

Both test classes use JUnit 4 and a fresh `EntityManagerFactory` for each test.

- `SalleServiceTest`: full CRUD cycle, `findByDisponible`, `findByCapaciteMinimum`
- `UtilisateurServiceTest`: full CRUD cycle, `findByEmail` (including an unknown email), `findAll`

## Possible next steps

- Add a `Reservation` entity linking a user to a room for a time slot
- Check that a room is free before booking it
- Add a command-line or web interface on top of the services
