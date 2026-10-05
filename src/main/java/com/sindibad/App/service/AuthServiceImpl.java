package com.sindibad.App.service;

import com.sindibad.App.dto.AuthResponseDTO;
import com.sindibad.App.dto.LoginRequestDTO;
import com.sindibad.App.dto.PersonResponseDTO;
import com.sindibad.App.dto.RegisterGuideRequestDTO;
import com.sindibad.App.dto.RegisterRequestDTO;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.exception.ResourceNotFoundException;
import com.sindibad.App.mapper.PersonMapper;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final PersonRepository personRepository;
    private final TouristRepository touristRepository;
    private final GuideRepository guideRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(PersonRepository personRepository,
                           TouristRepository touristRepository,
                           GuideRepository guideRepository,
                           PersonMapper personMapper,
                           PasswordEncoder passwordEncoder) {

        this.personRepository = personRepository;
        this.touristRepository = touristRepository;
        this.guideRepository = guideRepository;
        this.personMapper = personMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public AuthResponseDTO registerTourist(RegisterRequestDTO registerDTO) {

        if (personRepository.existsByEmail(registerDTO.email())) {
            throw new IllegalArgumentException(
                    "Email is already in use: " + registerDTO.email()
            );
        }

        Person person = personMapper.toEntity(registerDTO);
        person.setPassword(passwordEncoder.encode(registerDTO.password()));
        person.setAccountStatus("Active");

        Person savedPerson = personRepository.save(person);

        Tourist tourist = new Tourist();
        tourist.setPerson(savedPerson);
        tourist.setPreferredLanguage("English");
        touristRepository.save(tourist);

        PersonResponseDTO personResponseDTO = personMapper.toDto(savedPerson);

        // قم باستبدال "dummy-jwt-token" بمولد الـ Token الخاص بك (JwtUtils) عند تفعيله
        return new AuthResponseDTO("dummy-jwt-token", "TOURIST", personResponseDTO);
    }

    @Override
    @Transactional
    public AuthResponseDTO registerGuide(RegisterGuideRequestDTO guideDTO) {

        RegisterRequestDTO personalInfo = guideDTO.personalInfo();

        if (personRepository.existsByEmail(personalInfo.email())) {
            throw new IllegalArgumentException(
                    "Email is already in use: " + personalInfo.email()
            );
        }

        Person person = personMapper.toEntity(personalInfo);
        person.setPassword(passwordEncoder.encode(personalInfo.password()));
        person.setAccountStatus("Active");

        Person savedPerson = personRepository.save(person);

        Guide guide = new Guide();
        guide.setPerson(savedPerson);
        guide.setBio(guideDTO.bio());
        guide.setExperienceYears(guideDTO.experienceYears());
        guide.setApprovalStatus("Pending");
        guideRepository.save(guide);

        PersonResponseDTO personResponseDTO = personMapper.toDto(savedPerson);

        return new AuthResponseDTO("dummy-jwt-token", "GUIDE", personResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO loginDTO) {

        Person person = personRepository.findByEmail(loginDTO.email())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Invalid email or password.")
                );

        if (!passwordEncoder.matches(loginDTO.password(), person.getPassword())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        String role;
        if (touristRepository.existsByPersonPersonId(person.getPersonId())) {
            role = "TOURIST";
        } else if (guideRepository.existsByPersonPersonId(person.getPersonId())) {
            role = "GUIDE";
        } else {
            role = "USER";
        }

        PersonResponseDTO personResponseDTO = personMapper.toDto(person);

        return new AuthResponseDTO("dummy-jwt-token", role, personResponseDTO);
    }
}