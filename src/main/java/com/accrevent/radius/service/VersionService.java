package com.accrevent.radius.service;

import com.accrevent.radius.dto.SharePointURLDTO;
import com.accrevent.radius.dto.VersionDTO;
import com.accrevent.radius.model.*;
import com.accrevent.radius.repository.ConstantLifecycleRepository;
import com.accrevent.radius.repository.EditionRepository;
import com.accrevent.radius.repository.MarketingStoryRepository;
import com.accrevent.radius.repository.VersionRepository;
import com.accrevent.radius.util.LifecycleName;
import com.accrevent.radius.util.LifecyclePromoteConstants;
import com.accrevent.radius.util.VersionConstant;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.accrevent.radius.util.VersionConstant.VERSION_SEQUENCE;

@Service
public class VersionService {

    private final EditionRepository editionRepository;
    private final ConstantLifecycleRepository constantLifecycleRepository;
    private final VersionRepository versionRepository;
    private final VersionConstant versionConstant;
    private final SharePointService sharePointService;
    private final MarketingStoryRepository marketingStoryRepository;
    private final LifecyclePromoteConstants lifecyclePromoteConstants;

    private static final Logger logger = LoggerFactory.getLogger(VersionService.class);

    @Value("${sharepoint.channelFolderUrl}")
    private String channelFolderUrl;

    public VersionService(EditionRepository editionRepository, ConstantLifecycleRepository constantLifecycleRepository, VersionRepository versionRepository, VersionConstant versionConstant, SharePointService sharePointService, MarketingStoryRepository marketingStoryRepository, LifecyclePromoteConstants lifecyclePromoteConstants) {
        this.editionRepository = editionRepository;
        this.constantLifecycleRepository = constantLifecycleRepository;
        this.versionRepository = versionRepository;
        this.versionConstant = versionConstant;
        this.sharePointService = sharePointService;
        this.marketingStoryRepository = marketingStoryRepository;
        this.lifecyclePromoteConstants = lifecyclePromoteConstants;
    }

    @Transactional
    public VersionDTO createVersion(Long editionId, VersionDTO versionDTO) {

        // Fetch Edition
        Edition edition = editionRepository.findById(editionId)
                .orElseThrow(() -> new RuntimeException("Edition not found with id " + editionId));

        MarketingStory story = edition.getMarketingStory();
        if (story == null) throw new RuntimeException("Edition is not linked to a MarketingStory");

        Collection collection = story.getCollection();
        if (collection == null) throw new RuntimeException("MarketingStory is not linked to a Collection");

        Optional<Version> latestVersionOpt =
                versionRepository.findTopByEditionIdOrderByVersionIdDesc(editionId);

        if (latestVersionOpt.isPresent()) {
            Version latestVersion = latestVersionOpt.get();

            boolean isApproved = latestVersion.getLifecycles().stream()
                    .anyMatch(lifecycle ->
                            LifecycleName.APPROVED.equalsIgnoreCase(lifecycle.getLifecycleName())
                                    && LifecyclePromoteConstants.ACTIVE
                                    .equals(lifecycle.getStatus())
                    );

            if (!isApproved) {
                throw new IllegalStateException(
                        "Cannot create new version. Previous version must be Approved.");
            }
        }


        //  Determine next version label
        int versionCount = versionRepository.countByEditionId(editionId);
        if (versionCount >= VERSION_SEQUENCE.length) {
            throw new IllegalStateException("Maximum version limit reached for this edition.");
        }
        String nextVersion = VERSION_SEQUENCE[versionCount];
        versionDTO.setVersion(nextVersion);

            // --- Set default description for first version ---
            if (versionCount == 0 && (versionDTO.getDescription() == null || versionDTO.getDescription().isEmpty())) {
                versionDTO.setDescription("Initial description for Version A");
            }


            // Convert DTO -> Entity
        Version version = transformToEntity(versionDTO, edition);

        String currentStatus = "";
        // Attach lifecycles if none
        if (version.getLifecycles() == null || version.getLifecycles().isEmpty()) {
            List<ConstantLifecycle> constantLifecycles = constantLifecycleRepository.findByCycleId(7L);
            List<Lifecycle> lifecycleList = new ArrayList<>();
            AtomicBoolean isFirst = new AtomicBoolean(true);

            for (ConstantLifecycle constantLifecycle : constantLifecycles) {
                Lifecycle lifecycle = new Lifecycle();
                lifecycle.setLifecycleName(constantLifecycle.getCycleName());
                lifecycle.setVersion(version);
                lifecycle.setType(LifecyclePromoteConstants.DEFAULT);
                if (isFirst.get()) {
                    lifecycle.setStatus(LifecyclePromoteConstants.ACTIVE);
                    currentStatus = constantLifecycle.getCycleName(); //  use first lifecycle name
                    isFirst.set(false);
                } else {
                    lifecycle.setStatus(LifecyclePromoteConstants.INACTIVE);
                }
                isFirst.set(false);
                lifecycleList.add(lifecycle);
            }
            version.setLifecycles(lifecycleList);
        }

        //  Build folder hierarchy
        String rootFolderUrl = this.channelFolderUrl; // root folder from properties

        // Names of folders
        String collectionName = collection.getDisplayName() != null ? collection.getDisplayName().trim() : "UntitledCollection";
        String storyTitle = story.getTitle() != null ? story.getTitle().trim() : "UntitledStory";
        String editionContentType = edition.getContentType() != null ? edition.getContentType().trim() : "DefaultContentType";

        String versionFolderName = nextVersion + " " + currentStatus;

        String[] pathSegments = new String[]{collectionName, storyTitle, editionContentType, versionFolderName};

        // Create folder hierarchy in SharePoint
        SharePointURLDTO versionFolder = sharePointService.createFolderIfNotPresent(rootFolderUrl, pathSegments);

        collection.setSharepointUrl(collection.getSharepointUrl()); // already set
        collection.setSharepointPath(collection.getDisplayName());

        story.setSharepointUrl(story.getSharepointUrl()); // already set
        story.setSharepointPath(collection.getDisplayName() + "/" + story.getTitle());
        marketingStoryRepository.save(story);

        edition.setSharepointUrl(edition.getSharepointUrl()); // SharePoint returned URL
        edition.setSharepointPath(collection.getDisplayName() + "/" + story.getTitle() + "/" + edition.getContentType());
        editionRepository.save(edition);

        version.setSharepointUrl(versionFolder.getWebUrl()); // version-level folder
        version.setSharepointPath(collection.getDisplayName() + "/" + story.getTitle() + "/" + edition.getContentType() + "/" + versionFolderName);

        //  Save Version
        Version saved = versionRepository.save(version);
        return transformToVersionDTO(saved);
    }




