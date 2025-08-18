package com.smhrd.dtect.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.smhrd.dtect.entity.Member;

@Repository
public interface AdminRepository extends JpaRepository<Member, Long> {

}
