package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.MemRole;
import com.smhrd.dtect.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByMember_MemIdx(Long memIdx);

    @Query("select u from User u where u.member.memIdx = :memIdx")
    Optional<User> findByMemberId(@Param("memIdx") Long memIdx);

    @Query("select m.username from User u join u.member m where u.userIdx = :userId")
    Optional<String> findMemberUsernameByUserId(@Param("userId") Long userId);

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

    List<User> findByMember_MemRole(MemRole memRole);
}
