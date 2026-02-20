package com.accrevent.radius.util;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class MarketingContentConstants {

    public static final List<String> ContentType = Arrays.asList(
            "LinkedIn Post",
            "PPT Slide",
            "Flyer",
            "Video",
            "Email Sequence",
            "Whitepaper",
            "Blog",
            "Podcast"
    );


    public static final List<String> ContentConsumers = Arrays.asList(
            "CXO",
            "Department Head",
            "User"
    );
}
