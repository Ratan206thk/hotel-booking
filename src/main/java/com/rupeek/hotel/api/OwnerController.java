package com.rupeek.hotel.api;

import com.rupeek.hotel.api.ApiDtos.OwnerRequest;
import com.rupeek.hotel.api.ApiDtos.OwnerResponse;
import com.rupeek.hotel.api.ApiDtos.PropertyRequest;
import com.rupeek.hotel.api.ApiDtos.PropertyResponse;
import com.rupeek.hotel.domain.Owner;
import com.rupeek.hotel.service.OwnerService;
import com.rupeek.hotel.service.PropertyService;
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
@RequestMapping("/api")
public class OwnerController {

    private final OwnerService ownerService;
    private final PropertyService propertyService;

    public OwnerController(OwnerService ownerService, PropertyService propertyService) {
        this.ownerService = ownerService;
        this.propertyService = propertyService;
    }

    /** Registers an owner/chain, optionally with its initial properties. */
    @PostMapping("/owners")
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse registerOwner(@Valid @RequestBody OwnerRequest request) {
        Owner owner = ownerService.register(request.name());
        if (request.properties() != null) {
            request.properties().forEach(p -> propertyService.addProperty(owner.id(), p.toCommand()));
        }
        return OwnerResponse.from(ownerService.get(owner.id()));
    }

    @GetMapping("/owners/{ownerId}")
    public OwnerResponse getOwner(@PathVariable String ownerId) {
        return OwnerResponse.from(ownerService.get(ownerId));
    }

    /** Adds another property to an existing owner account. */
    @PostMapping("/owners/{ownerId}/properties")
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse addProperty(@PathVariable String ownerId, @Valid @RequestBody PropertyRequest request) {
        return PropertyResponse.from(propertyService.addProperty(ownerId, request.toCommand()));
    }
}
