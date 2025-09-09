package com.smhrd.dtect.repository.member;

import com.smhrd.dtect.entity.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByUsername(String username);

	boolean existsByUsername(String username);

	Optional<Member> findByEmail(String email);

	Optional<Member> findByOauthProviderAndOauthId(String oauthProvider, String oauthId);

	@Query("select m from Member m where lower(trim(m.username)) = lower(trim(:username))")
	Optional<Member> findByUsernameIgnoreCaseTrim(@Param("username") String username);

	@Query("""
           select m from Member m
           where lower(trim(m.username)) = lower(trim(:id))
              or lower(trim(coalesce(m.email,''))) = lower(trim(:id))
              or concat(coalesce(m.oauthProvider,''), ':', coalesce(m.oauthId,'')) = :id
           """)
	Optional<Member> findByAnyLoginId(@Param("id") String id);
}
