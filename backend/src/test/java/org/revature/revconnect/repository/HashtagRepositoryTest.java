package org.revature.revconnect.repository;

import org.junit.jupiter.api.Test;
import org.revature.revconnect.model.Hashtag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class HashtagRepositoryTest {

    @Autowired
    private HashtagRepository hashtagRepository;

    @Test
    void findByName_ReturnsHashtag() {
        Hashtag hashtag = Hashtag.builder().name("java").usageCount(1L).build();
        hashtagRepository.save(hashtag);

        Optional<Hashtag> result = hashtagRepository.findByName("java");

        assertTrue(result.isPresent());
        assertEquals("java", result.get().getName());
    }

    @Test
    void findTrending_ReturnsSortedByUsageCount() {
        Hashtag h1 = hashtagRepository.save(Hashtag.builder().name("java").build());
        Hashtag h2 = hashtagRepository.save(Hashtag.builder().name("spring").build());
        Hashtag h3 = hashtagRepository.save(Hashtag.builder().name("react").build());

        // Increment to make them trending in specific order: spring (3), react (2),
        // java (1)
        h2.incrementUsage();
        h2.incrementUsage();
        hashtagRepository.save(h2);

        h3.incrementUsage();
        hashtagRepository.save(h3);

        List<Hashtag> trending = hashtagRepository.findTrending(PageRequest.of(0, 10));

        assertEquals(3, trending.size());
        assertEquals("spring", trending.get(0).getName());
        assertEquals("react", trending.get(1).getName());
        assertEquals("java", trending.get(2).getName());
    }

    @Test
    void findByNameContainingIgnoreCase_ReturnsMatches() {
        hashtagRepository.save(Hashtag.builder().name("JavaProgramming").usageCount(1L).build());
        hashtagRepository.save(Hashtag.builder().name("JavaScript").usageCount(1L).build());
        hashtagRepository.save(Hashtag.builder().name("Python").usageCount(1L).build());

        List<Hashtag> results = hashtagRepository.findByNameContainingIgnoreCase("java");

        assertEquals(2, results.size());
    }
}
