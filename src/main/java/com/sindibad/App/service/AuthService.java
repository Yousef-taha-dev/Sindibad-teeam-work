package com.sindibad.App.service;

import com.sindibad.App.dto.AuthResponseDTO;
import com.sindibad.App.dto.LoginRequestDTO;
import com.sindibad.App.dto.RegisterGuideRequestDTO;
import com.sindibad.App.dto.RegisterRequestDTO;

public interface AuthService {

    AuthResponseDTO registerTourist(RegisterRequestDTO registerDTO);

    AuthResponseDTO registerGuide(RegisterGuideRequestDTO guideDTO);

    AuthResponseDTO login(LoginRequestDTO loginDTO);
}