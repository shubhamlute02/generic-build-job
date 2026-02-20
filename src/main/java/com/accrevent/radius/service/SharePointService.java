package com.accrevent.radius.service;

import com.accrevent.radius.dto.SharePointURLDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class SharePointService {

    @Value("${sharepoint.base-url}")
    private String baseUrl;

    @Value("${sharepoint.channelFolderUrl}")
    private String channelFolderUrl;

    private final RestTemplate restTemplate;
    private final OutlookCalendarAuthService authService;

    private final static Logger logger = LoggerFactory.getLogger(SharePointService.class);

    public SharePointService(RestTemplate restTemplate, OutlookCalendarAuthService authService) {
        this.restTemplate = restTemplate;
        this.authService = authService;
    }

    public String getCurrentChannelFolderUrl() {
        String folderUrl = channelFolderUrl.trim();
        logger.info("Input folder URL: {}" , folderUrl);

        if (isSharingLink(folderUrl)) {
            //  Resolve using /shares endpoint
            resolveFolderIdFromSharingUrl(folderUrl);
            return resolveFolderIdFromSharingUrl(folderUrl);
        } else {
            // Resolve using normal /sites/... path
            resolveFolderIdFromPathUrl(folderUrl);
            return resolveFolderIdFromPathUrl(folderUrl);
        }
    }



    private String resolveFolderIdFromSharingUrl(String sharingUrl) {
        try {

            logger.info("Original sharing URL: {}", sharingUrl);

            String encodedUrl = "u!" + Base64.getUrlEncoder()
                    .encodeToString(sharingUrl.getBytes(StandardCharsets.UTF_8));


            logger.info("Encoded sharing URL: {}", encodedUrl);

            String url = baseUrl + "/shares/" + encodedUrl + "/driveItem";
            logger.info("Final Graph URL: {}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders()),
                    String.class
            );

            logger.info("Graph response status: {}", response.getStatusCode());
            logger.info("Graph response body: {}", response.getBody());

            JSONObject json = new JSONObject(response.getBody());
            logger.info("AAA id={}, webUrl={}", json.getString("id"), json.getString("webUrl"));

            //  Always return the webUrl so it works consistently
            return json.getString("webUrl");

        } catch (HttpStatusCodeException ex) {
            logger.error("Graph error status: {}", ex.getStatusCode());
            logger.error("Graph error body: {}", ex.getResponseBodyAsString());
            throw ex;
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to resolve folder from sharing URL: " + sharingUrl, e);
        }
    }

    private String resolveFolderIdFromPathUrl(String folderUrl) {
        try {
            //  Root folder URL from application properties
            String rootFolderUrl = this.channelFolderUrl;

            logger.info("Root folder URL from channelFolderUrl: {}" , rootFolderUrl);

            // Parse input folder path
            URI uri = URI.create(folderUrl);
            String[] pathParts = uri.getPath().split("/");

            // Get root folder name from channelFolderUrl
            String rootFolderName = extractFolderNameFromUrl(rootFolderUrl);
            logger.info("rootFolderName {}",rootFolderName);

            List<String> folderSegments = new ArrayList<>();
            boolean startAdding = false;
            for (String part : pathParts) {
                if (part.isEmpty()) continue;
                if (part.equalsIgnoreCase(rootFolderName)) {
                    startAdding = true; // start after root folder
                    continue;
                }
                if (startAdding) folderSegments.add(part);
            }

            //  Iterate folder by folder using webUrl
            String currentParentUrl = rootFolderUrl;
            for (String segment : folderSegments) {
                logger.info("Checking folder: {},under parent URL: {}" , segment  , currentParentUrl);

                // Resolve siteId and driveId dynamically for the current folder URL
                String siteId = getSiteIdFromUrl(currentParentUrl);
                String driveId = getDriveIdFromSiteId(siteId);

                // List child folders under current parent
                List<SharePointURLDTO> children = listChildFoldersByUrl(driveId, currentParentUrl);
                Optional<SharePointURLDTO> existing = children.stream()
                        .filter(f -> f.getName().equalsIgnoreCase(segment))
                        .findFirst();

                if (existing.isPresent()) {
                    currentParentUrl = existing.get().getWebUrl();
                    logger.info("Found folder: {},→ URL:{}" , segment , currentParentUrl);
                } else {
                    // Folder missing → log and throw
                    logger.info("Folder missing: {},under parent URL: {}" , segment ,currentParentUrl);
                    throw new RuntimeException("Folder missing: " + segment + " under parent URL: " + currentParentUrl);
                }
            }

            logger.info("currentParentUrl{}",currentParentUrl);
            // Return final folder URL
            return currentParentUrl;

        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve folder URL from path: " + folderUrl, e);
        }
    }


    // Extract folder name from a SharePoint URL (last segment of path)

    private String extractFolderNameFromUrl(String folderUrl) {
        try {
            URI uri = URI.create(folderUrl);
            String[] parts = uri.getPath().split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                if (!parts[i].isEmpty()) return parts[i];
            }
            return "root";
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract folder name from URL: " + folderUrl, e);
        }
    }

    public SharePointURLDTO createFolderByPath(String siteId, String driveId, String parentWebUrl, String folderName) {
        try {
            String url;
            JSONObject body = new JSONObject();
            body.put("name", folderName);
            body.put("folder", new JSONObject());
            body.put("@microsoft.graph.conflictBehavior", "rename");

            if (isSharingLink(parentWebUrl)) {
                // Convert sharing link to Graph API endpoint
                String encodedUrl = "u!" + Base64.getUrlEncoder().encodeToString(parentWebUrl.getBytes(StandardCharsets.UTF_8));
                url = String.format("%s/shares/%s/driveItem/children", baseUrl, encodedUrl);
            } else {
                // Convert normal webUrl to drive-relative path
                URI uri = URI.create(parentWebUrl);
                String[] parts = uri.getPath().split("/");
                StringBuilder parentPathBuilder = new StringBuilder();
                boolean startAdding = false;
                for (String part : parts) {
                    if (part.isEmpty()) continue;
                    if ("Shared Documents".equalsIgnoreCase(part)) {
                        startAdding = true;
                        continue;
                    }
                    if (startAdding) {
                        if (parentPathBuilder.length() > 0) parentPathBuilder.append("/");
                        parentPathBuilder.append(UriUtils.encodePathSegment(part, StandardCharsets.UTF_8));
                    }
                }
                url = String.format("%s/drives/%s/root:/%s:/children", baseUrl, driveId, parentPathBuilder);
            }

            HttpEntity<String> request = new HttpEntity<>(body.toString(), getAuthHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to create folder: " + folderName + ", Response: " + response.getBody());
            }

            JSONObject responseJson = new JSONObject(response.getBody());
            return new SharePointURLDTO(
                    responseJson.getString("id"),
                    responseJson.getString("webUrl"),
                    responseJson.getString("name"),
                    driveId,
                    true,
                    false
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create folder: " + folderName + " under " + parentWebUrl, e);
        }
    }

    public SharePointURLDTO createFolderIfNotPresent(String parentFolderUrl, String[] pathSegments)
    {
        logger.info("parentFolderUrl in createFolderIfNotPresent= {}" , parentFolderUrl);

        String siteId = getSiteIdFromUrl(parentFolderUrl);

        String driveId = getDriveIdFromSiteId(siteId);

        // Always resolve parent to driveItem first
        SharePointURLDTO lastFolder = resolveFolderFromUrl(driveId, parentFolderUrl);;

        for (String raw : pathSegments) {
            String segment = (raw == null || raw.trim().isEmpty()) ? "Untitled" : raw.trim();

            //  List children by ID instead of URL
            List<SharePointURLDTO> children = listChildFoldersById(driveId, lastFolder.getId());

            Optional<SharePointURLDTO> existing = children.stream()
                    .filter(f -> f.getName().equalsIgnoreCase(segment))
                    .findFirst();

            if (existing.isPresent()) {
                lastFolder = existing.get();
            } else {
                //  Create new folder under parentId
                lastFolder = createFolderById(driveId, lastFolder.getId(), segment);
            }
        }

        return lastFolder;
    }

    //  New overload for OneDrive or arbitrary URL (fallback)
    public SharePointURLDTO resolveFolderFromUrl(String folderWebUrl) {
        try {
           logger.info("Fallback resolveFolderFromUrl by URL: {}" , folderWebUrl);

            String token = authService.getAccessToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // Encode the full URL for /shares API
            String base64Url = Base64.getUrlEncoder()
                    .encodeToString(folderWebUrl.getBytes(StandardCharsets.UTF_8));
            String apiUrl = baseUrl + "/shares/u!" + base64Url + "/driveItem";

            logger.info("API URL (fallback /shares): {}" , apiUrl);

            ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);
            Map<String, Object> body = response.getBody();
            logger.info("API response: {}" , body);

            SharePointURLDTO dto = new SharePointURLDTO();
            dto.setId((String) body.get("id"));
            dto.setName((String) body.get("name"));
            dto.setWebUrl((String) body.get("webUrl"));

            //  Capture driveId for later operations (rename/move)
            Map<String, Object> parentRef = (Map<String, Object>) body.get("parentReference");
            if (parentRef != null) {
                dto.setParentDriveId((String) parentRef.get("driveId"));
            }

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve folder by direct URL: " + folderWebUrl, e);
        }
    }

    public SharePointURLDTO resolveFolderFromUrl(String driveId, String folderWebUrl) {
    try {
        logger.info("Folder url in resolveFolderFromUrl: {}" ,folderWebUrl);
        String token = authService.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        //  Always resolve by share link encoding
        String base64Url = Base64.getUrlEncoder().encodeToString(folderWebUrl.getBytes(StandardCharsets.UTF_8));
        String apiUrl = baseUrl + "/shares/u!" + base64Url + "/driveItem";

        logger.info("API URL (resolve by webUrl): {}" , apiUrl);

        ResponseEntity<Map> response = restTemplate.exchange(apiUrl, HttpMethod.GET, entity, Map.class);
        Map<String, Object> body = response.getBody();
        logger.info("API response: {}" , body);

        SharePointURLDTO dto = new SharePointURLDTO();
        dto.setId((String) body.get("id"));
        dto.setName((String) body.get("name"));
        dto.setWebUrl((String) body.get("webUrl"));

        //new
        Map<String, Object> parentRef = (Map<String, Object>) body.get("parentReference");
        if (parentRef != null) {
            dto.setParentDriveId((String) parentRef.get("driveId"));
        }
        //new
        return dto;

    } catch (Exception e) {
        throw new RuntimeException("Failed to resolve folder: " + folderWebUrl, e);
    }
}


    // List children by parentId (safe for spaces/special chars)
    List<SharePointURLDTO> listChildFoldersById(String driveId, String parentId) {
        try {
            String url = String.format("%s/drives/%s/items/%s/children", baseUrl, driveId, parentId);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders()),
                    String.class
            );

            List<SharePointURLDTO> folders = new ArrayList<>();
            JSONArray arr = new JSONObject(response.getBody()).getJSONArray("value");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item.has("folder")) {
                    folders.add(new SharePointURLDTO(
                            item.getString("id"),
                            item.getString("webUrl"),
                            item.getString("name"),
                            driveId,
                            true,
                            false

                    ));
                }
            }
            return folders;

        } catch (Exception e) {
            throw new RuntimeException("Failed to list children for parentId=" + parentId, e);
        }
    }

    // Create folder by parentId
    private SharePointURLDTO createFolderById(String driveId, String parentId, String folderName) {
        try {
            String url = String.format("%s/drives/%s/items/%s/children", baseUrl, driveId, parentId);

            JSONObject body = new JSONObject();
            body.put("name", folderName);
            body.put("folder", new JSONObject());
            body.put("@microsoft.graph.conflictBehavior", "rename");

            HttpEntity<String> request = new HttpEntity<>(body.toString(), getAuthHeaders());
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            JSONObject json = new JSONObject(response.getBody());
            return new SharePointURLDTO(
                    json.getString("id"),
                    json.getString("webUrl"),
                    json.getString("name"),
                    driveId,
                    true,
                    false
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create folder: " + folderName + " under parentId=" + parentId, e);
        }
    }

    List<SharePointURLDTO> listChildFoldersByUrl(String driveId, String parentWebUrl) {
        try {
            String url;
            if (isSharingLink(parentWebUrl)) {
                String encodedUrl = "u!" + Base64.getUrlEncoder().encodeToString(parentWebUrl.getBytes(StandardCharsets.UTF_8));
                url = String.format("%s/shares/%s/driveItem/children", baseUrl, encodedUrl);
            } else {
                // normal path
                URI uri = URI.create(parentWebUrl);
                String[] parts = uri.getPath().split("/");
                StringBuilder parentPathBuilder = new StringBuilder();
                boolean startAdding = false;
                for (String part : parts) {
                    if (part.isEmpty()) continue;
                    if ("Shared Documents".equalsIgnoreCase(part)) {
                        startAdding = true;
                        continue;
                    }
                    if (startAdding) {
                        if (parentPathBuilder.length() > 0) parentPathBuilder.append("/");
                        parentPathBuilder.append(UriUtils.encodePathSegment(part, StandardCharsets.UTF_8));
                    }
                }
                url = String.format("%s/drives/%s/root:/%s:/children", baseUrl, driveId, parentPathBuilder);
            }

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders()),
                    String.class
            );

            List<SharePointURLDTO> folders = new ArrayList<>();
            JSONArray arr = new JSONObject(response.getBody()).getJSONArray("value");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                if (item.has("folder")) {
                    folders.add(new SharePointURLDTO(
                            item.getString("id"),
                            item.getString("webUrl"),
                            item.getString("name"),
                            driveId,
                            true,
                            false
                    ));
                }
            }
            return folders;

        } catch (Exception e) {
            throw new RuntimeException("Failed to list children for parent URL=" + parentWebUrl, e);
        }
    }

    // Create a single folder
    public SharePointURLDTO createFolder(String driveId, String parentId, String folderName) {
        try {
            String url = String.format("%s/drives/%s/items/%s/children", baseUrl, driveId, parentId);

            JSONObject body = new JSONObject();
            body.put("name", folderName);
            body.put("folder", new JSONObject()); // required!
            body.put("@microsoft.graph.conflictBehavior", "rename"); // or "fail"

            HttpEntity<String> entity = new HttpEntity<>(body.toString(), getAuthHeaders());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            JSONObject item = new JSONObject(response.getBody());
            return new SharePointURLDTO(
                    item.getString("id"),
                    item.getString("webUrl"),
                    item.getString("name"),
                    driveId,
                    true,
                    false

            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to create folder: " + folderName, e);
        }
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    public void renameFolderById(String driveId, String itemId, String newName) {
        try {
            String url = String.format("%s/drives/%s/items/%s", baseUrl, driveId, itemId);
            System.out.println("PATCH URL for rename: " + url);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(authService.getAccessToken());
            headers.setContentType(MediaType.APPLICATION_JSON);

            JSONObject body = new JSONObject();
            body.put("name", newName);

            HttpEntity<String> request = new HttpEntity<>(body.toString(), headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info(" Successfully renamed folder to: {}" , newName);
            } else {
                logger.info(" Failed to rename folder. Response: {}" , response.getBody());
                throw new RuntimeException("Failed to rename folder. HTTP Status: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to rename folder by id: " + itemId, e);
        }
    }



//    Extract site URL from full SharePoint folder URL
    String extractSiteUrl(String folderWebUrl) {
        try {
            int sitesIndex = folderWebUrl.indexOf("/sites/");
            if (sitesIndex == -1) {
                throw new RuntimeException("Invalid SharePoint URL, no '/sites/' found: " + folderWebUrl);
            }
            int nextSlash = folderWebUrl.indexOf("/", sitesIndex + 7); // 7 = length of "/sites/"
            if (nextSlash == -1) nextSlash = folderWebUrl.length();
            return folderWebUrl.substring(0, nextSlash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract site URL from: " + folderWebUrl, e);
        }
    }


public String getSiteIdFromUrl(String folderUrl) {
    try {
        logger.info("folderUrl in getSiteIdFromUrl : {}" , folderUrl);

        // Handle OneDrive sharing link or personal OneDrive URL
        if (isSharingLink(folderUrl) || folderUrl.contains("/personal/")) {
            logger.info(">>> Passing folder URL to resolveSiteIdFromSharingUrl: {}" , folderUrl);

            return resolveSiteIdFromSharingUrl(folderUrl);
        }

        // Normal SharePoint URL → parse /sites/{site} from path
        URI uri = URI.create(folderUrl);
        String host = uri.getHost();
        if (host == null) {
            throw new RuntimeException("Invalid URL, hostname cannot be determined: " + folderUrl);
        }

        String[] parts = uri.getPath().split("/");
        int siteIndex = -1;
        for (int i = 0; i < parts.length; i++) {
            if ("sites".equalsIgnoreCase(parts[i])) {
                siteIndex = i + 1;
                break;
            }
        }

        if (siteIndex == -1 || siteIndex >= parts.length) {
            throw new IllegalArgumentException("No valid '/sites/{site}' segment found in URL: " + folderUrl);
        }

        String sitePath = parts[siteIndex];
        String siteApiUrl = String.format("%s/sites/%s:/sites/%s", baseUrl, host, sitePath);

        ResponseEntity<String> siteResp = restTemplate.exchange(
                siteApiUrl,
                HttpMethod.GET,
                new HttpEntity<>(getAuthHeaders()),
                String.class
        );

        JSONObject json = new JSONObject(siteResp.getBody());
        return json.getString("id");

    } catch (Exception e) {
        throw new RuntimeException("Failed to get siteId from URL: " + folderUrl, e);
    }
}


    private String resolveSiteIdFromSharingUrl(String sharingUrl) {
        try {
            String encodedUrl = "u!" + Base64.getUrlEncoder()
                    .encodeToString(sharingUrl.getBytes(StandardCharsets.UTF_8));

            ResponseEntity<String> resp = restTemplate.exchange(
                    baseUrl + "/shares/" + encodedUrl + "/driveItem",
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders()),
                    String.class
            );

            JSONObject json = new JSONObject(resp.getBody());
            JSONObject parentRef = json.getJSONObject("parentReference");

            // 1️⃣ If siteId exists, use it
            if (parentRef.has("siteId") && !parentRef.isNull("siteId")) {
                return parentRef.getString("siteId");
            } else {
                // Otherwise, fallback: use parent driveId as "siteId" for OneDrive
                return parentRef.getString("driveId");
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve siteId from sharing URL: " + sharingUrl, e);
        }
    }

    // Simple sharing link detector
    private boolean isSharingLink(String url) {
        return url.contains("/:f:/") || url.contains("/:x:/") || url.contains("/:p:/");
    }

    private String getSiteIdFromDriveId(String driveId) {
        try {
            String url = String.format("%s/drives/%s", baseUrl, driveId);
            ResponseEntity<String> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders()),
                    String.class
            );
            JSONObject json = new JSONObject(resp.getBody());
            return json.getJSONObject("parentReference").getString("siteId");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get siteId from driveId: " + driveId, e);
        }
    }



    String getDriveIdFromSiteId(String siteId) {
        try {
            ResponseEntity<String> driveResp = restTemplate.exchange(
                    String.format("%s/sites/%s/drive", baseUrl, siteId),
                    HttpMethod.GET,
                    new HttpEntity<>(getAuthHeaders()),
                    String.class
            );
            return new JSONObject(driveResp.getBody()).getString("id");
        } catch (Exception e) {
            throw new RuntimeException("Failed to get driveId for siteId: " + siteId, e);
        }
    }

    public boolean isFolderEmpty(String folderUrl) {
        SharePointURLDTO folder = resolveFolderFromUrl(folderUrl); // your existing resolver
        String driveId = folder.getParentDriveId();
        String itemId = folder.getId();

        if (driveId == null || itemId == null) {
            throw new RuntimeException("Unable to resolve folder in SharePoint: " + folderUrl);
        }


        // Fetch both files and folders
        List<SharePointURLDTO> children = listFolderChildren(driveId, itemId);

        for (SharePointURLDTO child : children) {
            logger.info("Found item: {}, type={}", child.getName(), child.isFolder() ? "folder" : "file");

        }

        boolean empty = children.isEmpty();

        return empty;
    }


    public List<SharePointURLDTO> listFolderChildren(String driveId, String itemId) {
        List<SharePointURLDTO> children = new ArrayList<>();
        try {
            // Microsoft Graph API endpoint
            String url = String.format("%s/drives/%s/items/%s/children", baseUrl, driveId, itemId);

            HttpEntity<Void> entity = new HttpEntity<>(getAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to fetch folder children. HTTP Status: " + response.getStatusCode());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            if (root.has("value")) {
                for (JsonNode child : root.get("value")) {
                    SharePointURLDTO dto = new SharePointURLDTO();
                    dto.setId(child.get("id").asText());
                    dto.setName(child.get("name").asText());
                    dto.setWebUrl(child.get("webUrl").asText());
                    dto.setParentDriveId(driveId);
                    dto.setFolder(child.has("folder"));  // true if it's a folder
                    dto.setFile(child.has("file"));      // true if it's a file

                    children.add(dto);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to list folder children for itemId: " + itemId, e);
        }
        return children;
    }




    public void deleteFolderByUrl(String folderUrl) {
        try {
            SharePointURLDTO folder;

            // Resolve folder using existing method
            if (isSharingLink(folderUrl) || folderUrl.contains("/personal/")) {
                folder = resolveFolderFromUrl(folderUrl);
            } else {
                folder = resolveFolderFromUrl(getSiteIdFromUrl(folderUrl), folderUrl);
            }

            String driveId = folder.getParentDriveId();
            String itemId = folder.getId();

            if (driveId == null || itemId == null) {
                throw new RuntimeException("Unable to resolve folder in SharePoint: " + folderUrl);
            }

            // Delete folder without checking children (SharePoint handles it)
            deleteFolderById(driveId, itemId);

            logger.info("Deleted SharePoint folder: {}" , folderUrl);

        } catch (Exception e) {
            logger.info("Folder not found or already deleted: {}" , folderUrl );
        }
    }


    public void deleteFolderById(String driveId, String itemId) {
        try {
            String url = String.format("%s/drives/%s/items/%s", baseUrl, driveId, itemId);
            HttpEntity<Void> entity = new HttpEntity<>(getAuthHeaders());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to delete folder. HTTP Status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete folder by ID: " + itemId, e);
        }
    }

}
