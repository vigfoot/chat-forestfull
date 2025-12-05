package com.forestfull.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class TikTokController {

    private final TikTokProperties props;

}