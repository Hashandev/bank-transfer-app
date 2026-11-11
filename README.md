# Bank Transfer System (Assignment 01)

COSC 32133 / BECS 32263 – Full-Stack Software Development

Spring Boot + Thymeleaf + Spring Data JPA + MySQL + Lombok.

Demonstrates:
* `@Transactional` - a money transfer between two customers either fully succeeds or is fully rolled back
* `@Query` - JPQL, native SQL and UPDATE queries written in the repository interfaces
* `REQUIRES_NEW` - failed transfers are still recorded, even though the transfer itself is rolled back

## Run
1. Start MySQL (XAMPP -> MySQL -> Start). The database `bank_db` is created automatically.
2. Open the project in VS Code and run `BankTransferApplication.java`
   (or, with Maven installed: `mvn spring-boot:run`)
3. Open http://localhost:8080 (3 sample customers are created on first start)

If your MySQL root user has a password, copy `secrets.properties.example` to `secrets.properties` and edit it.

## URLs
| Method | URL | Purpose |
|---|---|---|
| GET | `/` | Dashboard (`?keyword=` searches customers by name) |
| POST | `/customers` | Add a customer |
| POST | `/transfer` | Transfer money |
| GET | `/accounts/{accountNumber}/history` | Transactions of one account |
| GET | `/large-transfers?min=10000` | Successful transfers above an amount |
| GET / POST | `/deposit`, `/withdraw` | Deposit and withdraw money |