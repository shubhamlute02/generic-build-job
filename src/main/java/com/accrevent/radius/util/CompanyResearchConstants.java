package com.accrevent.radius.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CompanyResearchConstants {

//    public static final String REVENUE_100M_OR_LESS = "< 100 million";
    public static final List<String> REVENUE = Arrays.asList(
            "< 100 million",
            "100 million to 500 million",
            "500 million to 1 billion",
            "> 1 billion to 5 billion",
            "> 5 billion to 10 billion",
            "> 10 billion to 20 billion",
            "> 20 billion"
    );

    public static final List<String> EMPLOYEE_COUNT = Arrays.asList(
            "< 1000",
            "1 to 5k",
            "5 to 10k",
            "10 to 25k",
            "25 to 50k",
            "> 50k"
    );
}
