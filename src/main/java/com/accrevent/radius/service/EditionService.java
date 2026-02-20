package com.accrevent.radius.service;

import com.accrevent.radius.dto.EditionDTO;
import com.accrevent.radius.dto.EditionRequestDTO;
import com.accrevent.radius.dto.SharePointURLDTO;
import com.accrevent.radius.dto.VersionDTO;
import com.accrevent.radius.model.Edition;
import com.accrevent.radius.model.MarketingStory;
import com.accrevent.radius.model.Version;
import com.accrevent.radius.repository.EditionRepository;
import com.accrevent.radius.repository.MarketingStoryRepository;
import com.accrevent.radius.util.MarketingContentConstants;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EditionService {

    private final VersionService versionService;
    private final MarketingStoryRepository marketingStoryRepository;
    private final EditionRepository editionRepository;
    private final SharePointService sharePointService;
    private final MarketingContentConstants marketingContentConstants;

    private static final Logger logger = LoggerFactory.getLogger(EditionService.class);

    public EditionService(VersionService versionService, MarketingStoryRepository marketingStoryRepository, EditionRepository editionRepository, SharePointService sharePointService, MarketingContentConstants marketingContentConstants) {
        this.versionService = versionService;
        this.marketingStoryRepository = marketingStoryRepository;
        this.editionRepository = editionRepository;
        this.sharePointService = sharePointService;
        this.marketingContentConstants = marketingContentConstants;
    }

    @Transactional
    public EditionDTO createEdition(EditionRequestDTO request) {
        //  Fetch the parent MarketingStory
        MarketingStory story = marketingStoryRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("MarketingStory not found with id " + request.getId()));

        //  Ensure Story folder exists (but do not auto-create)
        if (story.getSharepointUrl() != null) {
            SharePointURLDTO storyFolder = ensureFolderExists(
                    story.getSharepointUrl(),
                    story.getCollection().getSharepointUrl(),
                    story.getTitle(),
                    false
            );
            if (storyFolder != null) {
                story.setSharepointUrl(storyFolder.getWebUrl());  // clean webUrl
                story.setSharepointPath(storyFolder.getId());     // itemId
                logger.info("Story SharePoint path: {}", storyFolder.getId());
                marketingStoryRepository.save(story);
            }
        }

        // Create new Edition entity
        Edition edition = new Edition();
        edition.setContentType(request.getContentType());
        edition.setMarketingStory(story);

        // Ensure Edition folder exists (under story folder)
        if (story.getSharepointUrl() != null) {
            SharePointURLDTO editionFolder = ensureFolderExists(
                    edition.getSharepointUrl(),     // initially null
                    story.getSharepointUrl(),       // parent
                    edition.getContentType(),
                    true // allow creation here
            );

            if (editionFolder != null) {
                edition.setSharepointUrl(editionFolder.getWebUrl());  // clean webUrl
                edition.setSharepointPath(story.getSharepointPath() + "/" + edition.getContentType());
                edition.setSharepointItemId(editionFolder.getId());     // itemId
            }
        }

        // Save and return DTO
        Edition saved = editionRepository.save(edition);
        return transformToEditionDTO(saved);
    }

    private SharePointURLDTO ensureFolderExists(String existingUrl, String parentUrl, String folderName, boolean createIfMissing) {
        if (parentUrl == null || parentUrl.isEmpty()) {
            logger.info(" Parent folder missing, skipping creation for: {}" ,folderName);
            return null;
        }

        logger.info(">>> URL passed to getSiteIdFromUrl ensureFolderExists: {}" , parentUrl);

        String driveId = sharePointService.getDriveIdFromSiteId(
                sharePointService.getSiteIdFromUrl(parentUrl)
        );

        // Step 1: Try to resolve existing URL safely
        if (existingUrl != null && !existingUrl.isEmpty()) {
            SharePointURLDTO dto = sharePointService.resolveFolderFromUrl(driveId, existingUrl);
            if (dto != null) {
                return dto; // full DTO (id + webUrl)
            } else {
                logger.info(" Existing folder not found:{} " , existingUrl);
            }
        }

        // Step 2: Only create folder if allowed
        if (createIfMissing) {
            return sharePointService.createFolderIfNotPresent(
                    parentUrl,
                    new String[]{folderName}
            );
        } else {
            return null;
        }
    }

    public EditionDTO updateEdition(EditionRequestDTO request) {
        Edition edition = editionRepository.findById(request.getId())
                .orElseThrow(() -> new RuntimeException("Edition not found with id " + request.getId()));

        edition.setContentType(request.getContentType());
        Edition savedEdition = editionRepository.save(edition);

        // map to DTO
        EditionDTO dto = new EditionDTO();
        dto.setId(savedEdition.getId());
        dto.setContentType(savedEdition.getContentType());
        dto.setVersions(savedEdition.getVersions().stream().map(version -> {
            VersionDTO vdto = new VersionDTO();
            vdto.setVersionId(version.getVersionId());
            vdto.setVersion(version.getVersion());
            vdto.setDescription(version.getDescription());
            vdto.setSharepointPath(version.getSharepointPath());
            vdto.setStatus(version.getStatus());
            return vdto;
        }).collect(Collectors.toList()));

        return dto;
    }

