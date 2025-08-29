package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMember_MemIdx(Long memIdx);
    
    @Query("select m.username from User u join u.member m where u.userIdx = :userId")
    Optional<String> findMemberUsernameByUserId(@Param("userId") Long userId);
    
}
