package com.volta.ranking.usecase;

import com.volta.ranking.dto.auth.LoginRequestDTO;
import com.volta.ranking.dto.auth.TokenResponseDTO;

public interface AuthUseCase {

    TokenResponseDTO login(LoginRequestDTO dto);
}
