package com.forestfull.member;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * com.forestfull.member
 *
 * @author vigfoot
 * @version 2025-11-25
 */
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;



}
