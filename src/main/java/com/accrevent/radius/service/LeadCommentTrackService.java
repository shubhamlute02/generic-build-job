package com.accrevent.radius.service;

import com.accrevent.radius.Serializable.UserLeadCommentTimeTrack;
import com.accrevent.radius.model.LeadCommentTrack;
import com.accrevent.radius.repository.LeadCommentTrackRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class LeadCommentTrackService {

    private final LeadCommentTrackRepository leadCommentTrackRepository;

    public LeadCommentTrackService(LeadCommentTrackRepository leadCommentTrackRepository) {
        this.leadCommentTrackRepository = leadCommentTrackRepository;
    }


    public LeadCommentTrack createOrUpdateLeadCommentTrack(LeadCommentTrack leadCommentTrack) {
        if (leadCommentTrackRepository.existsById(new UserLeadCommentTimeTrack(leadCommentTrack.getUserId(), leadCommentTrack.getLeadId()))) {
            // Update the existing record
            leadCommentTrack.setDateTime(ZonedDateTime.now());
            return leadCommentTrackRepository.save(leadCommentTrack);
        }
        else
        {
            // Create a new record if it doesn't exist
            leadCommentTrack.setDateTime(ZonedDateTime.now());
            return leadCommentTrackRepository.save(leadCommentTrack);
        }
    }

    public LeadCommentTrack getUserLeadCommentTrack(String userId, Long leadId) {
        Optional<LeadCommentTrack> leadCommentTrack = leadCommentTrackRepository.findById(new UserLeadCommentTimeTrack(userId, leadId));
        if(leadCommentTrack.isPresent()) {
            return leadCommentTrack.get();
        }
        return null;
    }


}
