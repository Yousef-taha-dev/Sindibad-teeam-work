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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String EMAIL = "lina@example.com";
    private static final String RAW_PASSWORD = "password123";
    private static final String ENCODED_PASSWORD = "encoded-password";

    @Mock
    private PersonRepository personRepository;
    @Mock
    private TouristRepository touristRepository;
    @Mock
    private GuideRepository guideRepository;
    @Mock
    private AdminRepository adminRepository;
    @Mock
    private PersonMapper personMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                personRepository,
                touristRepository,
                guideRepository,
                adminRepository,
                personMapper,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void successfulTouristRegistrationSavesActivePersonAndEnglishTourist() {
        RegisterRequestDTO request = registration();
        Person mapped = mappedPerson();
        PersonResponseDTO personResponse = personResponse();
        when(personRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(personMapper.toEntity(request)).thenReturn(mapped);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person saved = invocation.getArgument(0);
            saved.setPersonId(3L);
            return saved;
        });
        when(personMapper.toDto(mapped)).thenReturn(personResponse);
        when(jwtService.generateToken(EMAIL, "TOURIST")).thenReturn("tourist-token");

        AuthResponseDTO response = authService.registerTourist(request);

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());
        Person savedPerson = personCaptor.getValue();
        assertEquals(ENCODED_PASSWORD, savedPerson.getPassword());
        assertEquals("Active", savedPerson.getAccountStatus());
        verify(passwordEncoder).encode(RAW_PASSWORD);

        ArgumentCaptor<Tourist> touristCaptor = ArgumentCaptor.forClass(Tourist.class);
        verify(touristRepository).save(touristCaptor.capture());
        Tourist savedTourist = touristCaptor.getValue();
        assertSame(savedPerson, savedTourist.getPerson());
        assertEquals("English", savedTourist.getPreferredLanguage());

        assertEquals("tourist-token", response.token());
        assertEquals("TOURIST", response.role());
        assertSame(personResponse, response.user());
        verify(guideRepository, never()).save(any());
    }

    @Test
    void successfulGuideRegistrationSavesActivePersonAndPendingGuideWithoutToken() {
        RegisterGuideRequestDTO request = new RegisterGuideRequestDTO(registration(), "Petra walks", 5);
        Person mapped = mappedPerson();
        PersonResponseDTO personResponse = personResponse();
        when(personRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(personMapper.toEntity(request.personalInfo())).thenReturn(mapped);
        when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(ENCODED_PASSWORD);
        when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
            Person saved = invocation.getArgument(0);
            saved.setPersonId(3L);
            return saved;
        });
        when(personMapper.toDto(mapped)).thenReturn(personResponse);

        AuthResponseDTO response = authService.registerGuide(request);

        ArgumentCaptor<Person> personCaptor = ArgumentCaptor.forClass(Person.class);
        verify(personRepository).save(personCaptor.capture());
        Person savedPerson = personCaptor.getValue();
        assertEquals(ENCODED_PASSWORD, savedPerson.getPassword());
        assertEquals("Active", savedPerson.getAccountStatus());
        verify(passwordEncoder).encode(RAW_PASSWORD);

        ArgumentCaptor<Guide> guideCaptor = ArgumentCaptor.forClass(Guide.class);
        verify(guideRepository).save(guideCaptor.capture());
        Guide savedGuide = guideCaptor.getValue();
        assertSame(savedPerson, savedGuide.getPerson());
        assertEquals("Petra walks", savedGuide.getBio());
        assertEquals(5, savedGuide.getExperienceYears());
        assertEquals("Pending", savedGuide.getApprovalStatus());

        assertNull(response.token());
        assertEquals("GUIDE", response.role());
        assertSame(personResponse, response.user());
        verify(jwtService, never()).generateToken(any(), any());
        verify(touristRepository, never()).save(any());
    }

    @Test
    void duplicateEmailRejectsTouristRegistrationBeforeSave() {
        when(personRepository.existsByEmail(EMAIL)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerTourist(registration())
        );

        assertEquals("Email is already in use: " + EMAIL, exception.getMessage());
        verify(personRepository, never()).save(any());
        verify(touristRepository, never()).save(any());
        verify(guideRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void duplicateEmailRejectsGuideRegistrationBeforeSave() {
        when(personRepository.existsByEmail(EMAIL)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.registerGuide(new RegisterGuideRequestDTO(registration(), "Petra walks", 5))
        );

        assertEquals("Email is already in use: " + EMAIL, exception.getMessage());
        verify(personRepository, never()).save(any());
        verify(touristRepository, never()).save(any());
        verify(guideRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void activeTouristLogsInWithGeneratedToken() {
        Person person = activePerson();
        PersonResponseDTO personResponse = personResponse();
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(touristRepository.existsByPersonPersonId(3L)).thenReturn(true);
        when(personMapper.toDto(person)).thenReturn(personResponse);
        when(jwtService.generateToken(EMAIL, "TOURIST")).thenReturn("tourist-token");

        AuthResponseDTO response = authService.login(new LoginRequestDTO(EMAIL, RAW_PASSWORD));

        verify(passwordEncoder).matches(RAW_PASSWORD, ENCODED_PASSWORD);
        assertEquals("tourist-token", response.token());
        assertEquals("TOURIST", response.role());
        assertSame(personResponse, response.user());
        verify(personRepository, never()).save(any());
        verify(touristRepository, never()).save(any());
        verify(guideRepository, never()).save(any());
    }

    @Test
    void unknownEmailIsRejected() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequestDTO(EMAIL, RAW_PASSWORD))
        );

        assertEquals("Invalid email or password.", exception.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtService, never()).generateToken(any(), any());
        verifyNothingSaved();
    }

    @Test
    void incorrectPasswordIsRejected() {
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(activePerson()));
        when(passwordEncoder.matches("wrong-password", ENCODED_PASSWORD)).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequestDTO(EMAIL, "wrong-password"))
        );

        assertEquals("Invalid email or password.", exception.getMessage());
        verify(passwordEncoder).matches("wrong-password", ENCODED_PASSWORD);
        verify(jwtService, never()).generateToken(any(), any());
        verifyNothingSaved();
    }

    @Test
    void inactiveAccountIsRejected() {
        Person person = activePerson();
        person.setAccountStatus("Inactive");
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequestDTO(EMAIL, RAW_PASSWORD))
        );

        assertEquals("Your account is not active.", exception.getMessage());
        verify(jwtService, never()).generateToken(any(), any());
        verifyNothingSaved();
    }

    @Test
    void unapprovedGuideIsRejected() {
        Person person = activePerson();
        Guide guide = new Guide();
        guide.setPerson(person);
        guide.setApprovalStatus("Pending");
        when(personRepository.findByEmail(EMAIL)).thenReturn(Optional.of(person));
        when(passwordEncoder.matches(RAW_PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
        when(guideRepository.findByPersonPersonId(3L)).thenReturn(Optional.of(guide));

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(new LoginRequestDTO(EMAIL, RAW_PASSWORD))
        );

        assertEquals("Your guide account is not approved yet.", exception.getMessage());
        verify(jwtService, never()).generateToken(any(), any());
        verifyNothingSaved();
    }

    private void verifyNothingSaved() {
        verify(personRepository, never()).save(any());
        verify(touristRepository, never()).save(any());
        verify(guideRepository, never()).save(any());
    }

    private RegisterRequestDTO registration() {
        return new RegisterRequestDTO(
                "Lina",
                "Haddad",
                EMAIL,
                RAW_PASSWORD,
                "0790000000",
                "Female",
                null,
                null
        );
    }

    private Person mappedPerson() {
        Person person = new Person();
        person.setEmail(EMAIL);
        person.setPassword(RAW_PASSWORD);
        person.setAccountStatus("Inactive");
        return person;
    }

    private Person activePerson() {
        Person person = new Person();
        person.setPersonId(3L);
        person.setEmail(EMAIL);
        person.setPassword(ENCODED_PASSWORD);
        person.setAccountStatus("Active");
        return person;
    }

    private PersonResponseDTO personResponse() {
        return new PersonResponseDTO(
                3L,
                "Lina",
                "Haddad",
                EMAIL,
                "0790000000",
                "Female",
                null,
                null,
                null,
                "Active"
        );
    }
}
