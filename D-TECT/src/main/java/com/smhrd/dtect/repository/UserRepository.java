package com.smhrd.dtect.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smhrd.dtect.entity.Member;

@Repository
public interface UserRepository extends JpaRepository<Member, Long>{

	Optional<Member> findByUsername(String username);
	
}