@Transactional
public void deleteEdition(Long editionId) {
    Edition edition = editionRepository.findById(editionId)
            .orElseThrow(() -> new RuntimeException("Edition not found with ID: " + editionId));

    // Check if it has Versions
    if (!edition.getVersions().isEmpty()) {
        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Cannot delete Edition. It has associated Versions."
        );
    }

    String folderUrl = edition.getSharepointUrl();

    if (folderUrl != null && !folderUrl.isEmpty()) {
        try {
            // Use the robust deleteFolderByUrl method
            sharePointService.deleteFolderByUrl(folderUrl);
        } catch (Exception e) {
            logger.info("Folder not found or already deleted: {}" , folderUrl );
        }
    }

    editionRepository.delete(edition);

}

    public List<String> getContentTypes() {
        return new ArrayList<>(marketingContentConstants.ContentType);
    }


    public Edition transformToEntity(EditionDTO dto, MarketingStory parentStory) {
        if (dto == null) return null;

        Edition edition = new Edition();

        // preserve ID for updates
        if (dto.getId() != null) {
            edition.setId(dto.getId());
        }

        edition.setContentType(dto.getContentType());
        edition.setMarketingStory(parentStory);

        if (dto.getVersions() != null) {
            List<Version> versions = dto.getVersions().stream()
                    .map(v -> {
                        Version version = versionService.transformToEntity(v, edition);
                        version.setEdition(edition); // explicitly attach back
                        return version;
                    })
                    .collect(Collectors.toList());
            edition.setVersions(versions);
        } else {
            edition.setVersions(new ArrayList<>());
        }

        return edition;
    }


    public EditionDTO transformToEditionDTO(Edition edition) {
        if (edition == null) return null;

        EditionDTO dto = new EditionDTO();
        dto.setId(edition.getId());
        dto.setContentType(edition.getContentType());

        List<VersionDTO> versionDTOs = edition.getVersions().stream().map(v -> {
            VersionDTO versionDTO = new VersionDTO();
            versionDTO.setVersionId(v.getVersionId());
            versionDTO.setVersion(v.getVersion());
            versionDTO.setDescription(v.getDescription());
            versionDTO.setSharepointPath(v.getSharepointPath());
            versionDTO.setStatus(v.getStatus());
            versionDTO.setSharepointUrl(v.getSharepointUrl());
            versionDTO.setEditionId(edition.getId());
            return versionDTO;
        }).toList();

        dto.setVersions(versionDTOs);
        return dto;
    }
}
