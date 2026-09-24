package org.revature.revconnect.repository;

import org.revature.revconnect.enums.BusinessCategory;
import org.revature.revconnect.model.BusinessProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {

    Optional<BusinessProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    Page<BusinessProfile> findByCategory(BusinessCategory category, Pageable pageable);

    Page<BusinessProfile> findByIsVerifiedTrue(Pageable pageable);

    Page<BusinessProfile> findByBusinessNameContainingIgnoreCase(String name, Pageable pageable);
}
