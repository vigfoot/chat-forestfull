package com.forestfull.domain;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String name;
    private String password;
    private String roles;
}