    public VersionDTO updateVersion(VersionDTO dto) {
        Version version = versionRepository.findById(dto.getVersionId())
                .orElseThrow(() -> new RuntimeException("Version not found with id " + dto.getVersionId()));

        // Update only fields that are allowed to change
        version.setDescription(dto.getDescription());
        version.setSharepointPath(dto.getSharepointPath());
        version.setStatus(dto.getStatus());
        if (dto.getEditionId() != null) {
            Edition edition = editionRepository.findById(dto.getEditionId())
                    .orElseThrow(() -> new RuntimeException("Edition not found with id " + dto.getEditionId()));
            version.setEdition(edition);
        }

        Version saved = versionRepository.save(version);

        // Build DTO for response
        VersionDTO response = new VersionDTO();
        response.setVersionId(saved.getVersionId());
        response.setVersion(saved.getVersion());
        response.setDescription(saved.getDescription());
        response.setSharepointPath(saved.getSharepointPath());
        response.setStatus(saved.getStatus());
        response.setEditionId(saved.getEdition().getId());

        return response;
    }


    @Transactional
    public void deleteVersion(Long versionId) {
        Version version = versionRepository.findById(versionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Version not found with ID: " + versionId
                ));

        String folderUrl = version.getSharepointUrl(); // folder URL stored in DB
        if (folderUrl != null && !folderUrl.isEmpty()) {
            try {
                boolean isEmpty = sharePointService.isFolderEmpty(folderUrl);

                if (!isEmpty) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Cannot delete Version. Its folder contains SubFolders/documents."
                    );
                }

                // Folder is empty → safe to delete
                sharePointService.deleteFolderByUrl(folderUrl);
                logger.info("Deleted SharePoint folder for Version: {}" ,version.getVersion());

            } catch (ResponseStatusException e) {
                throw e; // propagate 400 or 404
            } catch (Exception e) {
                logger.info("Error checking/deleting folder for Version: {}" , version.getVersion() , ". Skipping deletion.");
            }
        }

        // Safe to delete DB record
        versionRepository.delete(version);
    }


    public List<VersionDTO> getVersionsByEditionId(Long editionId) {
        Edition edition = editionRepository.findById(editionId)
                .orElseThrow(() -> new RuntimeException("Edition not found with id " + editionId));

        return edition.getVersions().stream().map(v -> {
            VersionDTO dto = new VersionDTO();
            dto.setVersionId(v.getVersionId());
            dto.setVersion(v.getVersion());
            dto.setDescription(v.getDescription());
            dto.setSharepointPath(v.getSharepointPath());
            dto.setSharepointUrl(v.getSharepointUrl());
            dto.setStatus(v.getStatus());
            dto.setEditionId(v.getEdition().getId());
            return dto;
        }).collect(Collectors.toList());
    }

    public Version transformToEntity(VersionDTO dto, Edition parentEdition) {
        if (dto == null) return null;

        Version version;

        if (dto.getVersionId() != null) {
            Optional<Version> existingVersionOpt = versionRepository.findById(dto.getVersionId());
            if (existingVersionOpt.isPresent()) {
                version = existingVersionOpt.get();
            } else {
                // Defensive: if not found, create new
                version = new Version();
                version.setVersionId(dto.getVersionId());
            }
        } else {
            // New record
            version = new Version();
        }
        version.setVersionId(dto.getVersionId());
        version.setVersion(dto.getVersion());
        version.setDescription(dto.getDescription());
        version.setSharepointPath(dto.getSharepointPath());
        version.setSharepointUrl(dto.getSharepointUrl());
        version.setStatus(dto.getStatus());
        version.setEdition(parentEdition);

        return version;
    }

    public VersionDTO transformToVersionDTO(Version version) {
        if (version == null) return null;

        VersionDTO dto = new VersionDTO();
        dto.setVersionId(version.getVersionId());
        dto.setVersion(version.getVersion());
        dto.setDescription(version.getDescription());
        dto.setSharepointPath(version.getSharepointPath());
        dto.setSharepointUrl(version.getSharepointUrl());
        dto.setStatus(version.getStatus());
        if (version.getEdition() != null) {
            dto.setEditionId(version.getEdition().getId());
        }
        return dto;
    }
}
