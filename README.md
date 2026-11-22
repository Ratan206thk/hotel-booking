# Hotel Booking System

Spring Boot (Java 17) backend for property discovery, onboarding, booking, payment and cancellation.
In-memory persistence behind repository interfaces; payment gateway mocked behind `PaymentProcessor`.

## Build, test, run

```bash
mvn clean test            # unit tests (no Spring context needed)
mvn spring-boot:run       # http://localhost:8080
```
Swagger UI: http://localhost:8080/swagger-ui.html

## Try it

```bash
# 1. Chain with two properties (a standalone property is just an owner with one property)
curl -s -X POST localhost:8080/api/owners -H 'Content-Type: application/json' -d '{
  "name":"Taj Group",
  "properties":[
   {"name":"Taj Mumbai","city":"Mumbai","locality":"Colaba","starRating":5,"amenities":["WIFI","POOL"],
    "roomTypes":[{"name":"Deluxe","capacity":2,"pricePerNight":8000,"totalRooms":3}]},
   {"name":"Taj Pune","city":"Pune","locality":"Camp","starRating":4,"amenities":["WIFI"],
    "roomTypes":[{"name":"Suite","capacity":4,"pricePerNight":6000,"totalRooms":2}]}]}'

# 1b. Single standalone property
curl -s -X POST localhost:8080/api/properties/standalone -H 'Content-Type: application/json' -d '{
  "name":"Budget Inn","city":"Mumbai","locality":"Andheri","starRating":2,"amenities":["WIFI"],
  "roomTypes":[{"name":"Standard","capacity":2,"pricePerNight":1500,"totalRooms":5}]}'

# 2. Search (required: city, checkIn, checkOut, guests; optional: locality, minPrice, maxPrice, amenities, minStars)
curl -s 'localhost:8080/api/properties/search?city=Mumbai&checkIn=2026-12-10&checkOut=2026-12-12&guests=2&maxPrice=9000&amenities=WIFI&minStars=2'

# 3. Book (use propertyId / roomTypeId from the responses above)
curl -s -X POST localhost:8080/api/bookings -H 'Content-Type: application/json' -d '{
  "guestId":"guest-1","propertyId":"<id>","roomTypeId":"<id>","checkIn":"2026-12-10","checkOut":"2026-12-12","guests":2}'

# 4. Pay  (CARD: 16 digits, 4000000000000002 is declined | UPI: name@bank, handle containing "fail" is declined | WALLET: any id, containing "fail" is declined)
curl -s -X POST localhost:8080/api/bookings/<bookingId>/payments -H 'Content-Type: application/json' -d '{
  "method":"CARD","instrument":"4111111111111111","idempotencyKey":"pay-001"}'

# 5. Cancel
curl -s -X POST localhost:8080/api/bookings/<bookingId>/cancel
```

## Design

| Package | Responsibility |
|---|---|
| `domain` | Entities & value objects (`Owner`, `Property`, `RoomType`, `Booking`, `Payment`, `DateRange`). No Spring, no persistence. Booking state machine lives in `BookingStatus` + guarded transitions on `Booking`. |
| `repository` / `repository.memory` | Repository interfaces + in-memory implementations. `InventoryRepository` has an atomic `tryReserve`. |
| `search` | `SearchService` + `PropertyFilterFactory` strategies. |
| `pricing` | `PricingStrategy` (default per-night). |
| `payment` | `PaymentProcessor` per method (Card/UPI/Wallet mocks), `PaymentProcessorRegistry`, `PaymentService`. |
| `cancellation` | `CancellationPolicy` (default `TieredCancellationPolicy`). |
| `service` | `BookingService` (lifecycle orchestration), `OwnerService`, `PropertyService`, command objects. |
| `api` | Controllers, DTOs, `GlobalExceptionHandler` (400 / 404 / 409). |

Key decisions
- **Single vs multi-property**: one model. `Owner` has N properties; a standalone property is an owner with 1.
- **Extensibility (open/closed)**: new filter = one `PropertyFilterFactory` bean; new payment method = enum value + one `PaymentProcessor` bean; new cancellation or pricing rule = new bean implementing the interface. No existing class changes.
- **Inventory model**: per room type, a count of rooms held per night (`[checkIn, checkOut)`, so back-to-back stays don't conflict). Booking availability = `totalRooms - max(held over nights)`.
- **Concurrency**: `tryReserve` is check-and-reserve under a per-(property, roomType) lock, so concurrent bookings can't oversell (proved by a 50-thread test). Pay/cancel on the same booking are serialised on the booking, and `Booking` transitions are synchronized. Cancelling twice fails the transition *before* any refund/release, so inventory is never released twice.
- **Lifecycle**: `PENDING_PAYMENT -> CONFIRMED -> CANCELLED`; `PENDING_PAYMENT -> PAYMENT_FAILED | CANCELLED`. Booking creation holds inventory immediately; payment failure or cancellation releases it.
- **Idempotent payments**: `idempotencyKey` is required. Replaying a key returns the original payment without re-charging; a key reused on a different booking is rejected.
- **Time** comes from an injected `Clock`, so cancellation tests are deterministic.

## Assumptions
- One booking = one room of one room type; `guests <= roomType.capacity`.
- Refund tiers (check-in taken as 00:00 on the check-in date): >= 48h: 100%, 24-48h: 50%, < 24h: 0%. Unpaid bookings cancel with no refund.
- Prices in a single currency, 2-decimal `BigDecimal`.
- Check-in in the past is rejected. City match is case-insensitive; locality/amenity filters are optional.
- No auth; any caller can act on any booking.

## With more time
- Hold expiry: auto-release `PENDING_PAYMENT` bookings after N minutes (scheduler).
- Real persistence (JPA) with a conditional `UPDATE ... WHERE booked < total` or row lock for inventory; optimistic locking on `Booking`.
- Compensation if a refund succeeds at the gateway but local persistence fails (outbox / retry).
- Date-aware dynamic pricing strategy (weekend/season/demand), multi-room bookings, pagination & sorting in search, MockMvc/integration tests, OpenAPI annotations.
