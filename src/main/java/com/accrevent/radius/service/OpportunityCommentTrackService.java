package com.accrevent.radius.service;

import com.accrevent.radius.Serializable.UserOpportunityCommentTimeTrack;
import com.accrevent.radius.model.OpportunityCommentTrack;
import com.accrevent.radius.repository.OpportunityCommentTrackRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class OpportunityCommentTrackService {

    private final OpportunityCommentTrackRepository opportunityCommentTrackRepository;

    public OpportunityCommentTrackService(OpportunityCommentTrackRepository opportunityCommentTrackRepository) {
        this.opportunityCommentTrackRepository = opportunityCommentTrackRepository;
    }


    public OpportunityCommentTrack createOrUpdateOpportunityCommentTrack(OpportunityCommentTrack opportunityCommentTrack) {
        if (opportunityCommentTrackRepository.existsById(new UserOpportunityCommentTimeTrack(opportunityCommentTrack.getUserId(), opportunityCommentTrack.getOpportunityId()))) {
            // Update the existing record
            opportunityCommentTrack.setDateTime(ZonedDateTime.now());
            OpportunityCommentTrack savedOpportunity = opportunityCommentTrackRepository.save(opportunityCommentTrack);
            return savedOpportunity;
        }
        else
        {
            // Create a new record if it doesn't exist
            opportunityCommentTrack.setDateTime(ZonedDateTime.now());
            return opportunityCommentTrackRepository.save(opportunityCommentTrack);
        }
    }

    public OpportunityCommentTrack getUserOpportunityCommentTrack(String userId, Long opportunityId) {
        Optional<OpportunityCommentTrack> opportunityCommentTrack = opportunityCommentTrackRepository.findById(new UserOpportunityCommentTimeTrack(userId, opportunityId));
        if(opportunityCommentTrack.isPresent()) {
            return opportunityCommentTrack.get();
        }
        return null;
    }


}
