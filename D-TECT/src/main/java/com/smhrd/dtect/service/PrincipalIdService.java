package com.smhrd.dtect.service;

import com.smhrd.dtect.entity.Expert;
import com.smhrd.dtect.entity.User;
import com.smhrd.dtect.repository.ExpertRepository;
import com.smhrd.dtect.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PrincipalIdService {

    private final UserRepository userRepository;
    private final ExpertRepository expertRepository;

    public Optional<Long> findUserIdByMemIdx(Long memIdx) {
        return userRepository.findByMember_MemIdx(memIdx).map(User::getUserIdx);
    }

    public Optional<Long> findExpertIdByMemIdx(Long memIdx) {
        return expertRepository.findByMember_MemIdx(memIdx).map(Expert::getExpertIdx);
    }
}
