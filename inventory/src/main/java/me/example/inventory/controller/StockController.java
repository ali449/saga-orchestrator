package me.example.inventory.controller;

import lombok.RequiredArgsConstructor;
import me.example.inventory.dto.StockIn;
import me.example.inventory.dto.StockOut;
import me.example.inventory.service.StockService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/stocks", consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class StockController {
    private final StockService service;

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody StockIn model) {
        String stockId = service.create(model);
        return ResponseEntity.created(URI.create("/api/stocks/" + stockId)).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StockOut> getById(@PathVariable String id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean isDeleted = service.delete(id);
        if (isDeleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.badRequest().build();
    }
}
