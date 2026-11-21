# Tatkal Seat Booking System

A REST API for booking Tatkal train seats where **one seat can never be sold to two people**, even when many users click "Book" at the same moment.

Built with Java 17, Spring Boot 3.5, Spring Data JPA, PostgreSQL, Spring Security (JWT + BCrypt), JUnit 5 and Mockito.

## The problem

Tatkal booking opens at a fixed time and thousands of people try to grab the same few seats in the same second. A naive "check if the seat is free, then insert a booking" has a race condition: two requests can both see the seat as free and both insert a booking. That is a double booking.

## How double booking is prevented

All of it happens inside one `@Transactional` method, `BookingService.book()`:

1. **Lock the seat rows.** `SeatRepository.findAndLockSeats()` uses `@Lock(PESSIMISTIC_WRITE)`, which runs `SELECT ... FOR UPDATE`. The first transaction gets the lock; any other transaction asking for the same seat **waits** until the first one commits or rolls back.
2. **Check while holding the lock.** Only after getting the lock do we check whether the seat already has a CONFIRMED booking for that journey. A request that had to wait runs this check after the first one committed, so it sees the booking and gets `409 Conflict`.
3. **All or nothing.** 1 to 4 seats are booked under one PNR. If any one seat is taken, an exception rolls back the whole transaction and nothing is saved.
4. **No deadlocks.** Seats are always locked in `ORDER BY id`, so two multi-seat bookings can't wait on each other in a circle. A lock timeout (5 s) stops a request from waiting forever.
5. **Database safety net.** The `bookings` table has `UNIQUE (journey_id, seat_id, active)`. Confirmed rows have `active = TRUE`; cancelled rows have `active = NULL` (NULLs never clash), so a cancelled seat can be booked again.

## Endpoints

| Method | URL | Access | What it does |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create a user account, returns a JWT |
| POST | `/api/auth/login` | public | Log in, returns a JWT |
| POST | `/api/admin/trains` | ADMIN | Add a train (creates its seats) |
| POST | `/api/admin/journeys` | ADMIN | Schedule a train on a date |
| GET | `/api/trains/search?source=Pune&destination=Mumbai&date=YYYY-MM-DD` | public | Trains on a route and date, with free seat count |
| GET | `/api/journeys/{id}/seats` | public | Seat map for a journey (free / booked) |
| POST | `/api/bookings` | logged in | Book 1–4 seats: `{"journeyId": 1, "seatIds": [1, 2]}` |
| GET | `/api/bookings/{pnr}` | owner only | Booking details |
| GET | `/api/bookings/my` | logged in | My bookings |
| POST | `/api/bookings/{pnr}/cancel` | owner only | Cancel all seats on a PNR |

Send the token as `Authorization: Bearer <token>`. Errors come back as JSON with status 400 / 401 / 404 / 409. Swagger UI: `http://localhost:8080/swagger-ui.html`.

## Run locally

Needs Java 17 and PostgreSQL with a database called `tatkal`.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/tatkal
export DB_USERNAME=postgres
export DB_PASSWORD=your-password
export JWT_SECRET=some-long-random-secret-at-least-32-characters
export ADMIN_EMAIL=admin@example.com
export ADMIN_PASSWORD=choose-a-password
./mvnw spring-boot:run
```

On first start, `DataSeeder` creates the admin user and two sample Pune → Mumbai trains (20 seats each, journeys for the next 3 days). Train timings and fares are sample data, not live IRCTC data. Set `SEED_DATA=false` to skip it.

With Docker:

```bash
docker build -t tatkal-booking .
docker run -p 8080:8080 -e DB_URL=jdbc:postgresql://host.docker.internal:5432/tatkal \
  -e DB_USERNAME=postgres -e DB_PASSWORD=your-password -e JWT_SECRET=... tatkal-booking
```

## Tests

```bash
./mvnw test
```

Tests run on in-memory H2 (PostgreSQL mode), so no database is needed.

- `BookingServiceTest` (Mockito): books 2 seats under one PNR, rejects an already-booked seat without saving anything, rejects more than 4 seats, and stops a user from cancelling someone else's booking.
- `AuthServiceTest` (Mockito): duplicate email is rejected; correct password gives a valid JWT, wrong password is rejected.
- `SeatBookingConcurrencyTest`: 30 threads, each a different user, are released at the same instant to book the **same seat**. The test checks that exactly 1 booking succeeds, the other 29 fail, and the database holds exactly 1 confirmed booking.

## Project structure

```
controller/   REST endpoints
service/      business logic (AuthService, TrainService, BookingService)
repository/   Spring Data JPA repositories (seat locking query lives here)
entity/       User, Train, Seat, Journey, Booking
dto/          request/response records
security/     JwtUtil, JwtFilter
config/       SecurityConfig, DataSeeder
exception/    custom exceptions + GlobalExceptionHandler
```
