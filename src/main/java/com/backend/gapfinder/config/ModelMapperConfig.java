package com.backend.gapfinder.config;

import com.backend.gapfinder.dto.GapBasicDTO;
import com.backend.gapfinder.models.GapModel;
import com.backend.gapfinder.models.UserModel;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Spring configuration class for custom bean definitions.
@Configuration
public class ModelMapperConfig {

    // Configures and provides a singleton ModelMapper bean for object mapping across the application.
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();

        // Custom mapping configuration from GapBasicDTO to GapModel.
        mapper.emptyTypeMap(GapBasicDTO.class, GapModel.class)
            .addMappings(m -> {
                // Explicit field-to-field mappings for basic attributes.
                m.map(GapBasicDTO::getId, GapModel::setId);
                m.map(GapBasicDTO::getStartTime, GapModel::setStartTime);
                m.map(GapBasicDTO::getEndTime, GapModel::setEndTime);
            })
            .setPostConverter(ctx -> {
                // Custom post-conversion logic to map the userId into a UserModel reference.
                Long userId = ctx.getSource().getUserId();
                if (userId != null) {
                    UserModel user = new UserModel();
                    user.setId(userId);
                    ctx.getDestination().setUser(user);
                }
                return ctx.getDestination();
            });

        return mapper;
    }
}