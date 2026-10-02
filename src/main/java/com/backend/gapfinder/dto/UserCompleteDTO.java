package com.backend.gapfinder.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserCompleteDTO extends UserBasicDTO {

    // List of student's personal interests
    private List interests;

}