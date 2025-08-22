package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMember_MemIdx(Long memIdx);
}
