# Camping Gear Rental Management

Java desktop prototype for managing camping equipment rentals and demonstrating design patterns.

## Tech stack

- Java 21 (compiled with JDK 25)
- Maven
- JavaFX
- MySQL and JDBC
- JUnit 5

## Prerequisites

- JDK 21 or newer
- Maven 3.9+
- MySQL 8+

## Build and run

```powershell
mvn clean test
mvn clean package
mvn javafx:run
```

## Database setup

Create a MySQL database, then run `database/schema.sql` followed by `database/seed.sql` against it. The seed data uses the demo account `admin` / `admin123` specified in the project plan; replace it before any non-demo use.

## Project structure

```text
src/main/java/com/campinggearrental/  JavaFX bootstrap application
src/main/resources/view/              Future FXML views
src/main/resources/css/               Future stylesheets
src/test/java/                        Tests
database/                             Shared schema and demo seed data
docs/                                 Project plan and diagrams
```

The intended flow is JavaFX View → Controller → Service → Repository/DAO → MySQL.

## Team responsibilities and patterns

| Member | Module | Pattern |
| --- | --- | --- |
| Nguyễn Minh Long (A) | Account, Customer, Database | Singleton |
| Hoàng Bình Quân (B) | Equipment, Inventory, Camping Package | Factory |
| Nguyễn Hồng Phúc Thọ (C) | Rental, Rent/Return | State |
| Lương Hữu Thiện (D) | Pricing, Checkout | Strategy |

Observer and Decorator are optional and intentionally not bootstrapped.
