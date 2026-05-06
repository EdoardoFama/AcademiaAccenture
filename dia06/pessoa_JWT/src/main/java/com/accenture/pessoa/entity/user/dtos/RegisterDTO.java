package com.accenture.pessoa.entity.user.dtos;

import com.accenture.pessoa.entity.enums.UserRoles;

public record RegisterDTO(String login, String password, UserRoles role) {
}
