package com.accrevent.radius.service;

import com.accrevent.radius.dto.UserRegionDTO;
import com.accrevent.radius.model.UserRegion;
import com.accrevent.radius.repository.UserRegionRepository;
import org.springframework.stereotype.Service;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


@Service
public class UserRegionService {
    private final UserRegionRepository userRegionRepository;
    public UserRegionService(UserRegionRepository userRegionRepository)
    {
        this.userRegionRepository = userRegionRepository;
    }

    public UserRegionDTO createUserRegion(UserRegionDTO userRegionDTO)
    {
        UserRegion userRegion = transformToUserRegion(userRegionDTO);
        if(userRegion.getUserRegionId() == null) {
            if (userRegionRepository.existsByUserId(userRegion.getUserId())) {
                throw new IllegalArgumentException("UserRegion with the same name already exists.");
            }
        }
        return transformToUserRegionDTO(userRegionRepository.save(userRegion));
    }

    public List<UserRegionDTO> getAllUserRegion()
    {
        List<UserRegionDTO> userRegionDTOList = new ArrayList<>();
        List<UserRegion> allUserRegions = userRegionRepository.findAll();
        allUserRegions.forEach(userRegion -> {
            userRegionDTOList.add(transformToUserRegionDTO(userRegion));
        });
        return userRegionDTOList;
    }

    public Optional<UserRegion> getUserRegionById(Long id)
    {
        return userRegionRepository.findById(id);
    }

    public boolean deleteUserRegion(Long id)
    {
        if(userRegionRepository.existsById(id))
        {
            userRegionRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<UserRegion> getUserListByRegion(String region){
        return userRegionRepository.findByRegion(region);
    }

    private UserRegion transformToUserRegion(UserRegionDTO userRegionDTO)
    {
        UserRegion userRegion = new UserRegion();
        if(userRegionDTO.getUserRegionId()!= null) {
            userRegion.setUserRegionId(userRegionDTO.getUserRegionId());
        }
        userRegion.setUserId(userRegionDTO.getUserId());
        userRegion.setRegion(userRegionDTO.getRegion());
        return userRegion;
    }
    private UserRegionDTO transformToUserRegionDTO(UserRegion userRegion)
    {
        UserRegionDTO userRegionDTO = new UserRegionDTO();
        userRegionDTO.setUserRegionId(userRegion.getUserRegionId());
        userRegionDTO.setUserId(userRegion.getUserId());
        userRegionDTO.setRegion(userRegion.getRegion());
        return userRegionDTO;
    }
    

    
}
