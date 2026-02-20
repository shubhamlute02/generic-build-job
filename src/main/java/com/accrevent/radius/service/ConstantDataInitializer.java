package com.accrevent.radius.service;

//import com.accrevent.radius.model.ConstantBusinessUnit;
import com.accrevent.radius.model.ConstantLifecycle;
import com.accrevent.radius.model.EditLifecycle;
import com.accrevent.radius.model.SelectedLifecycle;
//import com.accrevent.radius.repository.ConstantBusinessUnitRepository;
import com.accrevent.radius.repository.ConstantLifecycleRepository;
import com.accrevent.radius.repository.EditLifecycleRepository;
import com.accrevent.radius.repository.SelectedLifecycleRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ConstantDataInitializer {
    private final ConstantLifecycleRepository constantLifecycleRepository;
//    private final ConstantBusinessUnitRepository constantBusinessUnitRepository;
    @Autowired
    private SelectedLifecycleRepository repository;
    public ConstantDataInitializer(ConstantLifecycleRepository constantLifecycleRepository)
    {
        this.constantLifecycleRepository = constantLifecycleRepository;
//        this.constantBusinessUnitRepository = constantBusinessUnitRepository;
    }
    @PostConstruct
    public void initializeConstants() {

        if(constantLifecycleRepository.count() == 0)
        {
            //campaign
            initializeData(1L,"Backlog");
            initializeData(1L,"Preparation");
            initializeData(1L,"Active");
            initializeData(1L,"Closing");
            initializeData(1L,"OnHold");
            initializeData(1L,"Cancelled");
            initializeData(1L,"Completed");

            //lead
            initializeData(2L,"Identified");
            initializeData(2L,"Research");
            initializeData(2L,"Prospecting");
            initializeData(2L,"Disqualified");
            initializeData(2L,"Opportunity Created");

            //opportunity
            initializeData(3L,"Discovery");
            initializeData(3L,"Proposal");
            initializeData(3L,"Customer Evaluating");
            initializeData(3L,"Negotiating");
            initializeData(3L,"Closed Won");
            initializeData(3L,"Closed Lost");

            initializeData(8L,"Seeking Approval");
            initializeData(8L,"Hiring In Progress");
            initializeData(8L,"Closed");
            initializeData(8L,"On Hold");
            initializeData(8L,"Cancelled");

            initializeData(9L,"Reviewing Profile");
            initializeData(9L,"Profile Rejected");
            initializeData(9L,"Scheduling Interview");
            initializeData(9L,"Interview1");
            initializeData(9L,"Interview2");
            initializeData(9L,"Interview3");
            initializeData(9L,"Interview4");
            initializeData(9L,"Selected");
            initializeData(9L,"Rejected");

            //outreach campaign
            initializeData(4L,"Not Started");
            initializeData(4L,"In Progress");
            initializeData(4L,"Completed");

            //Email outreach task
            initializeData(5L,"Not Started");
            initializeData(5L,"Intro");
            initializeData(5L,"Follow Up 1");
            initializeData(5L,"Follow Up 2");
            initializeData(5L,"Follow Up 3");
            initializeData(5L,"Closure");
            initializeData(5L,"Meeting");
            initializeData(5L,"Completed");

            //task
            initializeData(6L,"Not Started");
            initializeData(6L,"In Work");
            initializeData(6L,"Completed");

            //version
            initializeData(7L,"Planning");
            initializeData(7L,"Ideation & Research");
            initializeData(7L,"Creation");
            initializeData(7L,"Review");
            initializeData(7L,"Approved");

            //linkedin outreach task
            initializeData(10L,"Not Started");
            initializeData(10L,"Connect");
            initializeData(10L,"Waiting For Acceptance");
            initializeData(10L,"Intro");
            initializeData(10L,"Follow Up 1");
            initializeData(10L,"Follow Up 2");
            initializeData(10L,"Follow Up 3");
            initializeData(10L,"Closure");
            initializeData(10L,"Meeting");
            initializeData(10L,"Completed");

            //phone outreach task
            initializeData(11L,"Not Started");
            initializeData(11L,"Calling");
            initializeData(11L,"Stopped");
            initializeData(11L,"Meeting");
            initializeData(11L,"Completed");

        }

        if (editLifecycleRepository.count() == 0) {
            initializeDefaultLifecycles();
        }

        if (!repository.existsById(1L)) {
            SelectedLifecycle selected = new SelectedLifecycle();
            selected.setId(1L);
            selected.setCampaign(1L);       // Default campaign lifecycle ID
            selected.setOpportunity(3L);    // Default opportunity lifecycle ID
            selected.setLead(2L);
            selected.setRequirement(8L);
            selected.setStaffSelectionProcess(9L);// Default lead lifecycle ID
            repository.save(selected);
            System.out.println("Default SelectedLifecycle initialized.");
        }

    }
    public void initializeData (Long cycleId,String cycleName)
    {
        ConstantLifecycle lifecycle = new ConstantLifecycle();
        lifecycle.setCycleId(cycleId);
        lifecycle.setCycleName(cycleName);
        constantLifecycleRepository.save(lifecycle);
    }


    @Autowired
    private EditLifecycleRepository editLifecycleRepository;

    // Method to initialize the default lifecycles
    public void initializeDefaultLifecycles() {
        initializeLifecycle("default1", new String[]{"backlog", "preparation", "active", "closing", "onHold", "cancelled", "complete"});
        initializeLifecycle("default2", new String[]{"identified", "research", "prospecting", "disqualified", "Opportunity Created"});
        initializeLifecycle("default3", new String[]{"discovery", "proposal", "customer evaluating", "closing", "closed won", "closed lost"});
        initializeLifecycle("default4", new String[]{"Seeking Approval", "Hiring In Progress", "Closed", "On Hold", "Cancelled"});
        initializeLifecycle("default5", new String[]{"Reviewing Profile", "Profile Rejected", "Scheduling Interview", "Interview1", "Interview2", "Interview3", "Interview4", "Selected", "Rejected"});

    }

    // Method to save a lifecycle with given name and states
    public void initializeLifecycle(String lifecycleName, String[] states) {
        EditLifecycle lifecycle = new EditLifecycle();
        lifecycle.setLifecycleName(lifecycleName);
        lifecycle.setLifecycleStates(List.of(states));
        editLifecycleRepository.save(lifecycle);
    }
}
