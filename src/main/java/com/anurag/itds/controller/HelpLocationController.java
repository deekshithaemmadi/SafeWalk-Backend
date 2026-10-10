package com.anurag.itds.controller;

import com.anurag.itds.dto.HelpLocationResponse;
import com.anurag.itds.service.HelpLocationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, read-only help-location directory. Served only from imported data —
 * nothing is fabricated. Results are paginated ({@code page}, {@code size}).
 */
@RestController
@RequestMapping("/api/help-locations")
public class HelpLocationController {

    private final HelpLocationService helpLocationService;

    public HelpLocationController(HelpLocationService helpLocationService) {
        this.helpLocationService = helpLocationService;
    }

    @GetMapping
    public Page<HelpLocationResponse> search(@RequestParam(required = false) String type,
                                              @RequestParam(required = false) String q,
                                              @RequestParam(required = false) String district,
                                              Pageable pageable) {
        return helpLocationService.search(helpLocationService.parseType(type), q, district, pageable);
    }
}
