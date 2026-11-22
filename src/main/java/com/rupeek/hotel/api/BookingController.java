package com.rupeek.hotel.api;

import com.rupeek.hotel.api.ApiDtos.BookingRequest;
import com.rupeek.hotel.api.ApiDtos.BookingResponse;
import com.rupeek.hotel.api.ApiDtos.PayRequest;
import com.rupeek.hotel.api.ApiDtos.PaymentOutcomeResponse;
import com.rupeek.hotel.domain.DateRange;
import com.rupeek.hotel.service.BookingCommand;
import com.rupeek.hotel.service.BookingService;
import com.rupeek.hotel.service.PaymentCommand;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse book(@Valid @RequestBody BookingRequest request) {
        return BookingResponse.from(bookingService.book(new BookingCommand(
                request.guestId(), request.propertyId(), request.roomTypeId(),
                new DateRange(request.checkIn(), request.checkOut()), request.guests())));
    }

    @GetMapping("/{bookingId}")
    public BookingResponse get(@PathVariable String bookingId) {
        return BookingResponse.from(bookingService.get(bookingId));
    }

    @PostMapping("/{bookingId}/payments")
    public PaymentOutcomeResponse pay(@PathVariable String bookingId, @Valid @RequestBody PayRequest request) {
        return PaymentOutcomeResponse.from(bookingService.pay(bookingId,
                new PaymentCommand(request.method(), request.instrument(), request.idempotencyKey())));
    }

    @PostMapping("/{bookingId}/cancel")
    public BookingResponse cancel(@PathVariable String bookingId) {
        return BookingResponse.from(bookingService.cancel(bookingId));
    }
}
