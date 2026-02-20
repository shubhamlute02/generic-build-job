package com.accrevent.radius.controller;


import com.accrevent.radius.model.ConstantLifecycle;
import com.accrevent.radius.model.EditLifecycle;
import com.accrevent.radius.repository.ConstantLifecycleRepository;
import com.accrevent.radius.repository.EditLifecycleRepository;
import com.accrevent.radius.repository.LifecycleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/editLifecycle")
public class EditLifecycleController {

    @Autowired
    private EditLifecycleRepository repository;


    @Autowired
    private ConstantLifecycleRepository constantLifecycleRepository;


    @PostMapping("/add")
    public EditLifecycle saveLifecycle(@RequestBody EditLifecycle lifecycleEntity) {
        return repository.save(lifecycleEntity);
    }

    @GetMapping("/getAll")
    public List<EditLifecycle> getAllLifecycles() {
        return repository.findAll();
    }

    @GetMapping("/getById/{id}")
    public ResponseEntity<EditLifecycle> getLifecycleById(@PathVariable Long id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<EditLifecycle> updateLifecycle(
            @PathVariable Long id,
            @RequestBody EditLifecycle updatedLifecycle) {

        return repository.findById(id).map(existing -> {
            existing.setLifecycleStates(updatedLifecycle.getLifecycleStates());
            existing.setLifecycleName(updatedLifecycle.getLifecycleName());
            EditLifecycle saved = repository.save(existing);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<String> deleteLifecycle(@PathVariable Long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return ResponseEntity.ok("Lifecycle with ID " + id + " deleted successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lifecycle with ID " + id + " not found.");
        }
    }

//    @PutMapping("/changeLifecycle/{lifecycleId}/{cycleId}")
//    public ResponseEntity<String> gethello( @PathVariable Long lifecycleId,
//                                            @PathVariable Long cycleId){
//
//        // Step 1: Fetch lifecycle states
//        EditLifecycle lifecycle = repository.findById(lifecycleId).orElse(null);
//        if (lifecycle == null) {
//            return ResponseEntity.notFound().build();
//        }
//
//        // Step 2: Delete old entries for this cycleId
//        constantLifecycleRepository.deleteByCycleId(cycleId);
//
//        // Step 3: Create new entries
//        List<String> states = lifecycle.getLifecycleStates();
//        for (String state : states) {
//            ConstantLifecycle cl = new ConstantLifecycle();
//            cl.setCycleId(cycleId);
//            cl.setCycleName(state);
//            constantLifecycleRepository.save(cl);
//        }
//
//        return ResponseEntity.ok("ConstantLifecycle states updated successfully for cycleId " + cycleId);
//    }

    //we asign lifecycle to any lead/campaign/opportunity
    //added new param as type here
@PutMapping("/changeLifecycle/{lifecycleId}/{cycleId}")
public ResponseEntity<Map<String, Object>> changeLifecycleStates(@PathVariable Long lifecycleId,
                                                                 @RequestParam String requiredType,
                                                                 @PathVariable Long cycleId) {

    // Step 1: Fetch lifecycle states
    EditLifecycle lifecycle = repository.findById(lifecycleId).orElse(null);
    if (lifecycle == null) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Lifecycle not found for ID: " + lifecycleId);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }
    Map<String, Object> response = new HashMap<>();

    // Optional: Validate lifecycle type
    if (requiredType != null && !requiredType.equalsIgnoreCase(lifecycle.getType())) {
        response.put("success", false);
        response.put("message", "Lifecycle type mismatch: expected " + requiredType + " but got " + lifecycle.getType());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    // Step 2: Delete old entries for this cycleId
    constantLifecycleRepository.deleteByCycleId(cycleId);

    // Step 3: Create new entries
    List<String> states = lifecycle.getLifecycleStates();
    List<String> createdStates = new ArrayList<>();
    for (String state : states) {
        ConstantLifecycle cl = new ConstantLifecycle();
        cl.setCycleId(cycleId);
        cl.setCycleName(state);
        constantLifecycleRepository.save(cl);
        createdStates.add(state);
    }

    // JSON response body
    response.put("success", true);
    response.put("message", "ConstantLifecycle states updated successfully.");
    response.put("cycleId", cycleId);
    response.put("createdStates", createdStates);

    return ResponseEntity.ok(response);
}

}


