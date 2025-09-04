package com.smhrd.dtect.repository;

import com.smhrd.dtect.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 정확 일치(기존)
    Optional<Member> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

    // 🔹 공백 제거 + 대소문자 무시
    @Query("select m from Member m where lower(trim(m.username)) = lower(trim(:username))")
    Optional<Member> findByUsernameIgnoreCaseTrim(@Param("username") String username);

    // 🔹 유연 조회: username OR email OR (oauthProvider:oauthId)
    @Query("""
           select m from Member m
           where lower(trim(m.username)) = lower(trim(:id))
              or lower(trim(coalesce(m.email,''))) = lower(trim(:id))
              or concat(coalesce(m.oauthProvider,''), ':', coalesce(m.oauthId,'')) = :id
           """)
    Optional<Member> findByAnyLoginId(@Param("id") String id);
}
