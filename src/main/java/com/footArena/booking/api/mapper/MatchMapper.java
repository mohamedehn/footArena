package com.footArena.booking.api.mapper;

import com.footArena.booking.api.dto.MatchDTO;
import com.footArena.booking.domain.model.entity.Match;

public class MatchMapper {
    public static MatchDTO MappedMatchToDto(Match match) {
        MatchDTO matchDTO = new MatchDTO();
        matchDTO.setId(match.getId());
        matchDTO.setFieldId(match.getField().getId());
        matchDTO.setType(match.getType());
        matchDTO.setLevel(match.getLevel());
        matchDTO.setPublic(match.isPublic());
        matchDTO.setFull(match.isFull());
        matchDTO.setMatchStatus(match.getMatchStatus());
        matchDTO.setDescription(match.getDescription());
        matchDTO.setStartDateTime(match.getStartDateTime());
        matchDTO.setEndDateTime(match.getEndDateTime());
        return matchDTO;
    }

    public static Match MappedMatchToEntity(MatchDTO matchDTO) {
        Match match = new Match();
        match.setId(matchDTO.getId());
        // Assuming Field is mapped elsewhere -- TODO with service or repository
        match.setField(null); // Placeholder, should be set with actual Field entity
        match.setType(matchDTO.getType());
        match.setLevel(matchDTO.getLevel());
        match.setPublic(matchDTO.isPublic());
        match.setFull(matchDTO.isFull());
        match.setMatchStatus(matchDTO.getMatchStatus());
        match.setDescription(matchDTO.getDescription());
        match.setStartDateTime(matchDTO.getStartDateTime());
        match.setEndDateTime(matchDTO.getEndDateTime());
        return match;
    }
}
