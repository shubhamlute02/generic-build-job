package com.accrevent.radius.service;

import com.accrevent.radius.model.Bookmark;
import com.accrevent.radius.repository.BookmarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BookmarkService {

    @Autowired
    private BookmarkRepository bookmarkRepository;

    public void saveBookmarkForLead(String userName, Long leadId, Boolean isBookmarked) {
        Bookmark bookmark = bookmarkRepository.findByUserNameAndLeadId(userName, leadId)
                .orElse(new Bookmark());
        bookmark.setUserName(userName);
        bookmark.setLeadId(leadId);
        bookmark.setOpportunityId(null);
        bookmark.setIsBookmarked(isBookmarked);
        bookmarkRepository.save(bookmark);
    }

    public void saveBookmarkForOpportunity(String userName, Long opportunityId, Boolean isBookmarked) {
        Bookmark bookmark = bookmarkRepository.findByUserNameAndOpportunityId(userName, opportunityId)
                .orElse(new Bookmark());
        bookmark.setUserName(userName);
        bookmark.setOpportunityId(opportunityId);
        bookmark.setLeadId(null);
        bookmark.setIsBookmarked(isBookmarked);
        bookmarkRepository.save(bookmark);
    }

    public Boolean getBookmarkForLead(String userName, Long leadId) {
        return bookmarkRepository.findByUserNameAndLeadId(userName, leadId)
                .map(Bookmark::getIsBookmarked)
                .orElse(false);
    }

    public Boolean getBookmarkForOpportunity(String userName, Long opportunityId) {
        return bookmarkRepository.findByUserNameAndOpportunityId(userName, opportunityId)
                .map(Bookmark::getIsBookmarked)
                .orElse(false);
    }
}

