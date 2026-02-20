package com.accrevent.radius.service;

import com.accrevent.radius.util.BusinessUnitConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class BusinessUnitService {
    @Autowired
    private BusinessUnitConstants businessUnitConstants;

    public List<String> getAllBusinessUnits() {
        return new ArrayList<>(businessUnitConstants.BUSINESS_UNITS);
    }
}



