
package com.sindibad.App.service;

import com.sindibad.App.dto.AuthResponseDTO;
import com.sindibad.App.dto.LoginRequestDTO;
import com.sindibad.App.dto.PersonResponseDTO;
import com.sindibad.App.dto.RegisterGuideRequestDTO;
import com.sindibad.App.dto.RegisterRequestDTO;
import com.sindibad.App.entity.Guide;
import com.sindibad.App.entity.Person;
import com.sindibad.App.entity.Tourist;
import com.sindibad.App.mapper.PersonMapper;
import com.sindibad.App.repository.AdminRepository;
import com.sindibad.App.repository.GuideRepository;
import com.sindibad.App.repository.PersonRepository;
import com.sindibad.App.repository.TouristRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final PersonRepository personRepository;
    private final TouristRepository touristRepository;
    private final GuideRepository guideRepository;
    private final AdminRepository adminRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            PersonRepository personRepository,
            TouristRepository touristRepository,
            GuideRepository guideRepository,
            AdminRepository adminRepository,
            PersonMapper personMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.personRepository = personRepository;
        this.touristRepository = touristRepository;
        this.guideRepository = guideRepository;
        this.adminRepository = adminRepository;
        this.personMapper = personMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ==================== Register Tourist ====================

    @Override
    @Transactional
    public AuthResponseDTO registerTourist(
            RegisterRequestDTO registerDTO) {

        if (personRepository.existsByEmail(registerDTO.email())) {
            throw new IllegalArgumentException(
                    "Email is already in use: " + registerDTO.email()
            );
        }

        Person person = personMapper.toEntity(registerDTO);

        // Hash password using BCrypt
        person.setPassword(
                passwordEncoder.encode(registerDTO.password())
        );

        person.setAccountStatus("Active");

        Person savedPerson = personRepository.save(person);

        // Create Tourist account
        Tourist tourist = new Tourist();
        tourist.setPerson(savedPerson);
        tourist.setPreferredLanguage("English");

        touristRepository.save(tourist);

        PersonResponseDTO personResponseDTO =
                personMapper.toDto(savedPerson);

        // Generate JWT
        String token = jwtService.generateToken(
                savedPerson.getEmail(),
                "TOURIST"
        );

        return new AuthResponseDTO(
                token,
                "TOURIST",
                personResponseDTO
        );
    }

    // ==================== Register Guide ====================

    @Override
    @Transactional
    public AuthResponseDTO registerGuide(
            RegisterGuideRequestDTO guideDTO) {

        RegisterRequestDTO personalInfo =
                guideDTO.personalInfo();

        if (personRepository.existsByEmail(personalInfo.email())) {
            throw new IllegalArgumentException(
                    "Email is already in use: " + personalInfo.email()
            );
        }

        Person person = personMapper.toEntity(personalInfo);

        // Hash password using BCrypt
        person.setPassword(
                passwordEncoder.encode(personalInfo.password())
        );

        person.setAccountStatus("Active");

        Person savedPerson = personRepository.save(person);

        // Create Guide account
        Guide guide = new Guide();

        guide.setPerson(savedPerson);
        guide.setBio(guideDTO.bio());
        guide.setExperienceYears(guideDTO.experienceYears());

        // Wait for admin approval
        guide.setApprovalStatus("Pending");

        guideRepository.save(guide);

        PersonResponseDTO personResponseDTO =
                personMapper.toDto(savedPerson);

        // No JWT before admin approval
        return new AuthResponseDTO(
                null,
                "GUIDE",
                personResponseDTO
        );
    }

    // ==================== Login ====================

    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO loginDTO) {

        Person person = personRepository
                .findByEmail(loginDTO.email())
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid email or password."
                        )
                );

        // Check password using BCrypt
        if (!passwordEncoder.matches(
                loginDTO.password(),
                person.getPassword())) {

            throw new BadCredentialsException(
                    "Invalid email or password."
            );
        }

        // Check account status
        if (!"Active".equalsIgnoreCase(person.getAccountStatus())) {
            throw new BadCredentialsException(
                    "Your account is not active."
            );
        }

        String role;

        // Check Admin first
        if (adminRepository.existsByPersonPersonId(
                person.getPersonId())) {

            role = "ADMIN";

            // Check Tourist
        } else if (touristRepository.existsByPersonPersonId(
                person.getPersonId())) {

            role = "TOURIST";

            // Check Guide
        } else {

            Guide guide = guideRepository
                    .findByPersonPersonId(person.getPersonId())
                    .orElseThrow(() ->
                            new BadCredentialsException(
                                    "Invalid email or password."
                            )
                    );

            // Guide must be approved by admin
            if (!"Approved".equalsIgnoreCase(
                    guide.getApprovalStatus())) {

                throw new BadCredentialsException(
                        "Your guide account is not approved yet."
                );
            }

            role = "GUIDE";
        }

        PersonResponseDTO personResponseDTO =
                personMapper.toDto(person);

        // Generate JWT
        String token = jwtService.generateToken(
                person.getEmail(),
                role
        );

        return new AuthResponseDTO(
                token,
                role,
                personResponseDTO
        );
    }
}
