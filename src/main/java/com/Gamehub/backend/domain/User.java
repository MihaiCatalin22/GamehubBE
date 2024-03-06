package com.Gamehub.backend.domain;

import java.util.HashSet;
import java.util.Set;
import lombok.*;
@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    private Long id;
    private String username;
    private String email;
    private String password;
}

