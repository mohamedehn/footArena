package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.UserDTO;
import com.footArena.booking.domain.entities.User;

public class UserMapper {
    public static UserDTO MappedUserToDto(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setFirstName(user.getFirstName());
        userDTO.setLastName(user.getLastName());
        userDTO.setEmail(user.getEmail());
        userDTO.setRole(user.getRole());
        userDTO.setEnabled(user.isEnabled());
        return userDTO;
    }

    public static User MappedUserToEntity(UserDTO userDTO) {
        User user = new User();
        user.setId(userDTO.getId());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setEmail(userDTO.getEmail());
        user.setRole(userDTO.getRole());
        user.setEnabled(userDTO.getEnabled());
        return user;
    }

}