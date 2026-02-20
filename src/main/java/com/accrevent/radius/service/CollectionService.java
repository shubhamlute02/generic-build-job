package com.accrevent.radius.service;

import com.accrevent.radius.dto.CollectionDTO;
import com.accrevent.radius.model.Collection;
import com.accrevent.radius.model.MarketingStory;
import com.accrevent.radius.model.Workspace;
import com.accrevent.radius.repository.CollectionRepository;
import com.accrevent.radius.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private final CollectionRepository collectionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final TaskService taskService;

    public CollectionService(CollectionRepository collectionRepository, WorkspaceRepository workspaceRepository, TaskService taskService) {
        this.collectionRepository = collectionRepository;
        this.workspaceRepository = workspaceRepository;
        this.taskService = taskService;
    }

    public CollectionDTO  createCollection(CollectionDTO collectionDTO){
        Collection collection = new Collection();
        collection.setDisplayName(collectionDTO.getDisplayName());

        if (collectionDTO.getWorkspaceId() != null) {
            Workspace workspace = workspaceRepository.findById(collectionDTO.getWorkspaceId())
                    .orElseThrow(() -> new RuntimeException("Workspace not found with id: " + collectionDTO.getWorkspaceId()));
            collection.setWorkspace(workspace);
        }
        // Save entity
        Collection saved = collectionRepository.save(collection);

        // Convert back Entity → DTO
        return transformToDTO(saved);

    }

    public List<CollectionDTO> getAllCollections() {
        List<Collection> collections = collectionRepository.findAll();
        return collections.stream()
                .map(this::transformToDTO)
                .collect(Collectors.toList());
    }



    private CollectionDTO transformToDTO(Collection collection) {
        CollectionDTO dto = new CollectionDTO();
        dto.setMarketingCollectionId(collection.getMarketingCollectionId());
        dto.setDisplayName(collection.getDisplayName());
        dto.setWorkspaceId(
                collection.getWorkspace() != null ? collection.getWorkspace().getWorkspaceId() : null
        );
        dto.setMarketingStoryIds(
                collection.getMarketingStories()
                        .stream()
                        .map(MarketingStory::getId)
                        .collect(Collectors.toList())
        );

        List<MarketingStory> stories = collection.getMarketingStories();

        // ✅ Compute in-work count by excluding last lifecycle of each version
        String finalStage = taskService.getFinalStageStatus(7L); // same as your reference method

        long inWorkCount = stories.stream()
                .flatMap(story -> story.getEditions().stream())
                .flatMap(edition -> edition.getVersions().stream())
                .filter(version -> !finalStage.equalsIgnoreCase(version.getStatus()))
                .count();

        dto.setInWorkVersionCount(inWorkCount);


        return dto;
    }

}
