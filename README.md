# Tatkal Seat Booking System

A REST API for booking Tatkal train seats where **one seat can never be sold to two people**, even when many users click "Book" at the same moment.

Built with Java 17, Spring Boot 3.5, Spring Data JPA, PostgreSQL and Spring Security (JWT + BCrypt).

## The problem

Tatkal booking opens at a fixed time and many people try to grab the same seats in the same second. A naive "check if the seat is free, then insert a booking" has a race condition: two requests can both see the seat as free and both insert a booking. That is a double booking.

## How locking prevents double booking

One booking is one seat on one journey, with its own 10-digit PNR. Everything happens inside one `@Transactional` method, `BookingService.book()`:

1. **Lock the seat row.** `SeatRepository.findAndLockSeat()` uses `@Lock(PESSIMISTIC_WRITE)`, which runs `SELECT ... FOR UPDATE` on that one seat. The first request gets the lock; any other request for the same seat **waits** until the first one commits or rolls back.
2. **Check while holding the lock.** Only after getting the lock do we check `existsByJourneyIdAndSeatIdAndStatus(journeyId, seatId, CONFIRMED)`. A request that had to wait runs this check after the first one committed, so it sees the booking and gets `409 Conflict`.
3. **Save and release.** If the seat is free, the booking is saved. The lock is released automatically when the transaction ends.
4. **No waiting forever.** The lock has a 5-second timeout. If it can't be taken in time, the request gets `409 Conflict` and can try again.

Cancelling sets the booking's status to `CANCELLED`, so the seat no longer has a confirmed booking and can be booked again.

## Endpoints

| Method | URL | Access | What it does |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create an account, returns a JWT |
| POST | `/api/auth/login` | public | Log in, returns a JWT |
| GET | `/api/trains/search?source=Pune&destination=Mumbai&date=YYYY-MM-DD` | public | Trains on a route and date, with free seat count |
| GET | `/api/journeys/{id}/seats` | public | Seat map for a journey (free / booked) |
| POST | `/api/bookings` | logged in | Book one seat: `{"journeyId": 1, "seatId": 5}` |
| GET | `/api/bookings/my` | logged in | My bookings, newest first |
| GET | `/api/bookings/{pnr}` | owner only | Booking details |
| POST | `/api/bookings/{pnr}/cancel` | owner only | Cancel a booking |

Send the token as `Authorization: Bearer <token>`. Errors come back as JSON with status 400 / 401 / 404 / 409. A PNR that belongs to someone else returns 404, so other users can't tell it exists. Swagger UI: `http://localhost:8080/swagger-ui.html`.

## How to run

Needs Java 17 and PostgreSQL with a database called `tatkal`.

```bash
export DB_URL=jdbc:postgresql://localhost:5432/tatkal
export DB_USERNAME=postgres
export DB_PASSWORD=your-password
export JWT_SECRET=some-long-random-secret-at-least-32-characters
./mvnw spring-boot:run
```

On first start, `DataSeeder` adds two sample Pune → Mumbai trains (20 seats each) with journeys for the next 3 days. Timings and fares are sample data, not live IRCTC data. Set `SEED_DATA=false` to skip it.

Try it: register, search for a journey, open its seat map, then book a seat with the token.

## Project structure

```
controller/   REST endpoints (AuthController, TrainController, BookingController)
service/      business logic (AuthService, TrainService, BookingService)
repository/   Spring Data JPA repositories (the seat locking query lives in SeatRepository)
entity/       User, Train, Seat, Journey, Booking, BookingStatus
dto/          request/response records
security/     JwtUtil, JwtFilter
config/       SecurityConfig, DataSeeder
exception/    custom exceptions + GlobalExceptionHandler
```
