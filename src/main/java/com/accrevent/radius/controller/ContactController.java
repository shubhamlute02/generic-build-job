package com.accrevent.radius.controller;

import com.accrevent.radius.dto.ContactDTO;
import com.accrevent.radius.model.Contact;
import com.accrevent.radius.service.ContactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contact")
public class ContactController {

    private  final ContactService contactService;

    public ContactController(ContactService contactService) {

        this.contactService = contactService;
    }

    @PostMapping("/addContact")
    public ResponseEntity<Map<String, Object>> addContact(@RequestBody ContactDTO contactDTO) {
        Map<String, Object> body = new HashMap<>();
        try {
            System.out.println("Received DTO: " + contactDTO);

            ContactDTO saved = contactService.addContact(contactDTO);

            body.put("status", "success");
            body.put("message", "Contact successfully created.");
            body.put("contact", saved);
            return new ResponseEntity<>(body, HttpStatus.CREATED);

        } catch (RuntimeException e) {
            body.put("status", "error");
            body.put("message", e.getMessage());
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("/getContactsByCompanyId")
    public ResponseEntity<?> getContactsByCompanyId(@RequestParam Long companyId) {
        Map<String, Object> body = new HashMap<>();
      try{
          List<ContactDTO> contacts = contactService.getContactsByCompanyId(companyId);
          return ResponseEntity.ok(contacts);
      } catch (RuntimeException e) {
          body.put("status", "error");
          body.put("message", e.getMessage());
          return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
      }
    }

    @GetMapping("/getContactById")
    public ResponseEntity<?> getContactById(@RequestParam Long contactId) {
        try {
            ContactDTO contact = contactService.getContactById(contactId);
            return ResponseEntity.ok(contact);
        } catch (RuntimeException e) {
            Map<String, Object> body = new HashMap<>();
            body.put("status", "error");
            body.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }



    @PutMapping("/updateContact")
    public ResponseEntity<Map<String, Object>> updateContact(
            @RequestParam Long id,
            @RequestBody ContactDTO contactDTO) {
        Map<String, Object> body = new HashMap<>();
        try {
            ContactDTO updated = contactService.updateContact(id, contactDTO);

            body.put("status", "success");
            body.put("message", "Contact successfully updated.");
            body.put("contact", updated);
            return new ResponseEntity<>(body, HttpStatus.OK);

        } catch (RuntimeException e) {
            body.put("status", "error");
            body.put("message", e.getMessage());
            return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
        }
    }

}
