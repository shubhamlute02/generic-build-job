package com.accrevent.radius.service;

import com.accrevent.radius.Serializable.UserCampaignCommentTimeTrack;
import com.accrevent.radius.model.CampaignCommentTrack;
import com.accrevent.radius.repository.CampaignCommentTrackRepository;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.util.Optional;

@Service
public class CampaignCommentTrackService {

    private final CampaignCommentTrackRepository campaignCommentTrackRepository;

    public CampaignCommentTrackService(CampaignCommentTrackRepository campaignCommentTrackRepository) {
        this.campaignCommentTrackRepository = campaignCommentTrackRepository;
    }


    public CampaignCommentTrack createOrUpdateCampaignCommentTrack(CampaignCommentTrack campaignCommentTrack) {
        if (campaignCommentTrackRepository.existsById(new UserCampaignCommentTimeTrack(campaignCommentTrack.getUserId(), campaignCommentTrack.getCampaignId()))) {
            // Update the existing record
            campaignCommentTrack.setDateTime(ZonedDateTime.now());
            CampaignCommentTrack savedCampaign = campaignCommentTrackRepository.save(campaignCommentTrack);
            return savedCampaign;
        }
        else
        {
            // Create a new record if it doesn't exist
            campaignCommentTrack.setDateTime(ZonedDateTime.now());
            return campaignCommentTrackRepository.save(campaignCommentTrack);
        }
    }

    public CampaignCommentTrack getUserCampaignCommentTrack(String userId, Long campaignId) {
        Optional<CampaignCommentTrack> campaignCommentTrack = campaignCommentTrackRepository.findById(new UserCampaignCommentTimeTrack(userId, campaignId));
        if(campaignCommentTrack.isPresent()) {
            return campaignCommentTrack.get();
        }
        return null;
    }


}
