package com.accrevent.radius.service;

import com.accrevent.radius.dto.UserResearchSequenceDTO;
import com.accrevent.radius.model.UserResearchSequence;
import com.accrevent.radius.repository.UserResearchSequenceRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompanyResearchSequenceService {

    @Autowired
    private UserResearchSequenceRepository userResearchSequenceRepository;

    public List<UserResearchSequenceDTO> addSequences(List<UserResearchSequenceDTO> dtos) {

        Set<String> userNames = dtos.stream()
                .map(UserResearchSequenceDTO::getUserName)
                .collect(Collectors.toSet());

        // Load all existing sequences for these userNames
        List<UserResearchSequence> existingSequences = userResearchSequenceRepository.findByUserNameIn(userNames);

        Map<String, UserResearchSequence> existingMap = existingSequences.stream()
                .collect(Collectors.toMap(UserResearchSequence::getUserName, seq -> seq));

        List<UserResearchSequence> sequencesToSave = new ArrayList<>();

        for (UserResearchSequenceDTO dto : dtos) {
            UserResearchSequence sequence = existingMap.get(dto.getUserName());

            if (sequence != null) {
                // Update sequenceOrder if changed
                if (!Objects.equals(sequence.getSequenceOrder(), dto.getSequenceOrder())) {
                    sequence.setSequenceOrder(dto.getSequenceOrder());
                    sequencesToSave.add(sequence);
                }
            } else {
                // Create new sequence if doesn't exist
                UserResearchSequence newSequence = new UserResearchSequence();
                newSequence.setUserName(dto.getUserName());
                newSequence.setSequenceOrder(dto.getSequenceOrder());
                sequencesToSave.add(newSequence);
            }
        }

        List<UserResearchSequence> savedEntities = userResearchSequenceRepository.saveAll(sequencesToSave);

        return savedEntities.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }


    private UserResearchSequenceDTO convertToDTO(UserResearchSequence entity) {
        UserResearchSequenceDTO dto = new UserResearchSequenceDTO();
        dto.setUserResearchSequenceId(entity.getUserResearchSequenceId());
        dto.setUserName(entity.getUserName());
        dto.setSequenceOrder(entity.getSequenceOrder());
        return dto;
    }


    @Transactional
    public List<UserResearchSequenceDTO> updateResearchSequence(List<UserResearchSequenceDTO> dtoList) {
        List<UserResearchSequenceDTO> updatedList = new ArrayList<>();

        for (UserResearchSequenceDTO dto : dtoList) {
            UserResearchSequence entity = userResearchSequenceRepository.findById(dto.getUserResearchSequenceId())
                    .orElseThrow(() -> new RuntimeException(
                            "UserResearchSequence not found with ID: " + dto.getUserResearchSequenceId()));

            entity.setUserName(dto.getUserName());
            entity.setSequenceOrder(dto.getSequenceOrder());

            UserResearchSequence saved = userResearchSequenceRepository.save(entity);

            // Map entity back to DTO
            UserResearchSequenceDTO updatedDto = new UserResearchSequenceDTO();
            updatedDto.setUserResearchSequenceId(saved.getUserResearchSequenceId());
            updatedDto.setUserName(saved.getUserName());
            updatedDto.setSequenceOrder(saved.getSequenceOrder());

            updatedList.add(updatedDto);
        }

        return updatedList;
    }

}
