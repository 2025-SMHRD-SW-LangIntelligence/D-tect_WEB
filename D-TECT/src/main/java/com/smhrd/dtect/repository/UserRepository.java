package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 기존 서비스/로직에서 광범위 사용: 유지 */
    Optional<User> findByMember_MemIdx(Long memIdx);

    /** 표준 쿼리 (Member → User) */
    @Query("select u from User u where u.member.memIdx = :memIdx")
    Optional<User> findByMemberId(@Param("memIdx") Long memIdx);

    /** user_idx → username (세션 sid 복원 등에서 사용) */
    @Query("select m.username from User u join u.member m where u.userIdx = :userId")
    Optional<String> findMemberUsernameByUserId(@Param("userId") Long userId);

    // (선택) 필요 시 아래 메서드들도 유지 가능
    @Query("select u from User u join u.member m where m.username = :username")
    Optional<User> findByMemberUsername(@Param("username") String username);

    @Query("select u from User u join u.member m where lower(m.username) = lower(:username)")
    Optional<User> findByMemberUsernameIgnoreCase(@Param("username") String username);

    @Query("""
           select u from User u join u.member m
           where m.username = :id
              or m.email = :id
              or concat(coalesce(m.oauthProvider,''), ':', coalesce(m.oauthId,'')) = :id
           """)
    Optional<User> findByAnyLoginId(@Param("id") String id);
}
