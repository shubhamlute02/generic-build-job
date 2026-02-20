package com.accrevent.radius.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
@Component
public class BusinessUnitConstants {
    public static final String PLM_WINDCHILL = "PLM Windchill" ;
    public static final String BANKING = "Banking" ;
    public static final String PLM_ARAS = "PLM Aras" ;
    public static final String IOT = "IOT" ;
    public static final String ENTERPRISE_APPS = "Enterprise Apps" ;
    public static final String QA_SRUM = "QA Scrum" ;


    public static final List<String> BUSINESS_UNITS = Arrays.asList(
            PLM_WINDCHILL,
            PLM_ARAS,
            IOT,
            ENTERPRISE_APPS,
            BANKING,
            QA_SRUM
    );

}



