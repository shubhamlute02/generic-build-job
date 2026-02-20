package com.accrevent.radius.service;

import com.accrevent.radius.dto.*;
import com.accrevent.radius.model.*;
import com.accrevent.radius.model.Collection;
import com.accrevent.radius.repository.CollectionRepository;
import com.accrevent.radius.repository.EditionRepository;
import com.accrevent.radius.repository.MarketingStoryRepository;
import com.accrevent.radius.repository.VersionRepository;
import com.accrevent.radius.util.LifecycleName;
import com.accrevent.radius.util.MarketingContentConstants;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MarketingStoryService {

    private final MarketingStoryRepository marketingStoryRepository;
    private final EditionService editionService;
    private final EditionRepository editionRepository;
    private final VersionService versionService;
    private final VersionRepository versionRepository;
    private final CollectionRepository collectionRepository;
    private final TaskService taskService;
    private final SharePointService sharePointService;
    private final MarketingContentConstants marketingContentConstants;

    private static final Logger logger = LoggerFactory.getLogger(MarketingStoryService.class);

    @Value("${sharepoint.channelFolderUrl}")
    private String channelFolderUrl;

    public MarketingStoryService(MarketingStoryRepository marketingStoryRepository, EditionService editionService, EditionRepository editionRepository, VersionService versionService, VersionRepository versionRepository, CollectionRepository collectionRepository, TaskService taskService, SharePointService sharePointService, MarketingContentConstants marketingContentConstants) {
        this.marketingStoryRepository = marketingStoryRepository;
        this.editionService = editionService;
        this.editionRepository = editionRepository;
        this.versionService = versionService;
        this.versionRepository = versionRepository;
        this.collectionRepository = collectionRepository;
        this.taskService = taskService;
        this.sharePointService = sharePointService;
        this.marketingContentConstants = marketingContentConstants;
    }

    @Transactional
    public MarketingStoryDTO createMarketingStory(MarketingStoryDTO marketingStoryDTO) {
        MarketingStory story = transformToEntity(marketingStoryDTO);
        Collection collection = story.getCollection();

        // Ensure Collection folder exists under channel
        if (collection.getSharepointUrl() == null || collection.getSharepointUrl().isBlank()) {
            String collectionWebUrl = ensureFolderExists(
                    null, // existing URL not present
                    sharePointService.getCurrentChannelFolderUrl(),
                    collection.getDisplayName()
            );
            collection.setSharepointUrl(collectionWebUrl);
            collectionRepository.save(collection);
        }

        // Ensure Story folder exists under Collection
        if (story.getSharepointUrl() == null || story.getSharepointUrl().isBlank()) {
            String storyWebUrl = ensureFolderExists(
                    null,
                    collection.getSharepointUrl(),
                    story.getTitle()
            );
            story.setSharepointUrl(storyWebUrl);
            story.setSharepointPath(collection.getSharepointUrl() + "/" + story.getTitle());
        }


        // Save MarketingStory
        MarketingStory saved = marketingStoryRepository.save(story);
        return transformToMarketingStoryDTO(saved);
    }

    private String ensureFolderExists(String existingUrl, String parentUrl, String folderName) {

        String siteId = sharePointService.getSiteIdFromUrl(parentUrl);
        String driveId = sharePointService.getDriveIdFromSiteId(siteId);

        // Step 1: Try to resolve existing URL (only if it is not empty)
        if (existingUrl != null && !existingUrl.isBlank()) {
            try {
                SharePointURLDTO dto = sharePointService.resolveFolderFromUrl(driveId, existingUrl);
                if (dto != null && dto.getWebUrl() != null) {
                    logger.info("Found existing folder: {}" , dto.getWebUrl());
                    return dto.getWebUrl(); // reuse existing folder
                }
            } catch (Exception e) {
                // Folder not found (likely deleted)
                System.out.println("Existing folder not valid, will recreate: " + existingUrl);
            }
        }

        // Step 2: Create new folder under parent
        try {
            SharePointURLDTO folder = sharePointService.createFolderIfNotPresent(
                    parentUrl,
                    new String[]{folderName}
            );
            logger.info(" Created new folder: {}" , folder.getWebUrl());
            return folder.getWebUrl();
        } catch (Exception e) {
            throw new RuntimeException(" Failed to create folder '" + folderName
                    + "' under parent: " + parentUrl, e);
        }
    }


    @Transactional
public MarketingStoryDTO updateMarketingStory(Long id, MarketingStoryDTO dto) {
    MarketingStory story = marketingStoryRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Marketing Story not found with id: " + id));

    String oldTitle = story.getTitle();

    // TRIM THE TITLE to remove leading/trailing spaces
    String newTitle = dto.getTitle() != null ? dto.getTitle().trim() : "";
    boolean titleChanged = !oldTitle.equals(newTitle);

    // Update story fields with TRIMMED title
    story.setTitle(newTitle);
    story.setDescription(dto.getDescription());
    story.setPurpose(dto.getPurpose());
    story.setContentConsumer(dto.getContentConsumer());

    if (dto.getMarketingCollectionId() != null) {
        Collection collection = collectionRepository.findById(dto.getMarketingCollectionId())
                .orElseThrow(() -> new RuntimeException("Collection not found with id: " + dto.getMarketingCollectionId()));
        story.setCollection(collection);
    }

    // Handle SharePoint folder rename if title changed
    if (titleChanged) {
        renameStoryFolder(story, oldTitle);

        // Update editions first
        updateEditionFoldersAfterRename(story, oldTitle);
        // Also update all version folders to reflect the new story title
        updateVersionFoldersAfterRename(story, oldTitle);


    }

    marketingStoryRepository.save(story);
    return transformToMarketingStoryDTO(story);
}


    private void renameStoryFolder(MarketingStory story, String oldTitle) {
        logger.info("Enter into renameStoryFolder - Old: {}, New: {}", oldTitle, story.getTitle());

        try {
            String trimmedNewTitle = story.getTitle().trim();
            String storyFolderUrl = story.getSharepointUrl();
            if (storyFolderUrl == null || storyFolderUrl.isEmpty()) {
                return;
            }

            SharePointURLDTO folder;
            String driveId;

            if (storyFolderUrl.contains("/sites/")) {
                // Site-based URL
                String siteUrl = sharePointService.extractSiteUrl(storyFolderUrl);
                String siteId = sharePointService.getSiteIdFromUrl(siteUrl);
                driveId = sharePointService.getDriveIdFromSiteId(siteId);
                folder = sharePointService.resolveFolderFromUrl(driveId, storyFolderUrl);
            } else {
                // Non-site URL, resolve directly via /shares API
                folder = sharePointService.resolveFolderFromUrl(storyFolderUrl);
                driveId = folder.getParentDriveId();
            }

            // Rename by ID
            sharePointService.renameFolderById(driveId, folder.getId(), trimmedNewTitle);

            // Update DB path/url
            String newUrl = updateStoryFolderUrl(folder.getWebUrl(), trimmedNewTitle);
            String collectionName = story.getCollection() != null
                    ? story.getCollection().getDisplayName()
                    : "UntitledCollection";
            story.setSharepointUrl(newUrl);
            story.setSharepointPath(collectionName + "/" + trimmedNewTitle);

            logger.info(" Successfully renamed STORY folder. New URL: {}" , newUrl);

        } catch (Exception e) {
            throw new RuntimeException("Failed to rename SharePoint folder", e);
        }
    }

    private void updateEditionFoldersAfterRename(MarketingStory story, String oldTitle) {
        for (Edition edition : story.getEditions()) {
            try {
                if (edition.getSharepointUrl() != null && !edition.getSharepointUrl().isEmpty()) {
                    String oldEditionUrl = edition.getSharepointUrl();

                    String oldTitleEncoded = encodePathSegment(oldTitle);
                    String newTitleEncoded = encodePathSegment(story.getTitle());

                    //  Replace old story title with new one directly in edition URL
                    String newEditionUrl = oldEditionUrl.replace(oldTitleEncoded, newTitleEncoded);

                    // Update Edition Path (unencoded, human-readable)
                    String oldEditionPath = edition.getSharepointPath();
                    String newEditionPath = oldEditionPath.replace(oldTitle, story.getTitle());

                    // Save updates
                    edition.setSharepointUrl(newEditionUrl);
                    edition.setSharepointPath(newEditionPath);

                    logger.info("Updated edition folder: {} -> {}", oldEditionUrl, newEditionUrl);


                }
            } catch (Exception e) {
                logger.info(" Failed to update edition folder for contentType: {}" , edition.getContentType());
                e.printStackTrace();
            }
        }
    }


    private void updateVersionFoldersAfterRename(MarketingStory story, String oldTitle) {
        String channelFolderUrl = sharePointService.getCurrentChannelFolderUrl();

        for (Edition edition : story.getEditions()) {
            for (Version version : edition.getVersions()) {
                try {
                    if (version.getSharepointUrl() != null && !version.getSharepointUrl().isEmpty()) {
                        // Update version folder paths to reflect new story title
                        String oldVersionUrl = version.getSharepointUrl();
                        logger.info("oldVersionUrl {}",oldVersionUrl);

                        // Encode old and new titles for URL
                        String oldTitleEncoded = encodePathSegment(oldTitle);
                        String newTitleEncoded = encodePathSegment(story.getTitle());

                        String newVersionUrl = oldVersionUrl.replace(oldTitleEncoded, newTitleEncoded);

                        logger.info("newVersionUrl {}",newVersionUrl);
                        String oldVersionPath = version.getSharepointPath();
                        String newVersionPath = oldVersionPath.replace(oldTitle, story.getTitle());

                        version.setSharepointUrl(newVersionUrl);
                        version.setSharepointPath(newVersionPath);

                       logger.info("Updated version folder: {} -> {}" , oldVersionUrl , newVersionUrl);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to update version folder for version " + version.getVersion());
                    e.printStackTrace();
                }
            }
        }
    }

    private String trimToCollectionUrl(String fullUrl, String storyTitle) {
        if (fullUrl == null || storyTitle == null) return fullUrl;

        // Encode title for URL-safe comparison
        String encodedStoryTitle = encodePathSegment(storyTitle);

        // Find the position of the story folder in the URL
        int index = fullUrl.indexOf("/" + encodedStoryTitle);
        if (index == -1) {
            // story folder not found, return full URL
            return fullUrl;
        }

        // Keep everything up to the story folder (exclusive)
        return fullUrl.substring(0, index);
    }

    public static String encodePathSegment(String segment) {
        try {
            return new URI(null, null, segment, null).getRawPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    private String updateStoryFolderUrl(String oldUrl, String newTitle) {
        try {
            URI uri = new URI(oldUrl);
            String decodedPath = URLDecoder.decode(uri.getPath(), StandardCharsets.UTF_8);

            String[] segments = decodedPath.split("/");
            String lastSegment = segments[segments.length - 1]; // e.g. "marketing story title1234"

            // Replace whole last segment with new title (spaces preserved)
            segments[segments.length - 1] = newTitle;

            // Rebuild decoded path
            String newDecodedPath = String.join("/", segments);

            // Rebuild full URL (with encoding for spaces)
            return uri.getScheme() + "://" + uri.getHost() + newDecodedPath.replace(" ", "%20");

        } catch (Exception e) {
            throw new RuntimeException("Error updating story folder URL", e);
        }
    }

    public MarketingStoryDTO getMarketingStoryDetails(Long id) {
        MarketingStory story = marketingStoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marketing Story not found with id: " + id));

        return transformToMarketingStoryDTO(story);
    }


    public void deleteMarketingStory(Long id) {
        MarketingStory story = marketingStoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Marketing Story not found with ID: " + id));

        // Delete the MarketingStory folder itself
        String folderUrl = story.getSharepointUrl();

        if (folderUrl != null && !folderUrl.isEmpty()) {
            try {
                sharePointService.deleteFolderByUrl(folderUrl);
            } catch (Exception e) {
                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete associated folder: " + e.getMessage());
            }
        }

        marketingStoryRepository.delete(story);
    }

    private String trimToMarketingStoryUrl(String url) {
        if (url == null || !url.contains("/")) return url;
        return url.substring(0, url.lastIndexOf("/"));
    }

    public List<String> getContentConsumers(){
        return new ArrayList<>(marketingContentConstants.ContentConsumers);
    }


    public Map<String, Object> getInWorkMarketingStoriesByCollection(Long collectionId) {
        // Get all stories in the collection
        List<MarketingStory> stories = marketingStoryRepository.findByCollection_MarketingCollectionId(collectionId);

        // Get final stage dynamically (for versions → cycleId = 7)
        String finalStage = taskService.getFinalStageStatus(7L);

        // Collect only in-work versions
        List<InWorkVersionResponseDTO> inWorkVersions = stories.stream()
                .flatMap(story -> story.getEditions().stream()
                        .flatMap(edition -> edition.getVersions().stream()
                                .filter(version -> {

                                    if (finalStage.equalsIgnoreCase(version.getStatus())) {
                                        return false;
                                    }

                                    boolean isInFinalStageLifecycle = version.getLifecycles().stream()
                                            .anyMatch(lc -> "active".equalsIgnoreCase(lc.getStatus())
                                                    && finalStage.equalsIgnoreCase(lc.getLifecycleName()));
                                    return !isInFinalStageLifecycle;
                                })
                                .map(version -> {
                                    InWorkVersionResponseDTO dto = new InWorkVersionResponseDTO();
                                    dto.setVersionId(version.getVersionId());
                                    dto.setVersion(version.getVersion());
                                    dto.setStatus(version.getStatus());

                                    dto.setMarketingStoryId(story.getId());
                                    dto.setMarketingStoryTitle(story.getTitle());

                                    dto.setContentType(edition.getContentType());
                                    dto.setContentConsumer(story.getContentConsumer());
                                    dto.setDescription(story.getDescription());

                                    dto.setInactive(isInactive(version.getLifecycles()));

                                    // logic for earliest due date
                                    Task earliestDueTask = null;
                                    Long earliestDueDate = null;
                                    String associatedLifecycleName = null;

                                    List<Lifecycle> activeLifecycles = version.getLifecycles().stream()
                                            .filter(lc -> "active".equalsIgnoreCase(lc.getStatus()))
                                            .collect(Collectors.toList());

                                    for (Lifecycle lifecycle : activeLifecycles) {
                                        // skip final lifecycle
                                        if (finalStage.equalsIgnoreCase(lifecycle.getLifecycleName())) {
                                            continue;
                                        }

                                        if (lifecycle.getTaskList() != null) {
                                            for (Task task : lifecycle.getTaskList()) {

                                                // Exclude completed & not started
                                                if (LifecycleName.COMPLETED.equalsIgnoreCase(task.getStatus()) ||
                                                        LifecycleName.NOT_STARTED.equalsIgnoreCase(task.getStatus())) {
                                                    continue;
                                                }

                                                if (task.getDueDate() != null) {
                                                    if (earliestDueDate == null || task.getDueDate() < earliestDueDate) {
                                                        earliestDueTask = task;
                                                        earliestDueDate = task.getDueDate();
                                                        associatedLifecycleName = lifecycle.getLifecycleName();
                                                    }
                                                }

                                                if (task.getSubTasks() != null) {
                                                    for (Task sub : task.getSubTasks()) {

                                                        // Exclude completed & not started
                                                        if (LifecycleName.COMPLETED.equalsIgnoreCase(task.getStatus()) ||
                                                                LifecycleName.NOT_STARTED.equalsIgnoreCase(task.getStatus())) {
                                                            continue;
                                                        }

                                                        if (sub.getDueDate() != null) {
                                                            if (earliestDueDate == null || sub.getDueDate() < earliestDueDate) {
                                                                earliestDueTask = sub;
                                                                earliestDueDate = sub.getDueDate();
                                                                associatedLifecycleName = lifecycle.getLifecycleName();
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Set date & lifecycle name in DTO
                                    System.out.println(earliestDueDate + "earliestDueDate");
                                    dto.setLatestDueDate(earliestDueDate);

                                    return dto;
                                })
                        )
                )
                .collect(Collectors.toList());

        // response
        Map<String, Object> response = new HashMap<>();
        response.put("inWorkMarketingStory", inWorkVersions);
        return response;
    }


    private boolean isInactive(List<Lifecycle> lifecycles) {

        List<Task> allTasks = new ArrayList<>();

        for (Lifecycle lifecycle : lifecycles) {

            // only active lifecycles
            if (!"active".equalsIgnoreCase(lifecycle.getStatus())) {
                continue;
            }

            if (lifecycle.getTaskList() == null) continue;

            for (Task task : lifecycle.getTaskList()) {
                allTasks.add(task);

                if (task.getSubTasks() != null) {
                    allTasks.addAll(task.getSubTasks());
                }
            }
        }

        // No tasks at all
        if (allTasks.isEmpty()) {
            return true;
        }

        // All tasks completed
        return allTasks.stream()
                .allMatch(t -> "completed".equalsIgnoreCase(t.getStatus()));
    }



    public Map<String, Object> getInWorkAndLatestApprovedMarketingStoriesByCollectionId(Long collectionId) {
        // Fetch all stories for the collection
        List<MarketingStory> stories = marketingStoryRepository.findByCollection_MarketingCollectionId(collectionId);

        // Get final stage dynamically (for versions → cycleId = 7)
        String finalStage = taskService.getFinalStageStatus(7L);

        List<MarketingStoryDTO> filteredStories = new ArrayList<>();

        for (MarketingStory story : stories) {
            MarketingStoryDTO storyDTO = transformToMarketingStoryDTO(story);

            // Filter editions + versions
            List<EditionDTO> filteredEditions = new ArrayList<>();

            for (Edition edition : story.getEditions()) {
                EditionDTO editionDTO = new EditionDTO();
                editionDTO.setId(edition.getId());
                editionDTO.setContentType(edition.getContentType());

                List<VersionDTO> inWorkVersions = new ArrayList<>();
                VersionDTO latestApproved = null;
                Long latestApprovedAt = 0L;

                for (Version version : edition.getVersions()) {
                    // In-work → keep
                    if (!finalStage.equalsIgnoreCase(version.getStatus())) {
                        inWorkVersions.add(versionService.transformToVersionDTO(version));
                    }

                    //  Approved → keep only the latest one
                    if (finalStage.equalsIgnoreCase(version.getStatus()) && version.getApprovedAt() != null) {
                        if (version.getApprovedAt() > latestApprovedAt) {
                            latestApproved = versionService.transformToVersionDTO(version);
                            latestApprovedAt = version.getApprovedAt();
                        }
                    }
                }

                // Add in-work + latest approved (if exists)
                List<VersionDTO> finalVersions = new ArrayList<>(inWorkVersions);
                if (latestApproved != null) {
                    finalVersions.add(latestApproved);
                }

                // Only keep edition if it has any versions
                if (!finalVersions.isEmpty()) {
                    editionDTO.setVersions(finalVersions);
                    filteredEditions.add(editionDTO);
                }
            }

            // Only keep story if it has editions
            if (!filteredEditions.isEmpty()) {
                storyDTO.setEditions(filteredEditions);
                filteredStories.add(storyDTO);
            }
        }

        // Response
        Map<String, Object> response = new HashMap<>();
        response.put("marketingStories", filteredStories);
        return response;
    }


    public List<MarketingStoryDTO> getMarketingStoriesByCollectionId(Long marketingCollectionId) {

        List<MarketingStory> stories = marketingStoryRepository.findByCollection_MarketingCollectionId(marketingCollectionId);
        return stories.stream()
                .map(this::transformToMarketingStoryDTO)
                .collect(Collectors.toList());
    }



    public MarketingStoryDTO transformToMarketingStoryDTO(MarketingStory story) {
        if (story == null) return null;

        MarketingStoryDTO dto = new MarketingStoryDTO();
        dto.setId(story.getId());
        dto.setTitle(story.getTitle());
        dto.setDescription(story.getDescription());
        dto.setPurpose(story.getPurpose());
        dto.setContentConsumer(story.getContentConsumer());

        if (story.getCollection() != null) {
            dto.setMarketingCollectionId(story.getCollection().getMarketingCollectionId());
            dto.setMarketingCollectionName(story.getCollection().getDisplayName());
        }

        if (story.getEditions() != null) {
            dto.setEditions(
                    story.getEditions().stream()
                            .map(editionService::transformToEditionDTO)
                            .collect(Collectors.toList())
            );
        }



        return dto;
    }

    public MarketingStory transformToEntity(MarketingStoryDTO dto) {
        MarketingStory story = new MarketingStory();
        story.setId(dto.getId());
        story.setTitle(dto.getTitle() != null ? dto.getTitle().trim() : null);
        story.setDescription(dto.getDescription());
        story.setPurpose(dto.getPurpose());
        story.setContentConsumer(dto.getContentConsumer());

        if (dto.getMarketingCollectionId() != null) {
            Collection collection = collectionRepository.findById(dto.getMarketingCollectionId())
                    .orElseThrow(() -> new RuntimeException("Collection not found with id: " + dto.getMarketingCollectionId()));
            story.setCollection(collection);
        }
        return story;
    }
}
