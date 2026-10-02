package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class OpenTableParticipantCompleteDTO extends OpenTableParticipantBasicDTO {

    // The open table session joined by the student
    private OpenTableBasicDTO openTable;

    // Student who joined the open table
    private UserBasicDTO user;
}