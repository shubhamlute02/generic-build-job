package com.accrevent.radius.controller;

import com.accrevent.radius.model.SelectedLifecycle;
import com.accrevent.radius.repository.SelectedLifecycleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/selectedLifecycle")
public class SelectedLifecycleController {

    @Autowired
    private SelectedLifecycleRepository repository;

    // Initialize with default values if not present
//    @PostMapping("/init")
//    public SelectedLifecycle initializeDefaults() {
//        Optional<SelectedLifecycle> existing = repository.findById(1L);
//        if (existing.isPresent()) {
//            return existing.get();
//        }
//
//        SelectedLifecycle defaultLifecycle = new SelectedLifecycle();
//        defaultLifecycle.setId(1L);
//        defaultLifecycle.setCampaign(1L);       // default1
//        defaultLifecycle.setOpportunity(2L);    // default2
//        defaultLifecycle.setLead(3L);           // default3
//        return repository.save(defaultLifecycle);
//    }

    // Get the selected lifecycle config
    @GetMapping("/get")
    public ResponseEntity<SelectedLifecycle> getSelectedLifecycle() {
        return repository.findById(1L)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Update the selected lifecycle config
    @PutMapping("/update")
    public ResponseEntity<SelectedLifecycle> updateSelectedLifecycle(@RequestBody SelectedLifecycle updated) {
        return repository.findById(1L).map(existing -> {
            existing.setCampaign(updated.getCampaign());
            existing.setOpportunity(updated.getOpportunity());
            existing.setLead(updated.getLead());
            return ResponseEntity.ok(repository.save(existing));
        }).orElse(ResponseEntity.notFound().build());
    }
}
