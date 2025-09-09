package com.smhrd.dtect.repository.phrase;

import com.smhrd.dtect.entity.phrase.Phrase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhraseRepository extends JpaRepository<Phrase, Long> {
}
