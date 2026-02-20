package com.accrevent.radius.controller;

import com.accrevent.radius.dto.ProgressReportForBussinessUnitDTO;
import com.accrevent.radius.service.ProgressReportForBussinessUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProgressReportForBussinessUnitController {

    @Autowired
    private ProgressReportForBussinessUnitService progressReportForBussinessUnitService;

    @GetMapping("/getProgressReportForBU")
    public ProgressReportForBussinessUnitDTO getProgressReportForBU(@RequestParam String campaignId){
        return progressReportForBussinessUnitService.getProgressReportForBU(campaignId);

    }

}
